package com.susy.plusplus.machine.valve;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;

/**
 * IR 流体阀（{@code susyplusplus:ir_fluid_valve}）—— 工业复兴（Industrial Renewal）
 * 流体阀的增强版。
 *
 * <p>
 * 行为与 {@link FluidValveMachine} 相同（红石控制、无缓存、关闭时仍与管道相连），
 * <b>额外</b>每 tick 主动从输入面邻居抽取流体并转发到输出面：
 * </p>
 *
 * <pre>
 * available = inputHandler.drain(Integer.MAX_VALUE, false)   // 模拟
 * accepted  = outputHandler.fill(available, true)            // 真正写入
 * if (accepted > 0) inputHandler.drain(accepted, true)        // 只扣掉真正送出去的量
 * </pre>
 *
 * <p>
 * 这与工业复兴 {@code TileEntityValvePipeLarge#update()} 完全一致
 * （它同样用 {@code Integer.MAX_VALUE} 做 {@code drain}）。
 * 所谓「速率无上限」的实际含义是「每次尽可能多地抽」，最终仍受<b>输出端</b>
 * {@code fill()} 的返回值限制；本模组不做额外限速。
 * </p>
 */
public class IRFluidValveMachine extends FluidValveMachine {

    public IRFluidValveMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new IRFluidValveMachine(metaTileEntityId);
    }

    @Override
    public void update() {
        super.update();
        if (getWorld() == null || getWorld().isRemote || !isOpen()) {
            return;
        }
        IFluidHandler input = inputNeighbourHandler();
        if (input == null) {
            return;
        }
        FluidStack available = input.drain(Integer.MAX_VALUE, false);
        if (available == null || available.amount <= 0) {
            return;
        }
        // 用副本转发，避免某些实现写坏传入的 FluidStack
        int accepted = forwardToOutput(available.copy(), true);
        if (accepted > 0) {
            input.drain(accepted, true);
        }
    }

    // ------------------------------------------------------------------
    // 渲染：不锈钢外壳 + 泵覆盖层（与普通流体阀区分）
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    protected ICubeRenderer getValveCasing() {
        return Textures.CLEAN_STAINLESS_STEEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void renderDirectionalOverlays(CCRenderState renderState, Matrix4 translation,
                                             IVertexOperation[] pipeline) {
        Textures.PUMP_OVERLAY.renderSided(getValveInputFace(), renderState, translation, pipeline);
        Textures.PIPE_OUT_OVERLAY.renderSided(getValveOutputFace(), renderState, translation, pipeline);
    }
}
