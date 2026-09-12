package com.susy.plusplus.multiblock;

import com.susy.plusplus.block.SuBlocks;
import com.susy.plusplus.client.SuTextures;
import com.susy.plusplus.config.SuConfig;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.recipes.RecipeMaps;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 强化土高炉（Reinforced Primitive Blast Furnace）。
 *
 * <ul>
 * <li>使用原版土高炉的
 * RecipeMap：{@code RecipeMaps.PRIMITIVE_BLAST_FURNACE_RECIPES}</li>
 * <li><b>不耗电</b>：无能量仓，见 {@link ReinforcedPbfRecipeLogic}</li>
 * <li><b>无需维护</b>：{@link #hasMaintenanceMechanics()} 返回 false</li>
 * <li>不使用 GT 的 primitive 框架（那样无法插仓室），而是用带电框架
 * {@link RecipeMapMultiblockController} 以支持仓室替换</li>
 * <li>按用户要求<b>不启用并行</b></li>
 * </ul>
 *
 * <p>
 * 结构（三层，{@code S} = 控制器，{@code R} = 强化耐火砖/仓室，{@code #} = 空气）：
 * </p>
 *
 * <pre>
 * 第 1 层(y=0)     第 2 层(y=1)     第 3 层(y=2)
 *   R R R            R R R            R R R
 *   R R R            R # R            R S R
 *   R R R            R # R            R R R
 *   R R R            R # R            R R R
 * </pre>
 *
 * <p>
 * 即 {@code aisle} 的每个字符串是一行（沿 X），字符串顺序沿 Z，多次 {@code aisle()} 沿 Y。
 * 控制器位于顶层、X 中间、Z 第 2 格。
 * </p>
 *
 * <p>
 * 本地化键：{@code susyplusplus.machine.reinforced_pbf.name}
 * （由 {@code MetaTileEntity#getMetaName()} = {@code <namespace>.machine.<path>}
 * 推导）。
 * </p>
 */
public class MetaTileEntityReinforcedPBF extends RecipeMapMultiblockController {

    public MetaTileEntityReinforcedPBF(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, RecipeMaps.PRIMITIVE_BLAST_FURNACE_RECIPES);
        // 替换为"不耗电/无维护"的配方逻辑。
        // 这是 GT 自己的惯用写法（如 MetaTileEntityElectricBlastFurnace = new
        // HeatingCoilRecipeLogic(this)）：
        // MetaTileEntity 的 trait 表是【按名字索引】的 Map，同名 trait 会直接覆盖，不会残留旧的 logic。
        this.recipeMapWorkable = new ReinforcedPbfRecipeLogic(this);
        // 并行数取自配置文件（默认 4；范围 1~64）
        this.recipeMapWorkable.setParallelLimit(Math.max(1, SuConfig.reinforcedPbfParallel));
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityReinforcedPBF(this.metaTileEntityId);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("RRR", "RRR", "RRR", "RRR")
                .aisle("RRR", "R#R", "R#R", "R#R")
                .aisle("RRR", "RSR", "RRR", "RRR")
                .where('R', casingOrHatchPredicate())
                .where('#', air())
                .where('S', selfPredicate())
                .build();
    }

    /**
     * R 的判定：强化耐火砖方块，<b>或</b>以下仓室。
     *
     * <ul>
     * <li>{@code IMPORT_ITEMS} / {@code EXPORT_ITEMS} —— 与土高炉 RecipeMap 的
     * 3 物品输入 / 3 物品输出对应</li>
     * <li>{@code IMPORT_FLUIDS} / {@code EXPORT_FLUIDS} —— 原版土高炉配方无流体，
     * 预留给以后可能添加的流体配方</li>
     * </ul>
     *
     * <p>
     * <b>刻意不包含</b>：{@code INPUT_ENERGY}（不耗电）、{@code MAINTENANCE_HATCH}
     * （无需维护）、{@code MUFFLER_HATCH}（未启用消声器机制）。
     * </p>
     */
    private static TraceabilityPredicate casingOrHatchPredicate() {
        return states(SuBlocks.REINFORCED_FIREBRICK.getDefaultState())
                .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
                .or(abilities(MultiblockAbility.EXPORT_ITEMS).setMaxGlobalLimited(2))
                .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1))
                .or(abilities(MultiblockAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1));
    }

    /** 无需维护：不产生维护问题、不需要维护仓。 */
    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    /**
     * 物品提示（在物品栏 / HEI 里 hover 时显示）。
     *
     * <p>
     * 对应本地化键 {@code susyplusplus.machine.reinforced_pbf.tooltip.*}，
     * 见 {@code assets/susyplusplus/lang/{en_us,zh_cn}.lang}。
     * </p>
     */
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(TextFormatting.GOLD + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.recipe"));
        tooltip.add(TextFormatting.GREEN + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.no_energy"));
        tooltip.add(TextFormatting.GREEN + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.no_maintenance"));
        tooltip.add(TextFormatting.AQUA + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.parallel"));
        tooltip.add(TextFormatting.GRAY + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.hatch"));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        // 使用 assets/susyplusplus/textures/blocks/reinforcedpbf/reinforced_bricks.png
        return SuTextures.REINFORCED_BRICKS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected ICubeRenderer getFrontOverlay() {
        // 正面覆盖层用 GT 原版土高炉的表盘（运行/暂停状态提示）
        return Textures.PRIMITIVE_BLAST_FURNACE_OVERLAY;
    }
}
