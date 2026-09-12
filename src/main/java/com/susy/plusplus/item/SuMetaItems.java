package com.susy.plusplus.item;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;
import com.susy.plusplus.item.battery.BatteryCaseBaubles;
import com.susy.plusplus.item.battery.ItemBatteryCase;

import gregtech.api.items.metaitem.MetaItem;

import net.minecraftforge.fml.common.Loader;

/**
 * 本模组的 MetaItem 注册入口。
 *
 * <p>
 * 注意：必须在本模组的 preInit 中调用 {@link #init()}（此时 GT 已初始化完
 * {@code gregtech.common.items.MetaItems}），才能安全地引用 GT 的物品。
 * </p>
 *
 * <p>
 * 每个物品都受 {@link SuConfig} 的开关控制（默认全开）。
 * 被关闭时对应的静态字段会保持 {@code null}，其它代码必须做空判断。
 * </p>
 */
public final class SuMetaItems {

    /** 是否已经执行过初始化（不能只靠字段判断——物品可能因配置被关闭而保持 null）。 */
    private static boolean initialized = false;

    // ---------- 防水喷漆 ----------

    public static ItemWaterproofSprayCan WATERPROOF_SPRAY_ITEM;
    public static MetaItem<?>.MetaValueItem WATERPROOF_SPRAY_CAN;

    // ---------- 电池盒 ----------

    public static ItemBatteryCase BATTERY_CASE_ITEM;
    public static MetaItem<?>.MetaValueItem BATTERY_CASE;

    private SuMetaItems() {
    }

    /** 幂等初始化。 */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // ---------- 防水喷漆 ----------
        if (SuConfig.enableWaterproofSprayCan) {
            WATERPROOF_SPRAY_ITEM = new ItemWaterproofSprayCan();
            WATERPROOF_SPRAY_CAN = WATERPROOF_SPRAY_ITEM.sprayCan;
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered item: susyplusplus:waterproof_spray_can");
        } else {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Waterproof spray can is DISABLED in config.");
        }

        // ---------- 电池盒 ----------
        if (SuConfig.enableBatteryCase) {
            BATTERY_CASE_ITEM = new ItemBatteryCase();
            BATTERY_CASE = BATTERY_CASE_ITEM.batteryCase;

            // Baubles（饰品栏）兼容：仅当 Baubles 已加载时才触碰其 API
            // （与 GT 的 BaublesModule 相同的隔离做法）。
            if (Loader.isModLoaded("baubles")) {
                BatteryCaseBaubles.addTrinket(BATTERY_CASE);
            }
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered item: susyplusplus:battery_case");
        } else {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Battery case is DISABLED in config.");
        }
    }
}
