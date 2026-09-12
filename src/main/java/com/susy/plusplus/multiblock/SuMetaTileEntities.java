package com.susy.plusplus.multiblock;

import com.susy.plusplus.Tags;
import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;
import com.susy.plusplus.multiblock.wireless.MetaTileEntityWirelessEnergyTower;

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
     * 故此处取 32100。若将来冲突，改这一个常量即可。
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

    /** 强化土高炉控制器（注册名 {@code susyplusplus:reinforced_pbf}）。 */
    public static MetaTileEntityReinforcedPBF REINFORCED_PBF;

    /** 无线能量传输塔控制器（注册名 {@code susyplusplus:wireless_energy_tower}）。 */
    public static MetaTileEntityWirelessEnergyTower WIRELESS_ENERGY_TOWER;

    private SuMetaTileEntities() {
    }

    /** 幂等初始化；在模组 init 阶段调用（此时 GT 已注册完自己的 MTE）。 */
    public static void init() {
        registerReinforcedPbf();
        registerWirelessEnergyTower();
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

    /** 无线能量传输塔：受 {@code enableWirelessEnergyTower} 控制。 */
    private static void registerWirelessEnergyTower() {
        if (WIRELESS_ENERGY_TOWER != null) {
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
}
