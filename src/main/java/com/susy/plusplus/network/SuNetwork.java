package com.susy.plusplus.network;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.Tags;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 本模组的简易网络层。
 *
 * <p>
 * 只有一条消息（客户端 → 服务端）：请求打开配置器界面。
 * </p>
 *
 * <p>
 * <b>为什么必须有网络层</b>：ModularUI
 * 的同步型界面（{@code SimpleGuiFactory#open(EntityPlayerMP)}）
 * 必须由<b>服务端</b>发起；而「Shift+V」是纯客户端的按键事件。因此客户端按键后
 * 发一条空包，由服务端调用 {@code GuiManager#open(...)} ——
 * 这样界面仍然只显示在客户端，但面板状态与 NBT 写入都在服务端，不会出现"只在本地改了 NBT"的假象。
 * </p>
 */
public final class SuNetwork {

    /** 频道名（1.12.2 的 SimpleNetworkWrapper 会把它拼上 modid 前缀）。 */
    private static final String CHANNEL_NAME = Tags.MOD_ID;

    private static SimpleNetworkWrapper instance;
    private static int nextMessageId = 0;

    private SuNetwork() {
    }

    /** 在 {@code preInit} 调用（客户端与服务端都要）。 */
    public static void init() {
        if (instance != null) {
            return;
        }
        instance = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        instance.registerMessage(PacketOpenConfigurator.Handler.class, PacketOpenConfigurator.class,
                nextMessageId++, Side.SERVER);
        SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Network channel '{}' registered.", CHANNEL_NAME);
    }

    public static SimpleNetworkWrapper get() {
        return instance;
    }

    /** 客户端 → 服务端。 */
    public static void sendToServer(IMessage message) {
        if (instance == null) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Network not initialised; message {} dropped.",
                    message.getClass().getSimpleName());
            return;
        }
        instance.sendToServer(message);
    }
}
