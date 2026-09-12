package com.susy.plusplus.recipe;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.block.SuBlocks;
import com.susy.plusplus.config.SuConfig;
import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.material.SuMaterials;
import com.susy.plusplus.multiblock.SuMetaTileEntities;

import gregtech.api.GTValues;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import supersymmetry.api.recipes.SuSyRecipeMaps;

/**
 * 配方注册（在 postInit 阶段、GT 配方加载完成后追加）。
 *
 * <p>
 * 使用 GT 自身的 {@code RecipeMap}，因此 JEI 会通过 GT 自带的 JEI 插件自动展示，
 * 无需自写 JEI 插件。
 * </p>
 */
public final class SuRecipes {

    /** Susy-Core 的 modid（提供 DRYER_RECIPES 等 GTCEu 本体没有的机器）。 */
    private static final String MOD_SUSY = "susy";

    /** Pyrotech 的 modid。 */
    private static final String MOD_PYROTECH = "pyrotech";

    private SuRecipes() {
    }

    public static void init() {
        // 受配置文件（config/susyplusplus.cfg）控制，默认全部开启
        if (SuConfig.enableWaterproofSprayCan) {
            registerCannerRecipes();
            registerMixerRecipes();
        }
        if (SuConfig.enableBatteryCase) {
            registerBatteryCaseRecipe();
        }

        // 火种科技(Pyrotech) 相关配方
        if (SuConfig.enablePyrotechRecipeTweaks) {
            registerDryerRecipes();
            registerExtractorRecipes();
            registerForgeHammerRecipes();
        }

        if (SuConfig.enableReinforcedPbf) {
            registerReinforcedPbfCraftingRecipes();
        }

        if (SuConfig.enableWirelessEnergyTower) {
            registerWirelessEnergyTowerRecipe();
        }

        // 橡胶流体管道相关配方
        if (SuConfig.enableRubberPipeTweaks) {
            registerRubberPipeRecipes();
        }
    }

    // ==========================================================================
    // 橡胶流体管道：合金炉（橡胶条 + 对应钢模头（不消耗） -> 管道），7 EU/t
    // ==========================================================================

    /**
     * 与 GT 自己 {@code PipeRecipeHandler} 相同的"模头不消耗"写法
     * （GT 用的是 {@code EXTRUDER_RECIPES + notConsumable(SHAPE_EXTRUDER_PIPE_*)}；
     * 本模组按需求改用<b>合金炉</b> {@code ALLOY_SMELTER_RECIPES}，7 EU/t）。
     *
     * <p>
     * 产出比例（按需求，<b>橡胶条数量 → 1 个管道</b>）：
     * </p>
     *
     * <ul>
     * <li>微型 {@code pipeTinyFluid}：2 条 → 1</li>
     * <li>小型 {@code pipeSmallFluid}：1 条 → 1</li>
     * <li>普通 {@code pipeNormalFluid}：3 条 → 1</li>
     * <li>大型 {@code pipeLargeFluid}：6 条 → 1</li>
     * <li>巨型 {@code pipeHugeFluid}：12 条 → 1</li>
     * </ul>
     *
     * <p>
     * <b>前置条件</b>：橡胶必须存在「橡胶锭」（{@code OrePrefix.ingot, Materials.Rubber}）
     * 与「橡胶流体管道」（{@code OrePrefix.pipe*Fluid, Materials.Rubber}）。
     * GTCEu 本体并没有给 {@code Materials.Rubber} 加 ingot / FLUID_PIPE 属性，
     * 本整合包由 SUSY / GroovyScript 提供；缺失时只打 WARN 并跳过，不会崩溃。
     * </p>
     */
    private static void registerRubberPipeRecipes() {
        // 橡胶锭（不是 Rod）
        ItemStack rubberStick = OreDictUnifier.get(OrePrefix.ingot, Materials.Rubber);
        if (rubberStick.isEmpty()) {
            SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] Skip rubber pipe recipes: no item for OrePrefix.ingot + Materials.Rubber.");
            return;
        }

        registerRubberPipe(OrePrefix.pipeTinyFluid, MetaItems.SHAPE_EXTRUDER_PIPE_TINY, rubberStick, 2);
        registerRubberPipe(OrePrefix.pipeSmallFluid, MetaItems.SHAPE_EXTRUDER_PIPE_SMALL, rubberStick, 1);
        registerRubberPipe(OrePrefix.pipeNormalFluid, MetaItems.SHAPE_EXTRUDER_PIPE_NORMAL, rubberStick, 3);
        registerRubberPipe(OrePrefix.pipeLargeFluid, MetaItems.SHAPE_EXTRUDER_PIPE_LARGE, rubberStick, 6);
        registerRubberPipe(OrePrefix.pipeHugeFluid, MetaItems.SHAPE_EXTRUDER_PIPE_HUGE, rubberStick, 12);
    }

    /** 单条配方：N 个橡胶条 + 对应【钢】模头（不消耗） -> 1 个该尺寸的橡胶流体管道（合金炉 / 7 EU/t）。 */
    private static void registerRubberPipe(OrePrefix pipePrefix, MetaItem<?>.MetaValueItem steelShape,
            ItemStack rubberStick, int stickAmount) {
        ItemStack pipe = OreDictUnifier.get(pipePrefix, Materials.Rubber);
        if (pipe.isEmpty()) {
            SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] Skip rubber pipe recipe for {}: the pipe item does not exist.", pipePrefix);
            return;
        }

        ItemStack stickInput = rubberStick.copy();
        stickInput.setCount(stickAmount);

        RecipeMaps.ALLOY_SMELTER_RECIPES.recipeBuilder()
                .inputs(stickInput)
                .notConsumable(steelShape)
                .outputs(pipe)
                .duration(100)
                .EUt(7)
                .buildAndRegister();
    }

    // ==========================================================================
    // 工作台配方：强化土高炉 / 强化耐火砖
    // ==========================================================================

    /**
     * 工作台合成（形状固定）：
     *
     * <pre>
     *   S P S        S = 4x 钢螺丝
     *   P B P        P = 4x 钢板
     *   S P S        B = 原版土高炉 / 原版耐火砖
     * </pre>
     *
     * <p>
     * 用 GT 的
     * {@link ModHandler#addShapedRecipe(String, net.minecraft.item.ItemStack, Object...)}，
     * 材料用 {@link UnificationEntry}（GT 的矿辞/材料统一写法，等价于 GT 自己的
     * {@code BatteryRecipes} 里
     * {@code 'P', new UnificationEntry(plate, BatteryAlloy)} 那种写法）。
     * </p>
     */
    private static void registerReinforcedPbfCraftingRecipes() {
        // 强化土高炉：4 钢螺丝 + 4 钢板 + 1x 原版土高炉
        Object pbf = MetaTileEntities.PRIMITIVE_BLAST_FURNACE.getStackForm();
        if (!pbf.equals(ItemStack.EMPTY)) {
            ModHandler.addShapedRecipe("susyplusplus_reinforced_pbf",
                    SuMetaTileEntities.REINFORCED_PBF.getStackForm(),
                    "SPS", "PBP", "SPS",
                    'S', new UnificationEntry(OrePrefix.screw, Materials.Steel),
                    'P', new UnificationEntry(OrePrefix.plate, Materials.Steel),
                    'B', pbf);
        }

        // 强化耐火砖：4 钢螺丝 + 4 钢板 + 1x 原版耐火砖（primitive_bricks 外壳）
        if (SuBlocks.REINFORCED_FIREBRICK != null) {
            ModHandler.addShapedRecipe("susyplusplus_reinforced_firebrick",
                    new ItemStack(SuBlocks.REINFORCED_FIREBRICK),
                    "SPS", "PBP", "SPS",
                    'S', new UnificationEntry(OrePrefix.screw, Materials.Steel),
                    'P', new UnificationEntry(OrePrefix.plate, Materials.Steel),
                    'B', MetaBlocks.METAL_CASING.getItemVariant(BlockMetalCasing.MetalCasingType.PRIMITIVE_BRICKS));
        }
    }

    // ==========================================================================
    // 组装机：无线能量传输塔
    // ==========================================================================

    /**
     * 组装机（{@code ASSEMBLER_RECIPES}）：512 EU/t，1200 tick（60 s）。
     *
     * <ul>
     * <li>1x HV 机器外壳（{@code MetaTileEntities.HULL[GTValues.HV]}）</li>
     * <li>4x HV 发射器 / 4x HV 接收器</li>
     * <li>16x HV 电路（矿词 {@code circuit} + {@code MarkerMaterials.Tier.HV}）</li>
     * <li>32x 聚氯乙烯板 / 32x 铝线缆（1x，{@code cableGtSingle}）</li>
     * <li>16000 L 润滑剂（流体）</li>
     * <li>电路 13（{@code circuitMeta(13)}，<b>不消耗</b>）</li>
     * </ul>
     *
     * <p>
     * 共 7 个物品输入（含不消耗的编程电路）与 1 种流体，
     * 都在 {@code ASSEMBLER_RECIPES} 的 {@code itemInputs(9)} / {@code fluidInputs(1)}
     * 限制之内。
     * </p>
     */
    private static void registerWirelessEnergyTowerRecipe() {
        if (SuMetaTileEntities.WIRELESS_ENERGY_TOWER == null) {
            return;
        }
        RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(MetaTileEntities.HULL[GTValues.HV].getStackForm())
                .inputs(MetaItems.EMITTER_HV.getStackForm(4))
                .inputs(MetaItems.SENSOR_HV.getStackForm(4))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.HV, 16)
                .input(OrePrefix.plate, Materials.PolyvinylChloride, 32)
                .input(OrePrefix.cableGtSingle, Materials.Aluminium, 32)
                .fluidInputs(Materials.Lubricant.getFluid(16000))
                .circuitMeta(13)
                .outputs(SuMetaTileEntities.WIRELESS_ENERGY_TOWER.getStackForm())
                .duration(1200)
                .EUt(512)
                .buildAndRegister();
    }

    // ==========================================================================
    // 通用工具
    // ==========================================================================

    /**
     * 取模组物品，等价于 GroovyScript 的 {@code item('modid:name', meta)}。
     *
     * <p>
     * 物品不存在时返回 {@link ItemStack#EMPTY}，调用方必须判断并跳过，
     * 避免注册出输入/输出为空的无效配方。
     * </p>
     */
    private static ItemStack modItem(String modid, String name, int amount, int meta) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, name));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, amount, meta);
    }

    /** {@code item('pyrotech:name', meta)}。 */
    private static ItemStack pyrotech(String name, int amount, int meta) {
        return modItem(MOD_PYROTECH, name, amount, meta);
    }

    /** 缺物品时统一告警并返回 {@code false}（调用方据此跳过注册）。 */
    private static boolean requireItems(String what, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                SusyPlusPlus.LOGGER.warn(
                        "[SusyPlusPlus] Cannot register recipe '{}': missing mod item (is Pyrotech installed?).", what);
                return false;
            }
        }
        return true;
    }

    // ==========================================================================
    // 电池盒
    // ==========================================================================

    /**
     * 组装机：电池盒 x1（LV / 200 ticks / 30 EU/t）。
     *
     * <p>
     * 输入（共 5 种物品 + 1 种流体，正好满足 {@code ASSEMBLER_RECIPES} 的
     * 9 物品 / 1 流体上限）：
     * </p>
     * <ul>
     * <li>4x 钢板</li>
     * <li>4x 聚乙烯箔</li>
     * <li>4x 1x 铜导线</li>
     * <li>4x 小型铜弹簧</li>
     * <li>4x 铜箔</li>
     * <li>36 mB 焊锡（{@code GTValues.L / 4}）</li>
     * </ul>
     *
     * <p>
     * <b>关于"72 锡 或 36 焊锡"</b>：无需自己写两条配方。
     * GT 的 {@code RecipeMapBuilder} 为 {@code ASSEMBLER_RECIPES} 注册了 onBuild 钩子——
     * 当一条组装机配方<b>只有 1 种流体输入且该流体是 {@code SolderingAlloy}</b> 时，
     * 会自动复制并追加一条把焊锡换成 {@code Tin}、且用量为 <b>2 倍</b> 的等价配方。
     * 因此这里写 {@code SolderingAlloy(36 mB)} 即可，GT 会自动生成
     * {@code Tin(72 mB)} 的替代配方。
     * </p>
     *
     * <p>
     * 注意：GTCEu 中<b>没有</b> {@code Materials.Solder} 这个材料，
     * "焊锡"对应的是 {@code Materials.SolderingAlloy}；线材前缀是
     * {@code OrePrefix.wireGtSingle}（不是 {@code wireGt01}）。
     * </p>
     */
    private static void registerBatteryCaseRecipe() {
        RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, Materials.Steel, 4)
                .input(OrePrefix.foil, Materials.Polyethylene, 4)
                .input(OrePrefix.wireGtSingle, Materials.Copper, 4)
                .input(OrePrefix.springSmall, Materials.Copper, 4)
                .input(OrePrefix.foil, Materials.Copper, 4)
                .fluidInputs(Materials.SolderingAlloy.getFluid(GTValues.L / 4))
                .outputs(SuMetaItems.BATTERY_CASE.getStackForm())
                .duration(200)
                .EUt(30)
                .buildAndRegister();
    }

    // ==========================================================================
    // 原有的防水喷漆配方
    // ==========================================================================

    /** 灌装机：空喷漆罐 x1 + 防水漆液 576 mB -> 防水喷漆 x1（32 ticks / 8 EU/t）。 */
    private static void registerCannerRecipes() {
        RecipeMaps.CANNER_RECIPES.recipeBuilder()
                .inputs(MetaItems.SPRAY_EMPTY.getStackForm())
                .fluidInputs(SuMaterials.WaterproofPaint.getFluid(GTValues.L * 4))
                .outputs(SuMetaItems.WATERPROOF_SPRAY_CAN.getStackForm())
                .duration(32)
                .EUt(8)
                .buildAndRegister();
    }

    /**
     * 搅拌机：防水漆液 1152 mB（160 ticks / 30 EU/t）。
     *
     * <p>
     * 因源码中不存在 清漆/醇酸树脂/聚氨酯、硅油、石蜡，改用最接近的已有材料：
     * </p>
     * <ul>
     * <li>{@code PolyvinylAcetate}（聚醋酸乙烯酯，源码中即流体）= 清漆/醇酸树脂 近似物，1000 mB</li>
     * <li>{@code SiliconeRubber}（硅橡胶，源码中即流体，化学式 Si(CH3)2O 即硅氧烷）= 硅油 近似物，250 mB</li>
     * <li>{@code Polydimethylsiloxane} 粉（源码中为 dust，同为硅氧烷固体）= 石蜡粉/防水填料 近似物，1 个</li>
     * </ul>
     */
    private static void registerMixerRecipes() {
        RecipeMaps.MIXER_RECIPES.recipeBuilder()
                .fluidInputs(Materials.PolyvinylAcetate.getFluid(1000))
                .fluidInputs(Materials.SiliconeRubber.getFluid(250))
                .input(OrePrefix.dust, Materials.Polydimethylsiloxane, 1)
                .fluidOutputs(SuMaterials.WaterproofPaint.getFluid(1152))
                .duration(160)
                .EUt(30)
                .buildAndRegister();
    }

    // ==========================================================================
    // Pyrotech 相关（用 GT 机器替代 Pyrotech 的手工流程）
    // ==========================================================================

    /**
     * 干燥机：{@code pyrotech:material:13} -> {@code pyrotech:material:12}
     * （10 ticks / 7 EU/t）。
     *
     * <p>
     * <b>注意：GTCEu 本体的 {@code RecipeMaps} 里没有 {@code DRYER_RECIPES}</b>
     * （已用 javap 核实 2.8.7-beta 与 2.8.10-beta 均无）。干燥机是本整合包自带的
     * <b>Susy-Core</b>（modid {@code susy}）添加的，配方表在
     * {@code supersymmetry.api.recipes.SuSyRecipeMaps#DRYER_RECIPES}。
     * </p>
     *
     * <p>
     * 用 {@code Loader.isModLoaded("susy")} 保护：未装 Susy-Core 时不会执行到
     * {@code SuSyRecipeMaps}，因此不会触发该类加载（NoClassDefFoundError）。
     * </p>
     */
    private static void registerDryerRecipes() {
        if (!Loader.isModLoaded(MOD_SUSY)) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Susy-Core (modid '{}') not loaded; skip dryer recipe.", MOD_SUSY);
            return;
        }
        ItemStack wet = pyrotech("material", 1, 13);
        ItemStack dry = pyrotech("material", 1, 12);
        if (!requireItems("dryer", wet, dry)) {
            return;
        }
        SuSyRecipeMaps.DRYER_RECIPES.recipeBuilder()
                .inputs(dry)
                .outputs(wet)
                .duration(10)
                .EUt(7)
                .buildAndRegister();
    }

    /**
     * 提取机：cobblestone（矿辞） -> {@code pyrotech:rock} x8（10 ticks / 7 EU/t）。
     *
     * <p>
     * 等价于 GroovyScript 的 {@code ore('cobblestone')}，
     * 用 {@link OreDictUnifier#get(String)} 取第一个匹配物品。
     * </p>
     */
    private static void registerExtractorRecipes() {
        ItemStack cobblestone = OreDictUnifier.get("cobblestone");
        if (cobblestone.isEmpty()) {
            // 极端兜底：矿辞未注册时直接用原版圆石。
            cobblestone = new ItemStack(Blocks.COBBLESTONE);
        }
        ItemStack rock = pyrotech("rock", 8, 0);
        if (!requireItems("extractor (cobblestone -> rock)", rock)) {
            return;
        }
        RecipeMaps.EXTRACTOR_RECIPES.recipeBuilder()
                .inputs(cobblestone)
                .outputs(rock)
                .duration(10)
                .EUt(7)
                .buildAndRegister();
    }

    /**
     * 锻造锤：{@code minecraft:flint} -> {@code pyrotech:material:10} x3
     * （10 ticks / 7 EU/t）。
     */
    private static void registerForgeHammerRecipes() {
        ItemStack shard = pyrotech("material", 3, 10);
        if (!requireItems("forge hammer (flint -> material:10)", shard)) {
            return;
        }
        RecipeMaps.FORGE_HAMMER_RECIPES.recipeBuilder()
                .inputs(new ItemStack(Items.FLINT))
                .outputs(shard)
                .duration(10)
                .EUt(7)
                .buildAndRegister();
    }
}
