package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;
import com.susy.plusplus.item.configurator.ConfiguratorData;
import com.susy.plusplus.item.configurator.ConfiguratorMode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;

/**
 * 配置器主界面：3 个模式按钮。
 *
 * <ul>
 * <li>按钮 1 修改机器输出面 / 按钮 2 复制机器配置 / 按钮 3 机器工具箱；</li>
 * <li><b>按下按钮 = 退出 UI + 把模式写入配置器 NBT</b>
 * （{@link ConfiguratorMode#write(ItemStack)} 会先清空旧 NBT）；</li>
 * <li>esc → 关闭界面（MUI2 主面板默认行为）。</li>
 * </ul>
 *
 * <p>
 * 界面由<b>服务端</b>发起（{@link #open(EntityPlayerMP)}），因此按钮的服务端动作
 * （{@link InteractionSyncHandler}）可以直接写物品 NBT —— 不会出现"只在本地改了 NBT"。
 * </p>
 */
public final class ConfiguratorMainUI implements IGuiHolder<GuiData> {

    /**
     * 工厂名（两端必须一致）。
     *
     * <p>
     * ⚠ ModularUI 限制工厂名 ≤ 32 字符（{@code GuiManager#registerFactory} 会直接抛异常），
     * 所以用短名 {@code susyplusplus:cfg_main}。
     * </p>
     */
    public static final String FACTORY_NAME = Tags.MOD_ID + ":cfg_main";

    private static final String PANEL_NAME = "configurator_main";
    private static final int WIDTH = 176;
    private static final int HEIGHT = 132;
    private static final int BUTTON_X = 8;
    private static final int BUTTON_W = WIDTH - BUTTON_X * 2;
    private static final int BUTTON_H = 22;
    private static final int FIRST_BUTTON_Y = 30;
    private static final int BUTTON_GAP = 26;

    private static SimpleGuiFactory factory;

    /** 在公共 init 阶段调用（客户端与服务端各构造一次，名字必须一致）。 */
    public static void init() {
        if (factory == null) {
            factory = GuiFactories.createSimple(FACTORY_NAME, new ConfiguratorMainUI());
        }
    }

    /** 服务端调用：打开主界面。 */
    public static void open(EntityPlayerMP player) {
        init();
        factory.open(player);
    }

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        final EntityPlayer player = data.getPlayer();
        final ItemStack configurator = SuGuiFactories.findConfigurator(player);
        final ConfiguratorMode current = ConfiguratorMode.get(configurator);

        ModularPanel panel = ModularPanel.defaultPanel(PANEL_NAME, WIDTH, HEIGHT);
        panel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BACKGROUND));
        // 光标停在非按钮区域时，不要让主题的"悬停背景"盖住半透明底色
        panel.disableHoverBackground();

        panel.child(SuGuiFactories.title("susyplusplus.gui.configurator.title")
                .left(BUTTON_X).top(8));

        ConfiguratorMode[] modes = { ConfiguratorMode.MODIFY_OUTPUT, ConfiguratorMode.COPY_CONFIG,
                ConfiguratorMode.MACHINE_TOOLBOX };
        int y = FIRST_BUTTON_Y;
        for (ConfiguratorMode mode : modes) {
            panel.child(modeButton(panel, player, mode, current, y));
            y += BUTTON_GAP;
        }

        panel.child(SuGuiFactories.text(configurator.isEmpty() ? "susyplusplus.gui.configurator.no_item"
                : "susyplusplus.gui.configurator.hint").left(BUTTON_X).top(y + 2));

        return panel;
    }

    private static ButtonWidget<?> modeButton(final ModularPanel panel, final EntityPlayer player,
            final ConfiguratorMode mode, ConfiguratorMode current, int y) {
        ButtonWidget button = new ButtonWidget();
        button.left(BUTTON_X).top(y).width(BUTTON_W).height(BUTTON_H);
        button.background(new Rectangle().setColor(mode == current ? SuGuiFactories.COLOR_SELECTED
                : SuGuiFactories.COLOR_BUTTON));
        button.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        // 白色粗体文字
        button.child(SuGuiFactories.buttonLabel(mode.getLangKey(), null).left(8).top(8));

        InteractionSyncHandler sync = new InteractionSyncHandler();
        sync.setOnMousePressed(action -> {
            // 服务端动作：写入模式（会清空旧 NBT），然后关闭界面
            ItemStack held = SuGuiFactories.findConfigurator(player);
            if (!held.isEmpty()) {
                ConfiguratorData.setMode(held, mode);
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.mode_set",
                        new TextComponentTranslation(mode.getLangKey())), true);
            }
            panel.closeIfOpen();
        });
        button.syncHandler(sync);
        return button;
    }
}
