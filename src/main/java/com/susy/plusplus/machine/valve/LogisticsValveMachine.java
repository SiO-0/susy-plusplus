package com.susy.plusplus.machine.valve;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 物流阀（{@code susyplusplus:logistics_valve}）。
 *
 * <ul>
 * <li>红石控制：无信号导通、有信号断开（默认开）。</li>
 * <li>扳手右键 = 设输入面；Shift + 扳手右键 = 设输出面。</li>
 * <li><b>无缓存</b>：输入面收到的物品<b>当场</b>插入输出面邻居的 {@code IItemHandler}，
 * 返回放不下的剩余量（没有内部槽位）。</li>
 * <li><b>不主动抽取</b>：由 GT 物品管道推送。</li>
 * <li><b>关闭时仍与管道相连</b>：两侧始终返回非 null 的 {@code IItemHandler}
 * （关闭时是不可写入的 0 槽实现），所以管道连接不会被断开。</li>
 * </ul>
 */
public class LogisticsValveMachine extends AbstractRedstoneValve {

    /** 输入面：1 个“虚拟槽”，插入即转发。 */
    private final IItemHandler inputHandler = new ValveItemHandler(true);

    /** 输出面：0 槽（只用于保持管道连接，不接受任何插入）。 */
    private final IItemHandler outputHandler = new ValveItemHandler(false);

    public LogisticsValveMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new LogisticsValveMachine(metaTileEntityId);
    }

    // ------------------------------------------------------------------
    // 能力
    // ------------------------------------------------------------------

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (side == null) {
                return null;
            }
            if (isInputSide(side)) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inputHandler);
            }
            if (isOutputSide(side)) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(outputHandler);
            }
            return null;
        }
        return super.getCapability(capability, side);
    }

    // ------------------------------------------------------------------
    // 转发
    // ------------------------------------------------------------------

    /** 把物品插入输出面邻居，返回剩余量。 */
    protected ItemStack forwardToOutput(@NotNull ItemStack stack, boolean simulate) {
        if (!isOpen() || stack.isEmpty()) {
            return stack;
        }
        IItemHandler handler = outputNeighbourHandler();
        if (handler == null) {
            return stack;
        }
        ItemStack remaining = stack;
        int slots = handler.getSlots();
        for (int i = 0; i < slots && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    @Nullable
    protected IItemHandler outputNeighbourHandler() {
        TileEntity neighbour = outputNeighbor();
        if (neighbour == null) {
            return null;
        }
        return neighbour.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                getValveOutputFace().getOpposite());
    }

    // ------------------------------------------------------------------
    // 无缓存的 IItemHandler
    // ------------------------------------------------------------------

    private final class ValveItemHandler implements IItemHandler {

        private final boolean acceptsItems;

        private ValveItemHandler(boolean acceptsItems) {
            this.acceptsItems = acceptsItems;
        }

        @Override
        public int getSlots() {
            // 输出面 0 槽：连接判定只看 capability 是否为 null，因此管道仍然连接
            return acceptsItems ? 1 : 0;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!acceptsItems || stack.isEmpty()) {
                return stack;
            }
            return forwardToOutput(stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return acceptsItems && isOpen() && !stack.isEmpty();
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
        Textures.CONVEYOR_OVERLAY.renderSided(getValveInputFace(), renderState, translation, pipeline);
        Textures.CONVEYOR_OVERLAY.renderSided(getValveOutputFace(), renderState, translation, pipeline);
    }
}
