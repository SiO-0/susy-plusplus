package com.susy.plusplus.pipe.steam;

import com.susy.plusplus.util.SteamFluids;

import gregtech.api.pipenet.tile.TileEntityPipeBase;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 蒸汽管道方块实体：<b>与 GT 流体管道相同的"即时整网分发"模型</b>。
 *
 * <h3>为什么不是"逐段推送"</h3>
 *
 * <p>
 * 第一版实现让每根管道自带缓冲罐并每 tick 往前推一格，结果是：
 * 每经过一根管道 +1 tick 延迟、管道里会残留蒸汽、和 GT 管道的手感完全不同。
 * 现在改成 GT 的做法：</p>
 *
 * <ul>
 * <li>管道本身<b>不存流体</b>；</li>
 * <li>有人往管道 {@code fill} 时，立刻从这根管道开始 <b>BFS 走完整张同压力管网</b>，
 * 把流体直接填进"网络末端"的 {@link IFluidHandler}（机器 / 储罐 / GT 流体管道）；
 * </li>
 * <li>有人从管道 {@code drain} 时，同样遍历网络，从末端处理器里抽流体
 * （供蒸汽泵覆盖板的导出模式使用）；</li>
 * <li>速率限制 = 网络里<b>最细那根</b>管道的速率（与 GT "最细处限速"一致）。</li>
 * </ul>
 *
 * <p>
 * GT 的 {@code PipeTankList#fill} 只是把流体存进 GT 管道自己的储罐、
 * 分发发生在它自己的 tick 里，所以把 GT 流体管道当作"末端处理器"填进去
 * <b>不会</b>造成同步递归。
 * </p>
 *
 * <h3>为什么仍然 implements ITickable</h3>
 *
 * <p>
 * GT 的管道方块是在 {@code TileEntityFluidPipeTickable#update()} 里调用
 * {@code getCoverableImplementation().update()} 来 tick 覆盖板的（已用 javap 核实）。
 * 本类的传输不依赖 tick，但<b>覆盖板需要</b>，所以保留 ITickable 并转发。
 * </p>
 */
public class TileEntitySteamPipe extends TileEntityPipeBase<SteamPipeType, SteamPipeProperties>
        implements ITickable {

    /** 网络扫描结果。 */
    private static final class NetSearch {

        private final List<IFluidHandler> endpoints;
        private final int throughput;

        private NetSearch(List<IFluidHandler> endpoints, int throughput) {
            this.endpoints = endpoints;
            this.throughput = throughput;
        }
    }

    private final Map<EnumFacing, IFluidHandler> faceHandlers = new EnumMap<>(EnumFacing.class);

    public TileEntitySteamPipe() {
        for (EnumFacing side : EnumFacing.VALUES) {
            faceHandlers.put(side, new SteamNetHandler(side));
        }
    }

    // ------------------------------------------------------------------
    // GT 管道基础
    // ------------------------------------------------------------------

    @Override
    public Class<SteamPipeType> getPipeTypeClass() {
        return SteamPipeType.class;
    }

    /** 本根管道的速率（L/t）；未初始化时按低压微型算。 */
    public int getThroughput() {
        SteamPipeType type = getPipeType();
        return type == null ? SteamPipeType.LOW_TINY.getThroughput() : type.getThroughput();
    }

    @Override
    public void update() {
        // 传输不需要 tick；这里只负责 tick 覆盖板（与 GT 流体管道一致）。
        getCoverableImplementation().update();
    }

    // ------------------------------------------------------------------
    // 能力
    // ------------------------------------------------------------------

    /**
     * 每个面都暴露流体能力。
     *
     * <p>
     * <b>必须无条件暴露</b>：GT 的 {@code BlockFluidPipe#canPipeConnectToBlock} 只检查
     * "邻居方块有没有 {@code IFluidHandler} 能力"，如果这里在"尚未连接"时返回 null，
     * GT 管道就永远连不上本模组的蒸汽管道（鸡生蛋问题）。
     * </p>
     */
    @Override
    @Nullable
    public <T> T getCapabilityInternal(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability != CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || facing == null) {
            return null;
        }
        if (isFaceBlocked(facing)) {
            return null;
        }
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(faceHandlers.get(facing));
    }

    // ------------------------------------------------------------------
    // 网络扫描 / 分发
    // ------------------------------------------------------------------

    /**
     * 从本管道开始 BFS 整张"同压力蒸汽管网"。
     *
     * @param sourceFacing 本次调用的来源面（{@code null} 表示不是由某个面推入的）；
     *                     会把该面正对的邻居从末端列表里排除，避免立刻原路推回。
     */
    private NetSearch scanNetwork(@Nullable EnumFacing sourceFacing) {
        List<IFluidHandler> endpoints = new ArrayList<>();
        Map<BlockPos, TileEntitySteamPipe> visited = new LinkedHashMap<>();
        Deque<TileEntitySteamPipe> queue = new ArrayDeque<>();

        visited.put(getPipePos(), this);
        queue.add(this);

        int throughput = Integer.MAX_VALUE;
        SteamPipeType selfType = getPipeType();

        while (!queue.isEmpty()) {
            TileEntitySteamPipe pipe = queue.poll();
            throughput = Math.min(throughput, pipe.getThroughput());

            for (EnumFacing side : EnumFacing.VALUES) {
                if (!pipe.isConnected(side) || pipe.isFaceBlocked(side)) {
                    continue;
                }
                TileEntity neighbour = pipe.getNeighbor(side);
                if (neighbour == null) {
                    continue;
                }

                if (neighbour instanceof TileEntitySteamPipe) {
                    TileEntitySteamPipe other = (TileEntitySteamPipe) neighbour;
                    // 只有同压力的蒸汽管道才继续走；不同压力的管道本来就是"不连通"的。
                    if (selfType == null || !selfType.samePressure(other.getPipeType())) {
                        continue;
                    }
                    BlockPos pos = other.getPipePos();
                    if (!visited.containsKey(pos)) {
                        visited.put(pos, other);
                        queue.add(other);
                    }
                    continue;
                }

                if (pipe == this && side == sourceFacing) {
                    // 不把"推我进来的那个方块"当成出口
                    continue;
                }

                IFluidHandler handler = neighbour
                        .getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
                if (handler != null) {
                    endpoints.add(handler);
                }
            }
        }

        if (throughput == Integer.MAX_VALUE) {
            throughput = getThroughput();
        }
        return new NetSearch(endpoints, throughput);
    }

    // ------------------------------------------------------------------
    // 单面处理器
    // ------------------------------------------------------------------

    private final class SteamNetHandler implements IFluidHandler {

        private final EnumFacing side;

        private SteamNetHandler(EnumFacing side) {
            this.side = side;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            // 管道不存流体：只报告"可无限吞吐（受速率限制）"的假储罐，
            // 让 TOP / 其它探针有东西可显示。
            return new IFluidTankProperties[] {
                    new FluidTankProperties(null, Math.max(getThroughput() * 20, 1), true, true)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (getWorld() == null || getWorld().isRemote) {
                return 0;
            }
            if (!SteamFluids.isSteam(resource) || isFaceBlocked(side)) {
                return 0;
            }
            NetSearch net = scanNetwork(side);
            int allowed = Math.min(resource.amount, net.throughput);
            int filled = 0;

            for (IFluidHandler handler : net.endpoints) {
                if (allowed <= 0) {
                    break;
                }
                FluidStack part = resource.copy();
                part.amount = allowed;
                int accepted = handler.fill(part, doFill);
                if (accepted <= 0) {
                    continue;
                }
                accepted = Math.min(accepted, allowed);
                filled += accepted;
                allowed -= accepted;
            }

            if (filled > 0 && doFill) {
                markAsDirty();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (!SteamFluids.isSteam(resource)) {
                return null;
            }
            return drainFromNetwork(resource.getFluid() == null ? null : resource.getFluid().getName(),
                    resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return drainFromNetwork(null, maxDrain, doDrain);
        }

        /**
         * 从网络末端抽流体。
         *
         * @param fluidName 只抽这种流体（{@code null} = 任意）
         */
        @Nullable
        private FluidStack drainFromNetwork(@Nullable String fluidName, int maxDrain, boolean doDrain) {
            if (getWorld() == null || getWorld().isRemote) {
                return null;
            }
            if (isFaceBlocked(side)) {
                return null;
            }
            NetSearch net = scanNetwork(null);
            int allowed = Math.min(maxDrain, net.throughput);
            FluidStack result = null;

            for (IFluidHandler handler : net.endpoints) {
                if (allowed <= 0) {
                    break;
                }
                FluidStack drained;
                if (fluidName == null) {
                    drained = handler.drain(allowed, doDrain);
                } else {
                    // 需要知道流体对象才能定向抽取：先模拟一次看看末端有什么
                    FluidStack probe = handler.drain(allowed, false);
                    if (probe == null || probe.getFluid() == null ||
                            !fluidName.equals(probe.getFluid().getName())) {
                        continue;
                    }
                    drained = handler.drain(probe.copy(), doDrain);
                }
                if (drained == null || drained.amount <= 0) {
                    continue;
                }
                if (result == null) {
                    result = drained.copy();
                } else {
                    result.amount += drained.amount;
                }
                allowed -= drained.amount;
            }

            if (result != null && doDrain) {
                markAsDirty();
            }
            return result;
        }
    }
}
