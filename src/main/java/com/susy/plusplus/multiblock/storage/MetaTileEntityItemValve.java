package com.susy.plusplus.multiblock.storage;

import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 物品阀门：多方块板条箱的"取放口"。
 *
 * <p>
 * 完全对齐 GT 的<b>储罐阀门</b>（{@code MetaTileEntityTankValve}）的行为：
 * 接入结构后<b>直接引用控制器的库存</b>（不做代理），脱离结构时换成"空库存"占位，
 * 以免能力检查出问题；并且朝下安装时会自动把物品输出到下方方块。
 * </p>
 *
 * <p>
 * 与储罐阀门的区别只有两点：能力换成自定义的 {@link SuStorageAbilities#ITEM_VALVE}
 * （暴露 {@code IItemHandlerModifiable}），以及材质按档位取外壳贴图。
 * GT 的原版阀门只有 {@code isMetal} 布尔两种材质，无法表达洁净不锈钢 / 加强钛，
 * 因此必须新增类。
 * </p>
 */
public class MetaTileEntityItemValve extends MetaTileEntityMultiblockPart
        implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    private final SuStorageTier tier;

    /** 自动输出到下方的间隔（tick）。 */
    private static final int AUTO_OUTPUT_INTERVAL = 5;

    public MetaTileEntityItemValve(ResourceLocation metaTileEntityId, SuStorageTier tier) {
        super(metaTileEntityId, 0);
        this.tier = tier;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityItemValve(metaTileEntityId, tier);
    }

    // ------------------------------------------------------------------
    // 库存：接入结构后直接使用控制器的库存
    // ------------------------------------------------------------------

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        initializeDummyInventory();
    }

    /** 未接入结构时用 0 格库存占位（等价于储罐阀门的 {@code FluidHandlerProxy(new FluidTankList(false), ...)}）。 */
    private void initializeDummyInventory() {
        ItemStackHandler dummy = new ItemStackHandler(0);
        this.itemInventory = dummy;
        this.importItems = dummy;
        this.exportItems = dummy;
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controllerBase) {
        super.addToMultiBlock(controllerBase);
        // 直接使用控制器的物品库存（板条箱是单物品类型 + long 计数的虚拟库存）
        IItemHandler controllerInventory = controllerBase.getItemInventory();
        if (controllerInventory instanceof IItemHandlerModifiable && controllerInventory.getSlots() > 0) {
            IItemHandlerModifiable modifiable = (IItemHandlerModifiable) controllerInventory;
            this.itemInventory = modifiable;
            this.importItems = modifiable;
            this.exportItems = modifiable;
        }
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controllerBase) {
        super.removeFromMultiBlock(controllerBase);
        initializeDummyInventory();
    }

    @Override
    protected boolean shouldSerializeInventories() {
        return false;
    }

    // ------------------------------------------------------------------
    // 能力
    // ------------------------------------------------------------------

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return SuStorageAbilities.ITEM_VALVE;
    }

    @Override
    public void registerAbilities(@NotNull List<IItemHandlerModifiable> abilities) {
        abilities.add(this.getImportItems());
    }

    @Override
    public boolean canPartShare() {
        return false;
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    /**
     * 阀门没有界面：{@link #openGUIOnRightClick()} 返回 {@code false}，此方法不会被调用，
     * 但 {@code MetaTileEntity} 把它声明为抽象方法，必须给出实现（GT 的储罐阀门同样处理）。
     */
    @Nullable
    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return null;
    }

    @Override
    public boolean needsSneakToRotate() {
        return true;
    }

    // ------------------------------------------------------------------
    // 每 tick：朝下时自动输出（对齐储罐阀门"朝下自动输出"的设定）
    // ------------------------------------------------------------------

    @Override
    public void update() {
        super.update();
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        if (getOffsetTimer() % AUTO_OUTPUT_INTERVAL != 0L || !isAttachedToMultiBlock()) {
            return;
        }
        if (getFrontFacing() != EnumFacing.DOWN) {
            return;
        }
        TileEntity neighbour = getNeighbor(getFrontFacing());
        if (neighbour == null) {
            return;
        }
        IItemHandler target = neighbour.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                getFrontFacing().getOpposite());
        if (target == null) {
            return;
        }
        pushInto(target);
    }

    /** 尽量把本阀门（= 控制器）里的物品塞进目标库存。 */
    private void pushInto(@NotNull IItemHandler target) {
        IItemHandler source = this.getImportItems();
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack inSlot = source.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(target, inSlot, false);
            int moved = inSlot.getCount() - remainder.getCount();
            if (moved > 0) {
                source.extractItem(slot, moved, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.PIPE_IN_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    public ICubeRenderer getBaseTexture() {
        if (getController() == null) {
            // 未接入结构时显示本档外壳（接入后交给父类，由控制器决定）
            return SuStorageTextures.casing(tier);
        }
        return super.getBaseTexture();
    }

    @Override
    public int getDefaultPaintingColor() {
        return 0xFFFFFF;
    }

    // ------------------------------------------------------------------
    // tooltip
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("susyplusplus.machine.item_valve.tooltip"));
        tooltip.add(I18n.format("susyplusplus.machine.item_valve.tooltip.down"));
    }

    @Override
    public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.tool_action.screwdriver.access_covers"));
        tooltip.add(I18n.format("gregtech.tool_action.wrench.set_facing"));
        super.addToolUsages(stack, world, tooltip, advanced);
    }
}
