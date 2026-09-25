package com.susy.plusplus.multiblock.storage;

import com.susy.plusplus.multiblock.SuMetaTileEntities;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 洁净不锈钢 / 加强钛多方块储罐。
 *
 * <p>
 * <b>直接继承 GT 的 {@code MetaTileEntityMultiblockTank}</b>，只覆写三件事：
 * </p>
 *
 * <ol>
 * <li>{@code createStructurePattern()}：外壳换成对应档位的机械方块、阀门换成我们自己的储罐阀门
 * （GT 原类里 {@code getCasingState()}/{@code getValve()} 是 private，无法覆写，
 * 所以只能整个模式重建 —— 结构与 GT 储罐<b>逐字符一致</b>，只替换 two 个方块）。</li>
 * <li>{@code getBaseTexture(...)}：按档位取外壳贴图。</li>
 * <li>{@code createMetaTileEntity(...)}：复刻时保持档位。</li>
 * </ol>
 *
 * <p>
 * 库存、界面、能力（{@code IFluidHandler}）、tooltip、无维护等行为全部<b>继承自 GT 原类</b>，
 * 因此不存在"抄错"的风险；容量通过父类构造器传入。
 * </p>
 */
public class SuMultiblockTank extends gregtech.common.metatileentities.multi.MetaTileEntityMultiblockTank {

    private static final int MIN_CASING = 23;
    private static final int MAX_VALVES = 2;

    private final SuStorageTier tier;

    public SuMultiblockTank(ResourceLocation metaTileEntityId, SuStorageTier tier) {
        // isMetal = true：不做流体过滤（与原版钢制储罐一致）
        super(metaTileEntityId, true, (int) tier.getTankCapacity());
        this.tier = tier;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new SuMultiblockTank(metaTileEntityId, tier);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        MetaTileEntity valve = tankValveOf(tier);
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

    /** 本档对应的外壳方块。 */
    private IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(tier.getCasing());
    }

    /** 本档对应的储罐阀门（结构里允许的仓室）。 */
    @Nullable
    public static MetaTileEntity tankValveOf(SuStorageTier tier) {
        switch (tier) {
            case REINFORCED_TITANIUM:
                return SuMetaTileEntities.REINFORCED_TITANIUM_TANK_VALVE;
            case CLEAN_STAINLESS_STEEL:
                return SuMetaTileEntities.CLEAN_STAINLESS_STEEL_TANK_VALVE;
            case STEEL:
            default:
                return MetaTileEntities.STEEL_TANK_VALVE;
        }
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return SuStorageTextures.casing(tier);
    }
}
