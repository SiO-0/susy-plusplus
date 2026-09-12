package com.susy.plusplus.multiblock;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;

import net.minecraft.util.Tuple;

/**
 * ReinforcedPBF 的配方逻辑：<b>不耗电</b> + <b>无需维护</b>。
 *
 * <p>
 * 本类沿用带电多方块框架（{@link MultiblockRecipeLogic}，因为要支持仓室替换），
 * 但把能量相关的方法全部"伪装"成永远充足 —— 与 GT 自己的
 * {@code gregtech.api.capability.impl.PrimitiveRecipeLogic} 完全相同的做法：
 * </p>
 *
 * <pre>
 * PrimitiveRecipeLogic:
 *   getEnergyInputPerSecond() → Integer.MAX_VALUE
 *   getEnergyStored()         → Integer.MAX_VALUE
 *   getEnergyCapacity()       → Integer.MAX_VALUE
 *   drawEnergy(...)           → true   // 假装耗了电
 *   getMaxVoltage()           → GTValues.LV
 * </pre>
 *
 * <p>
 * <b>为什么必须覆写 getMaxVoltage()：</b>父类
 * {@code MultiblockRecipeLogic#getMaxVoltage()}
 * 读的是能量仓的输入电压；本多方块没有能量仓（能力列表为空），
 * 其值会是 0，导致任何配方都因"tier 不够"而无法匹配。这里固定返回 LV。
 * </p>
 *
 * <p>
 * <b>无需维护：</b>一方面控制器覆写 {@code hasMaintenanceMechanics() → false}；
 * 这里再覆写 {@code getMaintenanceValues()} 返回 (0, 1.0)，双保险确保
 * 既不会有维护问题，也不会有"维护导致的时长惩罚"。
 * </p>
 *
 * <p>
 * 按要求<b>不启用并行</b>（不调用 {@code setParallelLimit}，保持默认 1）。
 * </p>
 */
public class ReinforcedPbfRecipeLogic extends MultiblockRecipeLogic {

    public ReinforcedPbfRecipeLogic(RecipeMapMultiblockController controller) {
        super(controller);
    }

    // ------------------------------------------------------------------ 不耗电

    @Override
    protected long getEnergyInputPerSecond() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected long getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected long getEnergyCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean drawEnergy(int recipeEUt, boolean simulate) {
        return true; // 假装能量已被扣除
    }

    @Override
    public long getMaxVoltage() {
        return GTValues.LV;
    }

    @Override
    public long getMaximumOverclockVoltage() {
        return GTValues.V[GTValues.LV];
    }

    // ------------------------------------------------------------------ 无需维护

    /** 返回 (维护问题数, 时长倍率) —— 恒为"无问题、无惩罚"。 */
    @Override
    protected Tuple<Integer, Double> getMaintenanceValues() {
        return new Tuple<>(0, 1.0D);
    }

    // ------------------------------------------------------------------ 隐藏 TOP
    // 能耗显示

    /**
     * TOP 的能耗行由
     * {@code gregtech.integration.theoneprobe.provider.RecipeLogicInfoProvider} 渲染，
     * 它取的是本方法；并且源码里有一句：
     *
     * <pre>
     * if (eut == 0)
     *     return; // do not display 0 eut
     * </pre>
     *
     * 因此这里恒返回 0，即可让强化土高炉的 TOP 面板<b>不再显示</b>
     * "耗电 1 EU/t (ULV)" 这一行（本机本来就不耗电）。
     * </p>
     */
    @Override
    public int getInfoProviderEUt() {
        return 0;
    }
}
