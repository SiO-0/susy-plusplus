package com.susy.plusplus.multiblock;

import com.susy.plusplus.Tags;
import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;
import com.susy.plusplus.machine.FluidSamplesStorageMachine;
import com.susy.plusplus.machine.StorageScannerMachine;
import com.susy.plusplus.multiblock.storage.MetaTileEntityItemValve;
import com.susy.plusplus.multiblock.storage.MetaTileEntityMultiblockCrate;
import com.susy.plusplus.multiblock.storage.SuMultiblockTank;
import com.susy.plusplus.multiblock.storage.SuStorageAbilities;
import com.susy.plusplus.multiblock.storage.SuStorageTier;
import com.susy.plusplus.multiblock.storage.SuTankValve;
import com.susy.plusplus.multiblock.wireless.MetaTileEntityWirelessEnergyTower;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.common.metatileentities.MetaTileEntities;

import net.minecraft.util.ResourceLocation;

/**
 * 本模组的 MetaTileEntity（机器）注册入口。
 *
 * <p>
 * 必须使用 GT 自己的
 * {@code MetaTileEntities.registerMetaTileEntity(int, T)}：
 * 它除了写入 MTE 注册表之外，还会<b>自动</b>完成
 * </p>
 *
 * <ul>
 * <li>多块仓室能力注册（{@code IMultiblockAbilityPart}）</li>
 * <li>JEI 多方块结构预览注册：
 * {@code MultiblockInfoCategory.registerMultiblock(controller)}（当
 * {@code shouldShowInJei()} 为真）</li>
 * </ul>
 *
 * <p>
 * 因此<b>不需要自写 JEI 插件</b>，也无需手动调用 registerMultiblock。
 * </p>
 */
public final class SuMetaTileEntities {

    /**
     * 本模组的 MTE 数字 ID。
     *
     * <p>
     * 该 ID 用于物品元数据，必须全局唯一。已核实的占用情况：
     * GT 本体用低位；Susy-Core 用 14500–18527 / 19000–20002 / 32000。
     * 故此处取 32100 起。若将来冲突，改这一个常量即可。
     * </p>
     */
    private static final int ID_REINFORCED_PBF = 32100;

    /**
     * 无线能量传输塔的数字 ID（{@code susyplusplus:wireless_energy_tower}）。
     *
     * <p>
     * 与 {@link #ID_REINFORCED_PBF} 相邻但不同值，避免冲突。
     * </p>
     */
    private static final int ID_WIRELESS_ENERGY_TOWER = 32101;

    /** 存储检测器的数字 ID（{@code susyplusplus:storage_scanner}）。 */
    private static final int ID_STORAGE_SCANNER = 32102;

    // ------------------------------------------------------------------
    // 多方块存储升级（32110 ~ 32119）
    // ------------------------------------------------------------------

    private static final int ID_STEEL_MULTIBLOCK_CRATE = 32110;
    private static final int ID_CLEAN_STAINLESS_STEEL_MULTIBLOCK_TANK = 32111;
    private static final int ID_CLEAN_STAINLESS_STEEL_MULTIBLOCK_CRATE = 32112;
    private static final int ID_REINFORCED_TITANIUM_MULTIBLOCK_TANK = 32113;
    private static final int ID_REINFORCED_TITANIUM_MULTIBLOCK_CRATE = 32114;
    private static final int ID_STEEL_ITEM_VALVE = 32115;
    private static final int ID_CLEAN_STAINLESS_STEEL_TANK_VALVE = 32116;
    private static final int ID_CLEAN_STAINLESS_STEEL_ITEM_VALVE = 32117;
    private static final int ID_REINFORCED_TITANIUM_TANK_VALVE = 32118;
    private static final int ID_REINFORCED_TITANIUM_ITEM_VALVE = 32119;

    // ------------------------------------------------------------------
    // 流体样品存储（32120 ~ 32122）
    // ------------------------------------------------------------------

    private static final int ID_FLUID_SAMPLES_STORAGE_MV = 32120;
    private static final int ID_FLUID_SAMPLES_STORAGE_HV = 32121;
    private static final int ID_FLUID_SAMPLES_STORAGE_EV = 32122;

    /** {@code GTValues.MV} / {@code HV} / {@code EV}。 */
    private static final int TIER_HV = 3;
    private static final int TIER_EV = 4;

    /** 流体样品存储每格（每个储罐）的容量：32,000 / 64,000 / 128,000 L。 */
    private static final int FLUID_SAMPLES_CAPACITY_MV = 32_000;
    private static final int FLUID_SAMPLES_CAPACITY_HV = 64_000;
    private static final int FLUID_SAMPLES_CAPACITY_EV = 128_000;

    /** MV 电压等级（{@code GTValues.MV}）。 */
    private static final int TIER_MV = 2;

    /** 强化土高炉控制器（注册名 {@code susyplusplus:reinforced_pbf}）。 */
    public static MetaTileEntityReinforcedPBF REINFORCED_PBF;

    /** 无线能量传输塔控制器（注册名 {@code susyplusplus:wireless_energy_tower}）。 */
    public static MetaTileEntityWirelessEnergyTower WIRELESS_ENERGY_TOWER;

    /** 「存储检测器」单方块机器（注册名 {@code susyplusplus:storage_scanner}）。 */
    public static StorageScannerMachine STORAGE_SCANNER;

    // ------------------------------------------------------------------
    // 多方块存储 / 板条箱
    // ------------------------------------------------------------------

    /** 钢制多方块板条箱（1,000,000 物品）。 */
    public static MetaTileEntityMultiblockCrate STEEL_MULTIBLOCK_CRATE;

    /** 洁净不锈钢多方块储罐（16,000,000 mB）。 */
    public static SuMultiblockTank CLEAN_STAINLESS_STEEL_MULTIBLOCK_TANK;

    /** 洁净不锈钢多方块板条箱（16,000,000 物品）。 */
    public static MetaTileEntityMultiblockCrate CLEAN_STAINLESS_STEEL_MULTIBLOCK_CRATE;

    /** 加强钛多方块储罐（32,000,000 mB）。 */
    public static SuMultiblockTank REINFORCED_TITANIUM_MULTIBLOCK_TANK;

    /** 加强钛多方块板条箱（32,000,000 物品）。 */
    public static MetaTileEntityMultiblockCrate REINFORCED_TITANIUM_MULTIBLOCK_CRATE;

    /** 钢制物品阀门。 */
    public static MetaTileEntityItemValve STEEL_ITEM_VALVE;

    /** 洁净不锈钢储罐阀门。 */
    public static SuTankValve CLEAN_STAINLESS_STEEL_TANK_VALVE;

    /** 洁净不锈钢物品阀门。 */
    public static MetaTileEntityItemValve CLEAN_STAINLESS_STEEL_ITEM_VALVE;

    /** 加强钛储罐阀门。 */
    public static SuTankValve REINFORCED_TITANIUM_TANK_VALVE;

    /** 加强钛物品阀门。 */
    public static MetaTileEntityItemValve REINFORCED_TITANIUM_ITEM_VALVE;

    /** 流体样品存储 MV（32 格 × 32,000 L）。 */
    public static FluidSamplesStorageMachine FLUID_SAMPLES_STORAGE_MV;

    /** 流体样品存储 HV（32 格 × 64,000 L）。 */
    public static FluidSamplesStorageMachine FLUID_SAMPLES_STORAGE_HV;

    /** 流体样品存储 EV（32 格 × 128,000 L）。 */
    public static FluidSamplesStorageMachine FLUID_SAMPLES_STORAGE_EV;

    private SuMetaTileEntities() {
    }

    /** 幂等初始化；在模组 init 阶段调用（此时 GT 已注册完自己的 MTE）。 */
    public static void init() {
        registerReinforcedPbf();
        registerWirelessEnergyTower();
        registerStorageScanner();
        registerStorageUpgrades();
        registerFluidSamplesStorage();
    }

    /**
     * 流体样品存储（MV / HV / EV）：对齐 Susy-Core 的 {@code fluid_samples_storage}
     * （32 个独立储罐、不耗电、无物品槽），只是提供三档并按需求放大每格容量。
     *
     * <p>
     * 受 {@code enableFluidSamplesStorage} 控制。
     * </p>
     */
    private static void registerFluidSamplesStorage() {
        // 该机器（贴图/配方沿用 Susy-Core 的 fluid_samples_storage）在纯 GT 环境下不保证可用，
        // 因此 vanillaGtCompat 打开时一并禁用。
        if (SuConfig.vanillaGtCompat) {
            SusyPlusPlus.LOGGER.info(
                    "[SusyPlusPlus] Fluid Sample Storage machines are DISABLED (vanilla GT compat mode).");
            return;
        }
        if (!SuConfig.enableFluidSamplesStorage) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Fluid Sample Storage machines are DISABLED in config.");
            return;
        }
        if (FLUID_SAMPLES_STORAGE_MV == null) {
            FLUID_SAMPLES_STORAGE_MV = MetaTileEntities.registerMetaTileEntity(ID_FLUID_SAMPLES_STORAGE_MV,
                    new FluidSamplesStorageMachine(
                            new ResourceLocation(Tags.MOD_ID, "fluid_samples_storage_mv"),
                            TIER_MV, FLUID_SAMPLES_CAPACITY_MV));
        }
        if (FLUID_SAMPLES_STORAGE_HV == null) {
            FLUID_SAMPLES_STORAGE_HV = MetaTileEntities.registerMetaTileEntity(ID_FLUID_SAMPLES_STORAGE_HV,
                    new FluidSamplesStorageMachine(
                            new ResourceLocation(Tags.MOD_ID, "fluid_samples_storage_hv"),
                            TIER_HV, FLUID_SAMPLES_CAPACITY_HV));
        }
        if (FLUID_SAMPLES_STORAGE_EV == null) {
            FLUID_SAMPLES_STORAGE_EV = MetaTileEntities.registerMetaTileEntity(ID_FLUID_SAMPLES_STORAGE_EV,
                    new FluidSamplesStorageMachine(
                            new ResourceLocation(Tags.MOD_ID, "fluid_samples_storage_ev"),
                            TIER_EV, FLUID_SAMPLES_CAPACITY_EV));
        }
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Registered Fluid Sample Storage (MV/HV/EV, 32 tanks x 32000/64000/128000 L, ids {}-{})",
                ID_FLUID_SAMPLES_STORAGE_MV, ID_FLUID_SAMPLES_STORAGE_EV);
    }

    /** 强化土高炉：受 {@code enableReinforcedPbf} 控制。 */
    private static void registerReinforcedPbf() {
        if (REINFORCED_PBF != null) {
            return;
        }
        if (!SuConfig.enableReinforcedPbf) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Reinforced PBF is DISABLED in config.");
            return;
        }
        REINFORCED_PBF = MetaTileEntities.registerMetaTileEntity(ID_REINFORCED_PBF,
                new MetaTileEntityReinforcedPBF(new ResourceLocation(Tags.MOD_ID, "reinforced_pbf")));
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Registered MetaTileEntity susyplusplus:reinforced_pbf (id={}, parallel={})",
                ID_REINFORCED_PBF, SuConfig.reinforcedPbfParallel);
    }

    /** 存储检测器（MV）：受 {@code enableStorageScanner} 控制。 */
    private static void registerStorageScanner() {
        if (STORAGE_SCANNER != null) {
            return;
        }
        if (!SuConfig.enableStorageScanner) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Storage Scanner is DISABLED in config.");
            return;
        }
        STORAGE_SCANNER = MetaTileEntities.registerMetaTileEntity(ID_STORAGE_SCANNER,
                new StorageScannerMachine(new ResourceLocation(Tags.MOD_ID, "storage_scanner"), TIER_MV));
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Registered MetaTileEntity susyplusplus:storage_scanner (id={}, tier=MV)",
                ID_STORAGE_SCANNER);
    }

    /** 无线能量传输塔：受 {@code enableWirelessEnergyTower} 控制；适配原版 GT 时不注册。 */
    private static void registerWirelessEnergyTower() {
        if (WIRELESS_ENERGY_TOWER != null) {
            return;
        }
        if (SuConfig.vanillaGtCompat) {
            SusyPlusPlus.LOGGER.info(
                    "[SusyPlusPlus] Wireless Energy Tower is DISABLED (vanilla GT compat mode).");
            return;
        }
        if (!SuConfig.enableWirelessEnergyTower) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Wireless Energy Tower is DISABLED in config.");
            return;
        }
        WIRELESS_ENERGY_TOWER = MetaTileEntities.registerMetaTileEntity(ID_WIRELESS_ENERGY_TOWER,
                new MetaTileEntityWirelessEnergyTower(
                        new ResourceLocation(Tags.MOD_ID, "wireless_energy_tower")));
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Registered MetaTileEntity susyplusplus:wireless_energy_tower (id={})",
                ID_WIRELESS_ENERGY_TOWER);
    }

    /**
     * 多方块存储升级：钢/洁净不锈钢/加强钛的板条箱、储罐与两种阀门。
     *
     * <p>
     * 受 {@code enableMultiblockStorage} 控制。阀门<b>先注册</b>（控制器的结构模式里要引用它们），
     * 不过结构模式是延迟构建的，注册顺序其实不影响正确性。
     * </p>
     *
     * <p>
     * <b>钢制储罐与钢制储罐阀门不在本模组注册</b>：GT 本体已有
     * {@code gregtech:tank.steel}（id 10044）与 {@code gregtech:tank_valve.steel}（id 11524），
     * 按用户要求直接复用。
     * </p>
     */
    private static void registerStorageUpgrades() {
        if (!SuConfig.enableMultiblockStorage) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Multiblock storage upgrades are DISABLED in config.");
            return;
        }

        // ---- 阀门（物品阀门 / 储罐阀门） ----
        if (STEEL_ITEM_VALVE == null) {
            STEEL_ITEM_VALVE = MetaTileEntities.registerMetaTileEntity(ID_STEEL_ITEM_VALVE,
                    new MetaTileEntityItemValve(new ResourceLocation(Tags.MOD_ID, "steel_item_valve"),
                            SuStorageTier.STEEL));
        }
        if (CLEAN_STAINLESS_STEEL_TANK_VALVE == null) {
            CLEAN_STAINLESS_STEEL_TANK_VALVE = MetaTileEntities.registerMetaTileEntity(
                    ID_CLEAN_STAINLESS_STEEL_TANK_VALVE,
                    new SuTankValve(new ResourceLocation(Tags.MOD_ID, "clean_stainless_steel_tank_valve"),
                            SuStorageTier.CLEAN_STAINLESS_STEEL));
        }
        if (CLEAN_STAINLESS_STEEL_ITEM_VALVE == null) {
            CLEAN_STAINLESS_STEEL_ITEM_VALVE = MetaTileEntities.registerMetaTileEntity(
                    ID_CLEAN_STAINLESS_STEEL_ITEM_VALVE,
                    new MetaTileEntityItemValve(
                            new ResourceLocation(Tags.MOD_ID, "clean_stainless_steel_item_valve"),
                            SuStorageTier.CLEAN_STAINLESS_STEEL));
        }
        if (REINFORCED_TITANIUM_TANK_VALVE == null) {
            REINFORCED_TITANIUM_TANK_VALVE = MetaTileEntities.registerMetaTileEntity(
                    ID_REINFORCED_TITANIUM_TANK_VALVE,
                    new SuTankValve(new ResourceLocation(Tags.MOD_ID, "reinforced_titanium_tank_valve"),
                            SuStorageTier.REINFORCED_TITANIUM));
        }
        if (REINFORCED_TITANIUM_ITEM_VALVE == null) {
            REINFORCED_TITANIUM_ITEM_VALVE = MetaTileEntities.registerMetaTileEntity(
                    ID_REINFORCED_TITANIUM_ITEM_VALVE,
                    new MetaTileEntityItemValve(
                            new ResourceLocation(Tags.MOD_ID, "reinforced_titanium_item_valve"),
                            SuStorageTier.REINFORCED_TITANIUM));
        }

        // 自定义 Ability 登记（让 JEI / 结构预览知道该能力对应哪些方块；结构匹配本身不依赖它）
        registerItemValveAbility(STEEL_ITEM_VALVE);
        registerItemValveAbility(CLEAN_STAINLESS_STEEL_ITEM_VALVE);
        registerItemValveAbility(REINFORCED_TITANIUM_ITEM_VALVE);

        // ---- 控制器：储罐 ----
        if (CLEAN_STAINLESS_STEEL_MULTIBLOCK_TANK == null) {
            CLEAN_STAINLESS_STEEL_MULTIBLOCK_TANK = MetaTileEntities.registerMetaTileEntity(
                    ID_CLEAN_STAINLESS_STEEL_MULTIBLOCK_TANK,
                    new SuMultiblockTank(
                            new ResourceLocation(Tags.MOD_ID, "clean_stainless_steel_multiblock_tank"),
                            SuStorageTier.CLEAN_STAINLESS_STEEL));
        }
        if (REINFORCED_TITANIUM_MULTIBLOCK_TANK == null) {
            REINFORCED_TITANIUM_MULTIBLOCK_TANK = MetaTileEntities.registerMetaTileEntity(
                    ID_REINFORCED_TITANIUM_MULTIBLOCK_TANK,
                    new SuMultiblockTank(
                            new ResourceLocation(Tags.MOD_ID, "reinforced_titanium_multiblock_tank"),
                            SuStorageTier.REINFORCED_TITANIUM));
        }

        // ---- 控制器：板条箱 ----
        if (STEEL_MULTIBLOCK_CRATE == null) {
            STEEL_MULTIBLOCK_CRATE = MetaTileEntities.registerMetaTileEntity(ID_STEEL_MULTIBLOCK_CRATE,
                    new MetaTileEntityMultiblockCrate(
                            new ResourceLocation(Tags.MOD_ID, "steel_multiblock_crate"),
                            SuStorageTier.STEEL));
        }
        if (CLEAN_STAINLESS_STEEL_MULTIBLOCK_CRATE == null) {
            CLEAN_STAINLESS_STEEL_MULTIBLOCK_CRATE = MetaTileEntities.registerMetaTileEntity(
                    ID_CLEAN_STAINLESS_STEEL_MULTIBLOCK_CRATE,
                    new MetaTileEntityMultiblockCrate(
                            new ResourceLocation(Tags.MOD_ID, "clean_stainless_steel_multiblock_crate"),
                            SuStorageTier.CLEAN_STAINLESS_STEEL));
        }
        if (REINFORCED_TITANIUM_MULTIBLOCK_CRATE == null) {
            REINFORCED_TITANIUM_MULTIBLOCK_CRATE = MetaTileEntities.registerMetaTileEntity(
                    ID_REINFORCED_TITANIUM_MULTIBLOCK_CRATE,
                    new MetaTileEntityMultiblockCrate(
                            new ResourceLocation(Tags.MOD_ID, "reinforced_titanium_multiblock_crate"),
                            SuStorageTier.REINFORCED_TITANIUM));
        }

        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Registered multiblock storage upgrades: 3 crates, 2 tanks, 5 valves (ids {}-{})",
                ID_STEEL_MULTIBLOCK_CRATE, ID_REINFORCED_TITANIUM_ITEM_VALVE);
    }

    private static void registerItemValveAbility(MetaTileEntityItemValve valve) {
        if (valve != null) {
            MultiblockAbility.registerMultiblockAbility(SuStorageAbilities.ITEM_VALVE, valve);
        }
    }
}
