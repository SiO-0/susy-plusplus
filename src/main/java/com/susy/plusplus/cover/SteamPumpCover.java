package com.susy.plusplus.cover;

import com.susy.plusplus.util.SteamFluids;

import gregtech.api.cover.CoverDefinition;
import gregtech.api.cover.CoverableView;
import gregtech.api.util.GTTransferUtils;
import gregtech.common.covers.CoverPump;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

/**
 * 「蒸汽泵」覆盖板：基于 GT 的 {@link CoverPump}，但
 * <ul>
 * <li>速率固定 <b>1024 L/t</b>（= 20480 L/s，见下方关于 {@code CoverPump} 额度语义的说明）；</li>
 * <li><b>只允许通过蒸汽</b>（白名单 {@code gregtech:steam} 与 {@code susy:dense_steam}）；</li>
 * <li><b>不消耗 EU</b>、<b>无内部缓存</b>（与 GT 电动泵一致：只有"本秒剩余额度"）。</li>
 * </ul>
 *
 * <h3>关于 {@code CoverPump} 的速率语义（已用 javap 核实 2.8.7 字节码）</h3>
 *
 * <p>
 * 构造参数存进 {@code maxFluidTransferRate}/{@code transferRate}，而
 * {@code update()} 的逻辑是：
 * </p>
 *
 * <pre>
 * if (isWorkingAllowed && fluidLeftToTransferLastSecond > 0)
 *     fluidLeftToTransferLastSecond -= doTransferFluids(fluidLeftToTransferLastSecond);
 * if (getOffsetTimer() % 20 == 0)
 *     fluidLeftToTransferLastSecond = transferRate;
 * </pre>
 *
 * <p>
 * 也就是说 GT 的"速率"是<b>每秒额度</b>，且<b>单个 tick 可以一次搬完整秒额度</b>。
 * 为了让"1024 L/t"是真正的每 tick 上限（而不是"一瞬间搬 20480 L 然后停 19 tick"），
 * 这里额外重写了 {@link #doTransferFluids(int)}，把每 tick 的传输上限钳到
 * {@link #MB_PER_TICK}。这样平均与瞬时都严格等于 1024 L/t、20480 L/s。
 * </p>
 *
 * <p>
 * 传输仍然走 GT 自己的 {@code GTTransferUtils.transferFluids(from, to, limit, filter)}
 * （与 {@code CoverPump#doTransferFluidsInternal} 完全相同的调用）。
 * </p>
 */
public class SteamPumpCover extends CoverPump {

    /** 每 tick 传输量（L/t）。 */
    public static final int MB_PER_TICK = 1024;

    /** 每秒额度（L/s）：{@link CoverPump} 的构造参数是"每秒额度"。 */
    public static final int MB_PER_SECOND = MB_PER_TICK * 20;

    public SteamPumpCover(@NotNull CoverDefinition definition, @NotNull CoverableView coverableView,
                          @NotNull EnumFacing attachedSide) {
        super(definition, coverableView, attachedSide, /* tier = */ 1 /* LV，仅用于底板贴图 */, MB_PER_SECOND);
    }

    // ------------------------------------------------------------------
    // 蒸汽白名单
    // ------------------------------------------------------------------

    /**
     * 判定是否为蒸汽。
     *
     * <p>
     * 用<b>流体注册名</b>比较而不是 {@code Material} 引用：
     * SUSY 的蒸汽是 GroovyScript 在运行期创建的（{@code susy:dense_steam}），
     * 本模组编译期拿不到对应的 Java 字段。也<b>不能</b>用 {@code name.contains("steam")}，
     * 因为 {@code susy:steamcracked_light_naphtha} 之类的裂解产物同样包含 "steam"。
     * </p>
     */
    public static boolean isSteam(FluidStack stack) {
        return SteamFluids.isSteam(stack);
    }

    /**
     * 本覆盖板接受的流体 = 蒸汽白名单 <b>且</b> 通过 UI 上的流体过滤器。
     *
     * <p>
     * 2.8.7 的 {@link CoverPump} 没有 {@code createFilterRow()} 开关（那是 2.8.10 才有的），
     * 过滤器行无法隐藏；因此这里让过滤器<b>叠加</b>在蒸汽白名单之上：
     * 未设置过滤器时（默认）等价于"任意蒸汽"，设置后可以进一步限定为某种蒸汽
     * （例如只要 {@code dense_steam}）。这样 UI 上的过滤器不会变成装饰品。
     * </p>
     */
    @Override
    protected boolean checkInputFluid(FluidStack fluidStack) {
        return isSteam(fluidStack) && getFluidFilterContainer().testFluidStack(fluidStack);
    }

    // ------------------------------------------------------------------
    // 传输
    // ------------------------------------------------------------------

    /** 把 GT 的"每秒额度"进一步钳到每 tick 上限，保证真正的 1024 L/t。 */
    @Override
    protected int doTransferFluids(int transferLimit) {
        return super.doTransferFluids(Math.min(transferLimit, MB_PER_TICK));
    }

    @Override
    protected int doTransferFluidsInternal(IFluidHandler myFluidHandler, IFluidHandler fluidHandler,
                                           int transferLimit) {
        if (getPumpMode() == PumpMode.IMPORT) {
            return GTTransferUtils.transferFluids(fluidHandler, myFluidHandler, transferLimit,
                    this::checkInputFluid);
        } else if (getPumpMode() == PumpMode.EXPORT) {
            return GTTransferUtils.transferFluids(myFluidHandler, fluidHandler, transferLimit,
                    this::checkInputFluid);
        }
        return 0;
    }
}
