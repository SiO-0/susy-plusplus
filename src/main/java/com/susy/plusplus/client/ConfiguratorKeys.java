package com.susy.plusplus.client;

import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.network.PacketOpenConfigurator;
import com.susy.plusplus.network.SuNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

/**
 * 配置器的快捷键：<b>Shift + V</b> 打开主界面。
 *
 * <p>
 * 只做客户端该做的事：判断按键 + 手里是否拿着配置器，然后发一条空包给服务端，
 * 由服务端打开 ModularUI 界面（见 {@link PacketOpenConfigurator}）。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ConfiguratorKeys {

    /** 按键分类（本地化键：{@code key.categories.susyplusplus}）。 */
    public static final String CATEGORY = "key.categories.susyplusplus";

    /** 按键名（本地化键：{@code key.susyplusplus.configurator.open}）。 */
    public static final String KEY_OPEN = "key.susyplusplus.configurator.open";

    private static KeyBinding openConfigurator;

    private ConfiguratorKeys() {
    }

    /** 由 {@code SuClientEvents.init()} 在客户端调用。 */
    public static void init() {
        if (openConfigurator != null) {
            return;
        }
        // 绑定 V（Shift 由代码判断，保证"Shift+V"这个组合稳定不变）
        openConfigurator = new KeyBinding(KEY_OPEN, KeyConflictContext.UNIVERSAL, Keyboard.KEY_V, CATEGORY);
        ClientRegistry.registerKeyBinding(openConfigurator);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openConfigurator == null || !openConfigurator.isPressed()) {
            return;
        }
        // 需求：Shift + V
        if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.currentScreen != null) {
            return;
        }
        // 手里要拿着配置器（客户端先筛一次，避免无意义的包）
        ItemStack held = minecraft.player.getHeldItemMainhand();
        if (held.isEmpty() || SuMetaItems.CONFIGURATOR == null || !SuMetaItems.CONFIGURATOR.isItemEqual(held)) {
            held = minecraft.player.getHeldItemOffhand();
            if (held.isEmpty() || SuMetaItems.CONFIGURATOR == null || !SuMetaItems.CONFIGURATOR.isItemEqual(held)) {
                return;
            }
        }
        SuNetwork.sendToServer(new PacketOpenConfigurator());
    }
}
