package com.susy.plusplus.multiblock.storage;

import com.susy.plusplus.multiblock.SuMetaTileEntities;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.SimpleTextWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.MetaBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 多方块板条箱（大容量物品存储）。
 *
 * <p>
 * 结构与 GT 的<b>钢制多方块储罐</b>（{@code MetaTileEntityMultiblockTank}）完全一致：
 * 固定 3×3×3、控制器在底层中心、外壳 ≥23 格、阀门最多 2 个、内部 1 格空腔。
 * 只是把"流体 + 储罐阀门"换成"物品 + 物品阀门"。
 * </p>
 *
 * <h3>容量为什么是"单物品类型"</h3>
 *
 * <p>
 * 需求容量是 1,000,000 / 16,000,000 / 32,000,000 <b>物品</b>。
 * 一个 {@code ItemStack} 的 count 上限是 127（byte），槽位式实现需要 15,625 个以上槽位，
 * 根本放不下 GUI 也不可能原版同步。因此这里采用与 GT 量子箱完全相同的范式：
 * <b>单一物品类型 + {@code long} 计数器</b>（见 {@link VirtualItemStorage}）。
 * 换一种物品类型必须先取空。
 * </p>
 *
 * <h3>破坏控制器 / 内容物</h3>
 *
 * <p>
 * 按用户确认：<b>内容物随控制器一起消失</b>（{@link #clearMachineInventory} 只清空、不产出掉落）。
 * </p>
 */
public class MetaTileEntityMultiblockCrate extends MultiblockWithDisplayBase {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 188;

    /** 与 GT 储罐一致的方块数量限制。 */
    private static final int MIN_CASING = 23;
    private static final int MAX_VALVES = 2;

    private final SuStorageTier tier;
    private final long capacity;

    /** 单物品类型的大容量库存。 */
    private VirtualItemStorage storage;

    public MetaTileEntityMultiblockCrate(ResourceLocation metaTileEntityId, SuStorageTier tier) {
        super(metaTileEntityId);
        this.tier = tier;
        this.capacity = tier.getCrateCapacity();
        // 与 GT 的 MetaTileEntityMultiblockTank 同样的写法：容量只有赋值后才能建库存
        initializeInventory();
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        // 父类构造器可能提前调用一次（此时 tier 还是 null），直接跳过
        if (tier == null) {
            return;
        }
        this.storage = new VirtualItemStorage(this, capacity);
        this.itemInventory = this.storage;
        this.importItems = this.storage;
        this.exportItems = this.storage;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityMultiblockCrate(metaTileEntityId, tier);
    }

    @Override
    protected void updateFormedValid() {
        // 纯存储，无每 tick 逻辑
    }

    // ------------------------------------------------------------------
    // 结构
    // ------------------------------------------------------------------

    @Override
    protected BlockPattern createStructurePattern() {
        MetaTileEntity valve = getValve();
        TraceabilityPredicate casing = states(getCasingState()).setMinGlobalLimited(MIN_CASING);
        if (valve != null) {
            casing = casing.or(metaTileEntities(valve).setMaxGlobalLimited(MAX_VALVES));
        }

        return FactoryBlockPattern.start()
                .aisle("XXX", "XXX", "XXX")
                .aisle("XXX", "X X", "XXX")
                .aisle("XXX", "XSX", "XXX")
                .where('S', selfPredicate())
                .where('X', casing)
                .where(' ', air())
                .build();
    }

    /** 本档对应的外壳方块（GT 现成机械方块）。 */
    private IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(tier.getCasing());
    }

    /** 本档对应的物品阀门（结构里允许的仓室）。 */
    @Nullable
    public static MetaTileEntity itemValveOf(SuStorageTier tier) {
        switch (tier) {
            case REINFORCED_TITANIUM:
                return SuMetaTileEntities.REINFORCED_TITANIUM_ITEM_VALVE;
            case CLEAN_STAINLESS_STEEL:
                return SuMetaTileEntities.CLEAN_STAINLESS_STEEL_ITEM_VALVE;
            case STEEL:
            default:
                return SuMetaTileEntities.STEEL_ITEM_VALVE;
        }
    }

    private MetaTileEntity getValve() {
        return itemValveOf(tier);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getFrontOverlay().renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return SuStorageTextures.casing(tier);
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        // 用 GT 现成的"量子箱盖"覆盖层表示这是一个大容量储物方块
        return Textures.QUANTUM_CHEST_OVERLAY;
    }

    // ------------------------------------------------------------------
    // 交互 / 能力
    // ------------------------------------------------------------------

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, net.minecraft.util.EnumHand hand, EnumFacing facing,
                                CuboidRayTraceResult hitResult) {
        if (!isStructureFormed()) {
            return false;
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return isStructureFormed();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (isStructureFormed() && storage != null) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(storage);
            }
            return null;
        }
        return super.getCapability(capability, side);
    }

    /** 按用户确认：内容物随控制器消失，不做掉落。 */
    @Override
    public void clearMachineInventory(@NotNull NonNullList<ItemStack> itemBuffer) {
        if (storage != null) {
            storage.clear();
        }
    }

    // ------------------------------------------------------------------
    // 持久化
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        if (storage != null) {
            storage.writeToNBT(data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (storage == null) {
            initializeInventory();
        }
        if (storage != null) {
            storage.readFromNBT(data);
        }
    }

    // ------------------------------------------------------------------
    // 界面（GT 旧 GUI）
    // ------------------------------------------------------------------

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        return ModularUI.builder(GuiTextures.BACKGROUND, GUI_WIDTH, GUI_HEIGHT)
                .label(10, 5, getMetaFullName())
                // 容量
                .widget(new ImageWidget(7, 18, 162, 18, GuiTextures.DISPLAY))
                .widget(new SimpleTextWidget(88, 23, "susyplusplus.gui.crate.capacity", 0xFFFFFF,
                        () -> Long.toString(capacity)))
                // 已存储
                .widget(new ImageWidget(7, 38, 162, 18, GuiTextures.DISPLAY))
                .widget(new SimpleTextWidget(88, 43, "susyplusplus.gui.crate.stored", 0xFFFFFF,
                        () -> Long.toString(storage == null ? 0L : storage.getStoredCount())))
                // 存储的物品名（服务端求值 → 直接用 getDisplayName()，不触碰客户端 I18n）
                .widget(new ImageWidget(7, 58, 162, 18, GuiTextures.DISPLAY))
                .widget(new SimpleTextWidget(88, 63, "", 0xFFFFFF, this::storedItemName))
                // 按需求：不再提供任何物品槽位 —— 存量可达百万，原版"整槽替换"语义会把它清成 64。
                // 物品种类 + 数量写在主方块 NBT（StoredItem / StoredCount），取放一律走物品阀门 / 管道 / 漏斗 / AE2。
                .widget(new SimpleTextWidget(88, 84, "susyplusplus.gui.crate.hint", 0xA0A0A0, () -> ""))
                .bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7, 106);
    }

    /** 供 GUI 显示的物品名（服务端安全：不使用客户端专属的 {@code net.minecraft.client.resources.I18n}）。 */
    private String storedItemName() {
        if (storage == null || storage.isEmpty()) {
            return "";
        }
        return storage.getVirtualStack().getDisplayName();
    }

    // ------------------------------------------------------------------
    // tooltip
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(I18n.format("susyplusplus.machine.multiblock_crate.tooltip.single_type"));
        tooltip.add(I18n.format("gregtech.universal.tooltip.item_storage_total", capacity));
        tooltip.add(I18n.format("susyplusplus.machine.multiblock_crate.tooltip.valve"));
    }
}
