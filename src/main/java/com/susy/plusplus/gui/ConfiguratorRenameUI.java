package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;
import com.susy.plusplus.machine.MachineNameHelper;
import com.susy.plusplus.network.PacketRenameMachine;
import com.susy.plusplus.network.SuNetwork;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/**
 * 「重命名机器」界面（配置器模式 5：选中 RENAME 后右键机器打开）。
 *
 * <h3>为什么不再直接用"配置器的铁砧名"</h3>
 *
 * <p>
 * 旧行为是右键瞬间把配置器的铁砧名写进机器，无法输入、也无法只清空名字。
 * 现在改成打开本界面，提供：
 * </p>
 *
 * <ul>
 * <li><b>输入框</b>：预填当前名字；</li>
 * <li><b>应用</b>：把输入框内容写入 {@code CustomName}（空输入 = 清除）；</li>
 * <li><b>清空名称</b>：无条件清除 {@code CustomName}（等价于删除该 NBT 键）；</li>
 * <li><b>关闭</b>：不做任何修改。</li>
 * </ul>
 *
 * <p>
 * 写入动作走 {@link PacketRenameMachine}（客户端发包 → 服务端执行），
 * 服务端写完再回 {@code PacketSyncMachineName} 让客户端<b>立刻</b>刷新悬浮名牌。
 * </p>
 */
public final class ConfiguratorRenameUI implements IGuiHolder<PosGuiData> {

    /** 工厂名（≤32 字符，两端一致）。 */
    public static final String FACTORY_NAME = Tags.MOD_ID + ":cfg_rename";

    private static final String PANEL_NAME = "configurator_rename";
    private static final int WIDTH = 220;
    private static final int HEIGHT = 100;

    private static final int MARGIN = 8;
    private static final int FIELD_TOP = 36;
    private static final int FIELD_H = 18;
    private static final int BUTTON_TOP = 62;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_W = 62;

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        final BlockPos pos = data.getBlockPos();
        // ⚠ 不能用 data.getPlayer().world（字段声明在 Entity 上，RFG reobf 不沿继承链映射）；
        // 转成 Entity 后调 getEntityWorld()，映射一定存在。
        final World world = ((net.minecraft.entity.Entity) data.getPlayer()).getEntityWorld();
        final String currentName = MachineNameHelper.getName(world, pos);

        ModularPanel panel = ModularPanel.defaultPanel(PANEL_NAME, WIDTH, HEIGHT);
        panel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BACKGROUND));
        // 光标停在非控件区域时不要让主题悬停背景盖住半透明底色
        panel.disableHoverBackground();

        panel.child(SuGuiFactories.title("susyplusplus.gui.configurator.rename.title")
                .left(MARGIN).top(6));
        panel.child(SuGuiFactories.text("susyplusplus.gui.configurator.rename.hint")
                .left(MARGIN).top(20));

        // 名字输入框（预填当前名字；服务端构建时读到的是权威值，客户端是已同步的值）
        TextFieldWidget nameField = new TextFieldWidget();
        nameField.left(MARGIN).top(FIELD_TOP).width(WIDTH - MARGIN * 2).height(FIELD_H);
        nameField.setText(currentName == null ? "" : currentName);
        panel.child(nameField);

        // 应用
        panel.child(button("susyplusplus.gui.configurator.confirm", SuGuiFactories.COLOR_ENABLED,
                MARGIN, () -> {
                    String text = nameField.getText();
                    send(pos, world, text == null ? "" : text.trim());
                    panel.closeIfOpen();
                }));

        // 清空名称（无条件空串 → 服务端删除 CustomName）
        panel.child(button("susyplusplus.gui.configurator.rename.clear", SuGuiFactories.COLOR_BUTTON,
                MARGIN + BUTTON_W + 6, () -> {
                    send(pos, world, "");
                    panel.closeIfOpen();
                }));

        // 关闭
        panel.child(button("susyplusplus.gui.configurator.cancel", SuGuiFactories.COLOR_BUTTON,
                MARGIN + (BUTTON_W + 6) * 2, panel::closeIfOpen));

        return panel;
    }

    /** 只有客户端发一次包；服务端这一侧不做任何事（避免出现"两端各写一次"）。 */
    private static void send(BlockPos pos, World world, String name) {
        if (world != null && world.isRemote) {
            SuNetwork.sendToServer(new PacketRenameMachine(pos, name));
        }
    }

    private static ButtonWidget<?> button(String langKey, int color, int x, Runnable onClick) {
        ButtonWidget button = new ButtonWidget();
        button.left(x).top(BUTTON_TOP).width(BUTTON_W).height(BUTTON_H);
        button.background(new Rectangle().setColor(color));
        button.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        button.child(SuGuiFactories.buttonLabel(langKey, null).left(10).top(7));

        InteractionSyncHandler sync = new InteractionSyncHandler();
        sync.setOnMousePressed(action -> onClick.run());
        button.syncHandler(sync);
        return button;
    }
}
