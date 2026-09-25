package com.susy.plusplus.config;

import com.susy.plusplus.Tags;

import net.minecraftforge.common.config.Config;

/**
 * Susy Plus Plus 的配置文件。
 *
 * <p>
 * 使用 Forge 1.12.2 的注解式配置（{@link Config}）。Forge 的 {@code ConfigManager}
 * 会在模组构造阶段自动读取/生成配置文件：
 * </p>
 *
 * <pre>
 * .minecraft/config/susyplusplus.cfg
 * </pre>
 *
 * <p>
 * 这些都是<b>加载期开关</b>：改动后需要<b>重启游戏</b>才会生效
 * （物品/方块/机器的注册无法在运行时热插拔）。
 * </p>
 *
 * <p>
 * 注意：即使对应物品被关闭，其依赖的材料/纹理也不会出错 ——
 * 只是不再注册物品、机器与相关配方。
 * </p>
 */
@Config(modid = Tags.MOD_ID)
public class SuConfig {

        @Config.Comment({
                        "Enable the Waterproof Spray Can item (and its canner/mixer recipes).",
                        "启用防水喷漆物品（以及灌装机/搅拌机的相关配方）。",
                        "Default: true"
        })
        public static boolean enableWaterproofSprayCan = true;

        @Config.Comment({
                        "Enable the Battery Case item (and its assembler recipe).",
                        "启用电池盒物品（以及其组装机配方）。",
                        "Default: true"
        })
        public static boolean enableBatteryCase = true;

        @Config.Comment({
                        "Enable the Reinforced PBF multiblock machine (and its crafting recipe).",
                        "强化土高炉多方块机器（以及其工作台配方）。",
                        "Note: the Reinforced Firebrick block is always available.",
                        "注意：强化耐火砖方块始终可用。",
                        "Default: true"
        })
        public static boolean enableReinforcedPbf = true;

        @Config.Comment({
                        "Parallel count of the Reinforced PBF.",
                        "强化土高炉的并行数。",
                        "Range: 1 ~ 64, Default: 4"
        })
        @Config.RangeInt(min = 1, max = 64)
        public static int reinforcedPbfParallel = 4;

        @Config.Comment({
                        "Enable the Wireless Energy Transmission Tower multiblock machine.",
                        "启用无线能量传输塔多方块机器。",
                        "Note: it needs a Susy-Core cargo drone (renamed with an anvil to \"x y z\")",
                        "      and GT batteries in the input bus.",
                        "注意：需要 Susy-Core 的货运无人机（用铁砧改名为「x y z」）与输入总线里的 GT 电池。",
                        "Default: true"
        })
        public static boolean enableWirelessEnergyTower = true;

        @Config.Comment({
                        "Enable the Configurator item (Shift+V opens its UI).",
                        "启用配置器物品（Shift+V 打开界面）。",
                        "Default: true"
        })
        public static boolean enableConfigurator = true;

        @Config.Comment({
                        "Enable the Trolley item: Shift+right-click a GT machine to pick it up (no drops),",
                        "right-click to put it back down (full NBT is kept, including caches and covers).",
                        "Multiblock controllers / multiblock parts cannot be picked up.",
                        "启用手推车物品：Shift+右键 GT 机器搬起（零掉落），右键放下",
                        "（完整保留 NBT，含缓存与封面）。多方块控制器 / 多方块部件不可搬起。",
                        "Default: true"
        })
        public static boolean enableTrolley = true;

        @Config.Comment({
                        "Enable the Storage Scanner machine (MV).",
                        "It scans nearby block entities that expose an item inventory (chests, furnaces,",
                        "GT machines, other mods' containers - including non player placed ones) and",
                        "exposes them as ONE aggregate inventory, so hoppers can pull items out of it.",
                        "启用存储检测器机器（MV）：扫描周围带物品库存的方块实体（箱子/熔炉/GT 机器/",
                        "其它 mod 容器，含非玩家放置的），并聚合成一个库存对外暴露，漏斗可直接抽取。",
                        "Default: true"
        })
        public static boolean enableStorageScanner = true;

        @Config.Comment({
                        "Storage Scanner: de-duplicate containers that share ONE inventory.",
                        "Multiblock storages (e.g. Industrial Renewal's storage rank) return the SAME",
                        "inventory from every block entity, which would make the scanned items count",
                        "N times (N = number of blocks). Enable to count such shared inventories once.",
                        "存储检测器：对“共享同一份库存”的容器去重。",
                        "多方块存储（例如工业复兴 storage rank）的每个方块实体都返回同一份库存，",
                        "不去重会把扫到的物品按方块数重复计算 N 倍。",
                        "Default: true"
        })
        public static boolean storageScannerDedupeMultiblockStorage = true;

        @Config.Comment({
                        "Rubber pipe tweaks:",
                        "  1) make the Rubber fluid pipe's throughput equal to Steel's;",
                        "  2) add Alloy Smelter recipes for rubber fluid pipes",
                        "     (rubber rods + matching STEEL extruder shape, 7 EU/t, shape NOT consumed).",
                        "橡胶管道修改：",
                        "  1) 把橡胶流体管道的速率改成与钢一致；",
                        "  2) 添加合金炉配方（橡胶条 + 对应【钢】模头（不消耗），7 EU/t）产出各尺寸橡胶流体管道。",
                        "Default: false"
        })
        public static boolean enableRubberPipeTweaks = false;

        @Config.Comment({
                        "Pyrotech (火种科技) recipe tweaks: the Dryer / Extractor / Forge Hammer recipes added by this mod.",
                        "本模组添加的火种科技(Pyrotech)相关配方（干燥机 / 提取机 / 锻造锤）。",
                        "Default: true"
        })
        public static boolean enablePyrotechRecipeTweaks = true;
}
