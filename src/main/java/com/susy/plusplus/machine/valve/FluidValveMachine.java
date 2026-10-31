package com.susy.plusplus.machine.valve;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 流体阀（{@code susyplusplus:fluid_valve}）。
 *
 * <ul>
 * <li>红石控制：无信号导通、有信号断开（默认开）。</li>
 * <li>扳手右键 = 设输入面；Shift + 扳手右键 = 设输出面。</li>
 * <li><b>无缓存</b>：输入面的 {@code fill()} 直接转发给输出面邻居的 {@code IFluidHandler}。</li>
 * <li><b>不主动抽取</b>：需要管道或上游机器推送（要主动抽取请用 IR 流体阀）。</li>
 * <li><b>关闭时仍与管道相连</b>：两侧始终返回非 null 的 {@code IFluidHandler}（关闭时为不可用实现），
 * 因此 GT 流体管道与 Susy-Core 无罐流体管道都不会因 capability 变化而断开。</li>
 * </ul>
 */
public class FluidValveMachine extends AbstractRedstoneValve {

    /** 输入面：只接受 fill（并当场转发），不能 drain。 */
    private final IFluidHandler inputHandler = new ValveFluidHandler(true);

    /** 输出面：既不能 fill 也不能 drain —— 仅用于让管道保持连接。 */
    private final IFluidHandler outputHandler = new ValveFluidHandler(false);

    public FluidValveMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new FluidValveMachine(metaTileEntityId);
    }

    // ------------------------------------------------------------------
    // 能力
    // ------------------------------------------------------------------

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (side == null) {
                // 通用查询（TOP 等）不返回：阀门没有可展示的储罐
                return null;
            }
            if (isInputSide(side)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(inputHandler);
            }
            if (isOutputSide(side)) {
                return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(outputHandler);
            }
            return null;
        }
        return super.getCapability(capability, side);
    }

    // ------------------------------------------------------------------
    // 转发
    // ------------------------------------------------------------------

    /**
     * 把流体交给输出面邻居。
     *
     * @return 实际被接收的量（0 表示没人接收 / 阀门关闭）
     */
    protected int forwardToOutput(@NotNull FluidStack stack, boolean doFill) {
        if (!isOpen()) {
            return 0;
        }
        TileEntity neighbour = outputNeighbor();
        if (neighbour == null) {
            return 0;
        }
        IFluidHandler handler = neighbour.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                getValveOutputFace().getOpposite());
        if (handler == null) {
            return 0;
        }
        return handler.fill(stack, doFill);
    }

    /** 输入面邻居的流体处理器（IR 阀主动抽取时使用）。 */
    @Nullable
    protected IFluidHandler inputNeighbourHandler() {
        TileEntity neighbour = inputNeighbor();
        if (neighbour == null) {
            return null;
        }
        return neighbour.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                getValveInputFace().getOpposite());
    }

    @Nullable
    protected IFluidHandler outputNeighbourHandler() {
        TileEntity neighbour = outputNeighbor();
        if (neighbour == null) {
            return null;
        }
        return neighbour.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                getValveOutputFace().getOpposite());
    }

    // ------------------------------------------------------------------
    // 无缓存的 IFluidHandler
    // ------------------------------------------------------------------

    private final class ValveFluidHandler implements IFluidHandler {

        private final boolean acceptsFluid;

        private ValveFluidHandler(boolean acceptsFluid) {
            this.acceptsFluid = acceptsFluid;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            // 名义容量很大（阀门不存储，只是转发；这样上游会愿意尝试推送）
            return new IFluidTankProperties[] {
                    new FluidTankProperties(null, Integer.MAX_VALUE, acceptsFluid, false)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (!acceptsFluid || resource == null || resource.amount <= 0) {
                return 0;
            }
            return forwardToOutput(resource, doFill);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            // 阀门没有任何存量
            return null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    protected ICubeRenderer getValveCasing() {
        return Textures.SOLID_STEEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void renderDirectionalOverlays(CCRenderState renderState, Matrix4 translation,
                                             IVertexOperation[] pipeline) {
        Textures.PIPE_IN_OVERLAY.renderSided(getValveInputFace(), renderState, translation, pipeline);
        Textures.PIPE_OUT_OVERLAY.renderSided(getValveOutputFace(), renderState, translation, pipeline);
    }

    // ------------------------------------------------------------------
    // 持久化（阀门无额外状态，全部在基类）
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        return super.writeToNBT(data);
    }
}
