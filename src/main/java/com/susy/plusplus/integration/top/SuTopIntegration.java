package com.susy.plusplus.integration.top;

import com.susy.plusplus.SusyPlusPlus;

import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.ITheOneProbe;

import net.minecraftforge.fml.common.Loader;

/**
 * The One Probe 集成入口。
 *
 * <p>
 * 注册方式与 GT 自身的 {@code TheOneProbeModule} 完全一致：在 init 阶段取
 * {@code TheOneProbe.theOneProbeImp} 并 {@code registerProvider(...)}。
 * </p>
 */
public final class SuTopIntegration {

    private static final String TOP_MODID = "theoneprobe";

    private SuTopIntegration() {
    }

    public static void init() {
        if (!Loader.isModLoaded(TOP_MODID)) {
            return;
        }
        ITheOneProbe oneProbe = TheOneProbe.theOneProbeImp;
        if (oneProbe == null) {
            SusyPlusPlus.LOGGER.warn("The One Probe 已加载，但 theOneProbeImp 为 null，跳过 TOP 集成。");
            return;
        }
        oneProbe.registerProvider(new WaterproofInfoProvider());
        SusyPlusPlus.LOGGER.info("已注册 The One Probe 防水状态显示。");

        oneProbe.registerProvider(new WirelessTowerInfoProvider());
        SusyPlusPlus.LOGGER.info("已注册 The One Probe 无线能量传输塔进度显示。");
    }
}
