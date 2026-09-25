package com.susy.plusplus.machine;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerProxy;
import gregtech.api.capability.impl.NotifiableFluidTank;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.TankWidget;
import com.susy.plusplus.multiblock.storage.SuStorageTextures;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 流体样品存储（Fluid Sample Storage）—— MV / HV / EV 三档。
 *
 * <p>
 * 对齐 Susy-Core 的同名机器（{@code supersymmetry:fluid_samples_storage}，注册 id 18525，
 * 见 {@code MetaTileEntityFluidSamplesStorage}）：<b>32 个互相独立的储罐</b>（8×4 网格），
 * 没有物品槽、不处理配方、<b>不耗电</b>，纯粹是一个"低科技的分装架"。
 * </p>
 *
 * <p>
 * 与 Susy-Core 版本的区别（按需求）：
 * </p>
 *
 * <ul>
 * <li>提供 <b>MV / HV / EV</b> 三档，每格（每个储罐）容量分别为
 * <b>32,000 / 64,000 / 128,000 L</b>（Susy-Core 原版只有一档 LV，8000 L/格）；</li>
 * <li>贴图沿用 GT 自带的 {@code machines/fluid_samples_storage} 覆盖层 +
 * {@code Textures.VOLTAGE_CASINGS[tier]}（<b>不需要新增任何 PNG</b>）；</li>
 * <li>不依赖 Susy-Core（纯 GT 环境也能用）。</li>
 * </ul>
 *
 * <p>
 * 32 个储罐的内容物通过 {@link FluidTankList#serializeNBT()} 写进本方块自己的 NBT。
 * </p>
 */
public class FluidSamplesStorageMachine extends MetaTileEntity {

    /** 储罐数量（与 Susy-Core 原版一致）。 */
    private static final int TANK_COUNT = 32;

    /** 界面里每行 / 每列的储罐数（与 Susy-Core 原版一致）。 */
    private static final int GRID_COLS = 8;
    private static final int GRID_ROWS = 4;

    private static final String NBT_FLUIDS = "FluidInventory";

    /** 电压等级（{@code GTValues.MV/HV/EV}），只用于基础贴图与 tooltip。 */
    private final int tier;

    /** 每格容量（mB）。 */
    private final int tankCapacity;

    private final NotifiableFluidTank[] fluidTanks = new NotifiableFluidTank[TANK_COUNT];

    /** 32 个储罐的共享列表；传进 / 传出 / 对外能力都用它。 */
    private FluidTankList fluidTankList;

    public FluidSamplesStorageMachine(ResourceLocation metaTileEntityId, int tier, int tankCapacity) {
        super(metaTileEntityId);
        this.tier = tier;
        this.tankCapacity = tankCapacity;
        for (int i = 0; i < TANK_COUNT; i++) {
            this.fluidTanks[i] = new NotifiableFluidTank(tankCapacity, this, false);
        }
        this.fluidTankList = new FluidTankList(false, fluidTanks);
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new FluidSamplesStorageMachine(metaTileEntityId, tier, tankCapacity);
    }

    @Override
    protected void initializeInventory() {
        // 父类构造器会先调用一次（此时 fluidTankList 还是 null），直接跳过
        if (this.fluidTankList == null) {
            return;
        }
        super.initializeInventory();
        // 32 个储罐共用同一份列表：不再额外包一层 FluidHandlerProxy
        this.fluidInventory = this.fluidTankList;
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return this.fluidTankList;
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return this.fluidTankList;
    }

    /**
     * 与 Susy-Core 原版一致：<b>不响应 {@code side == null} 的流体能力查询</b>，
     * 以免 The One Probe 之类的通用查询把 32 个储罐全部罗列出来。
     */
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && side == null) {
            return null;
        }
        return super.getCapability(capability, side);
    }

    // ------------------------------------------------------------------
    // 持久化
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        if (fluidTankList != null) {
            data.setTag(NBT_FLUIDS, fluidTankList.serializeNBT());
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (fluidTankList != null && data.hasKey(NBT_FLUIDS)) {
            fluidTankList.deserializeNBT(data.getCompoundTag(NBT_FLUIDS));
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /**
     * 基础贴图按电压档（与 Susy-Core 原版同样用 {@code Textures.VOLTAGE_CASINGS}）。
     *
     * <p>
     * ⚠ 2.8.10 的 {@code MetaTileEntity} 并没有无参 {@code getBaseTexture()} 可覆写，
     * 因此这里在 {@link #renderMetaTileEntity} 里显式渲染（GT 的量子箱也是这么做的）。
     * </p>
     */
    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.VOLTAGE_CASINGS[tier].render(renderState, translation, pipeline);
        SuStorageTextures.fluidSamplesOverlay().renderOrientedState(renderState, translation, pipeline,
                getFrontFacing(), false, false);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        TextureAtlasSprite sprite = SuStorageTextures.fluidSamplesOverlay().getParticleSprite();
        if (sprite == null) {
            // 兜底：万一覆盖层贴图没注册成功，也不要返回 null（否则物品/粒子渲染会 NPE）
            sprite = Textures.VOLTAGE_CASINGS[tier].getParticleSprite();
        }
        return Pair.of(sprite, getPaintingColorForRendering());
    }

    // ------------------------------------------------------------------
    // tooltip
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(I18n.format("susyplusplus.machine.fluid_samples_storage.tooltip.tanks",
                TANK_COUNT, tankCapacity));
        tooltip.add(I18n.format("susyplusplus.machine.fluid_samples_storage.tooltip.no_energy"));
        tooltip.add(I18n.format("gregtech.universal.tooltip.fluid_storage_capacity", (long) TANK_COUNT * tankCapacity));
    }

    // ------------------------------------------------------------------
    // 界面：8×4 = 32 个储罐（与 Susy-Core 原版同款布局）
    // ------------------------------------------------------------------

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        int windowHeight = 18 + 18 * GRID_ROWS + 94;
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, windowHeight)
                .label(10, 5, getMetaFullName());

        int gridLeft = 89 - GRID_COLS * 9;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                builder.widget(new TankWidget(fluidTankList.getTankAt(index),
                        gridLeft + col * 18, 18 + row * 18, 18, 18)
                        .setBackgroundTexture(GuiTextures.FLUID_SLOT)
                        .setContainerClicking(true, true)
                        .setAlwaysShowFull(true));
            }
        }

        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7,
                18 + 18 * GRID_ROWS + 12);
        return builder.build(getHolder(), entityPlayer);
    }

    /** 便于日志 / 调试。 */
    public int getTier() {
        return tier;
    }

    /** 便于日志 / 调试。 */
    public int getTankCapacity() {
        return tankCapacity;
    }
}
