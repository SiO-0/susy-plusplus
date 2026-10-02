package com.susy.plusplus.multiblock;

import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.unification.material.Materials;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电动碾磨机（Electric Grinder）—— LV 电动多方块研磨机。
 *
 * <h2>定位</h2>
 *
 * <p>
 * 填补「蒸汽时代的蒸汽研磨机」与「MV 中期的偏心破碎机」之间的空档，
 * 且<b>不需要锰钢</b>：拿到钢之后就能造。
 * </p>
 *
 * <h2>配方：直接复用研磨机的配方表</h2>
 *
 * <p>
 * 本机器<b>没有自己的配方表</b>，直接把 <b>{@code RecipeMaps.MACERATOR_RECIPES}
 * 当作自己的配方表</b>：
 * </p>
 *
 * <ul>
 * <li>产率 / 副产物 / 耗时<b>与研磨机逐字段相同</b>——本来就是同一张表里的同一条配方；</li>
 * <li>整合包用 GroovyScript 对研磨机做的任何增删改，本机器<b>自动跟随</b>，不存在时序问题；</li>
 * <li>能加工的东西 = 研磨机能加工的东西（含整合包给矿床方块等手写的研磨配方）；</li>
 * <li>JEI 里直接显示<b>研磨机的配方分类</b>，不另开分类。</li>
 * </ul>
 *
 * <h2>吞吐来自并行，而不是改配方</h2>
 *
 * <p>
 * 时长沿用研磨机自身的时长，提升吞吐全靠<b>并行</b>：<b>固定 32 并行</b>
 * （{@link #PARALLEL_LIMIT}，不提供配置项）。并行数直接交给 GT 的
 * {@link MultiblockRecipeLogic#setParallelLimit(int)}，它会再按
 * <b>输入量、输出空间、电力</b>限制实际并行数，所以并行开大
 * <b>不会丢物品</b>，只会自动降并行。
 * </p>
 *
 * <h2>结构：刻意做成「控制器居中 + 外壳均匀」的对称形状</h2>
 *
 * <p>
 * 3(X) × 3(Y) × 3(Z) 的钢外壳，中间一格是空腔，四根竖直角柱用钢框架加强：
 * </p>
 *
 * <pre>
 * 俯视（y=1 层）        正视（沿 Z 看）
 *  F C F                  y=2:  F C F
 *  C # C                  y=1:  C # C     ← 正面中央就是控制器
 *  F C F                  y=0:  F C F
 * </pre>
 *
 * <ul>
 * <li>{@code F} = Steel Frame Box（4 根竖直角柱，起吊结构用）</li>
 * <li>{@code C} = Solid Steel Machine Casing（外壳，可替换为仓室）</li>
 * <li>{@code S} = 控制器（位于某一面的正中，玩家可直接右键）</li>
 * <li>{@code #} = 空气（内部空腔）</li>
 * </ul>
 *
 * <p>
 * <b>为什么必须是这种「四面看起来一样」的结构</b>：GT 的 JEI 结构预览是按
 * <b>固定朝向</b>把这个 pattern 画出来的（控制器朝向预览里的正前方），
 * 而玩家实际摆放控制器时，controller 的 {@code frontFacing} 取决于放置方向，
 * GT 会把整个 pattern 按该朝向旋转后再校验。如果 pattern 本身前后不对称
 * （例如前段进料斗、后段齿轮箱），那么"照着 JEI 图摆出来的结构"和
 * "机器认定的正面"就会差 90°/180°，看起来就是<b>主面朝向与 JEI 不一致</b>。
 * 外壳均匀、控制器居中之后，任何旋转都自洽，JEI 里看到的就与摆出来的完全一致。
 * </p>
 *
 * <p>
 * <b>刻意不启用维护机制</b>（与研磨机一致，避免早期玩家被卡）。
 * </p>
 *
 * <p>
 * 本地化键：{@code susyplusplus.machine.electric_grinder.name}
 * （由 {@code MetaTileEntity#getMetaName()} = {@code <namespace>.machine.<path>} 推导）。
 * </p>
 */
public class MetaTileEntityElectricGrinder extends RecipeMapMultiblockController {

    /**
     * 并行数：<b>32</b>（= 单方块 LV 研磨机的 32 倍吞吐）。
     *
     * <p>
     * 刻意<b>不做成配置项</b>：这个值一旦改动，已有存档的
     * {@code config/susyplusplus.cfg} 里会留着旧值（Forge 不会更新已存在的配置项），
     * 让人以为改动没生效。需要改的话直接改这个常量。
     * </p>
     */
    public static final int PARALLEL_LIMIT = 32;

    public MetaTileEntityElectricGrinder(ResourceLocation metaTileEntityId) {
        // ⚠ 直接复用研磨机的配方表：本机器不持有自己的 RecipeMap，
        //   JEI 里显示的也是研磨机的配方分类。
        super(metaTileEntityId, RecipeMaps.MACERATOR_RECIPES);
        // 沿用 GT 自己的并行实现：它会按输入 / 输出空间 / 电力再限制实际并行数，
        // 因此本机器永远不会因为"并行开太大"而丢物品，只会自动降低并行。
        this.recipeMapWorkable.setParallelLimit(PARALLEL_LIMIT);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityElectricGrinder(this.metaTileEntityId);
    }

    // ------------------------------------------------------------------
    // 结构
    // ------------------------------------------------------------------

    /**
     * 对称的 3×3×3 钢外壳（控制器居中、外壳均匀）。
     *
     * <h3>⚠ aisle 的方向语义</h3>
     *
     * <p>
     * {@code FactoryBlockPattern.start()} 等价于
     * {@code new FactoryBlockPattern(RIGHT, UP, BACK)}，即
     * </p>
     *
     * <ul>
     * <li><b>一个字符串内的字符</b> → 沿 <b>X</b>（宽）</li>
     * <li><b>同一个 {@code aisle()} 里的多个字符串</b> → 沿 <b>Y</b>（高）</li>
     * <li><b>多次 {@code aisle()} 调用</b> → 沿 <b>Z</b>（进深）</li>
     * </ul>
     *
     * <h3>为什么结构要对称</h3>
     *
     * <p>
     * 见类注释：JEI 预览把 pattern 按固定朝向画出来，玩家放置时 GT 会按 controller 的
     * {@code frontFacing} 旋转整个 pattern 再校验。只要外壳均匀、控制器位于面中央，
     * 任何旋转都自洽，JEI 里看到的与摆出来的就完全一致（本模组的
     * {@code MetaTileEntityReinforcedPBF} 用的也是这种"外壳均匀"的写法）。
     * </p>
     */
    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                // z=0 面：四角钢框架、四边机身、中央空腔
                .aisle("FCF", "C#C", "FCF")
                // z=1 中部：仅四根角柱与四条棱
                .aisle("FCF", "C#C", "FCF")
                // z=2 面：与 z=0 相同，但中央是控制器
                .aisle("FCF", "CSC", "FCF")
                .where('S', selfPredicate())
                .where('C', casingOrHatchPredicate())
                .where('F', frames(Materials.Steel))
                .where('#', air())
                .build();
    }

    /**
     * 外壳谓词：{@code C} 位置可以是 Solid Steel Machine Casing，也可以是允许的仓室。
     *
     * <h3>⚠ 为什么这里【绝对不能】对 {@code states(...)} 调 {@code setMinGlobalLimited(...)}</h3>
     *
     * <p>
     * {@code setMinGlobalLimited(int)} 的实现是
     * {@code limited.addAll(common); common.clear(); ...}——它会把这条
     * {@code SimplePredicate} 从 {@code TraceabilityPredicate.common}
     * <b>移进 {@code limited}</b>。而 GT 的 JEI 结构预览
     * （{@code BlockPattern#getPreview}）取方块的顺序是：
     * </p>
     *
     * <ol>
     * <li>先遍历 {@code limited}，按 {@code previewCount} / {@code minGlobalCount}
     * <b>以「额度」逐格消耗</b>，额度用完就换下一条；</li>
     * <li>再遍历 {@code common} 里 {@code previewCount > 0} 的条目（同样按额度）；</li>
     * <li>最后才遍历 {@code common} 里 {@code previewCount == -1} 的条目——
     * <b>只有这一档会「不限量」覆盖剩余的全部位置</b>。</li>
     * </ol>
     *
     * <p>
     * 所以一旦写成 {@code states(getCasingState()).setMinGlobalLimited(10)}，
     * 预览就只会画出 10 格机身 + 每类仓室各 1 格，<b>剩下十几个 {@code C} 位置什么都不画</b>，
     * 右键那些格子也点不出可替换方块列表——看起来就像「仓室不显示、机身贴图丢失」。
     * GT 自己的机器（{@code MetaTileEntityLargeMiner} / {@code DistillationTower} /
     * {@code FusionReactor} / {@code LargeTurbine} / {@code FluidDrill}）的机身谓词
     * 都只写 {@code states(getCasingState())}，不设全局下限。
     * </p>
     *
     * <p>
     * 结构强度并没有因此放松：每个 {@code C} 位置仍必须是「机身 or 允许的仓室」，
     * 而各仓室的数量由下面的 {@code setMinGlobalLimited} / {@code setMaxGlobalLimited}
     * 单独约束；本机器不接受维护仓与流体仓（不写进谓词即可）。
     * </p>
     *
     * <p>
     * 这里刻意用显式的 {@code abilities(...)} 而不是 {@code autoAbilities(...)}：
     * 本机器没有流体仓与维护仓，显式列出可让每个仓室的上下限一目了然，
     * 也与本模组 {@code MetaTileEntityReinforcedPBF} 的写法保持一致。
     * </p>
     */
    private static TraceabilityPredicate casingOrHatchPredicate() {
        return states(getCasingState())
                // 能源仓：至少 1 个（否则机器没法工作），最多 3 个
                // （要跑满 32 并行请放 2 个 LV 能源仓，或 1 个 MV 能源仓）
                .or(abilities(MultiblockAbility.INPUT_ENERGY)
                        .setMinGlobalLimited(1).setMaxGlobalLimited(3).setPreviewCount(1))
                // 物品输入总线（原料从这里进）
                .or(abilities(MultiblockAbility.IMPORT_ITEMS)
                        .setMaxGlobalLimited(2).setPreviewCount(1))
                // 物品输出总线（产物从这里出）
                .or(abilities(MultiblockAbility.EXPORT_ITEMS)
                        .setMaxGlobalLimited(2).setPreviewCount(1));
    }

    /** 外壳方块：Solid Steel Machine Casing（与偏心破碎机同一档材质）。 */
    private static IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID);
    }

    // ------------------------------------------------------------------
    // 显示 / 渲染
    // ------------------------------------------------------------------

    /**
     * 不启用维护机制。
     *
     * <p>
     * 蒸汽研磨机与单方块研磨机都没有维护；本机器定位是"蒸汽之后的第一台电动矿处设备"，
     * 此时玩家往往还没建立完整的维护供应链。要求维护会让人望而却步，
     * 因此与 {@code MetaTileEntityReinforcedPBF} 保持一致的策略。
     * </p>
     *
     * <p>
     * 实现上是不把维护仓写进 {@link #casingOrHatchPredicate()} 的谓词，
     * 因此结构里根本不会接受维护仓。
     * </p>
     */
    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip,
                               boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // 并行数是常量（不再是配置项），直接显示即可。
        tooltip.add(I18n.format("susyplusplus.machine.electric_grinder.tooltip.parallel",
                PARALLEL_LIMIT));
        tooltip.add(I18n.format("susyplusplus.machine.electric_grinder.tooltip.same_yield"));
        tooltip.add(I18n.format("susyplusplus.machine.electric_grinder.tooltip.no_maintenance"));
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        // 复用 GT 自带的研磨机正面覆盖层（不新增贴图）
        return Textures.MACERATOR_OVERLAY;
    }
}
