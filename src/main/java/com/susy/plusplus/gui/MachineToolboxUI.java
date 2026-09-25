package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.metatileentity.multiblock.IMaintenance;
import gregtech.common.metatileentities.electric.MetaTileEntityTransformer;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 「机器工具箱」界面（配置器模式 3，普通右键机器后打开）。
 *
 * <p>
 * <b>只添加这台机器真正支持的按钮</b>：不支持的按钮完全不创建 →
 * 不会留空位，面板高度也按实际按钮数计算（列表自动向上收拢）。
 * </p>
 *
 * <table border="1">
 * <caption>按钮与源码 API 的对应</caption>
 * <tr>
 * <th>按钮</th>
 * <th>API（已在编译用 GT jar 中核实）</th>
 * </tr>
 * <tr>
 * <td>音效</td>
 * <td>{@code MetaTileEntity#isMuffled()} / {@code toggleMuffled()}</td>
 * </tr>
 * <tr>
 * <td>机器开关</td>
 * <td>{@code CAPABILITY_CONTROLLABLE} →
 * {@code IControllable#isWorkingEnabled/setWorkingEnabled}</td>
 * </tr>
 * <tr>
 * <td>输出口允许输入</td>
 * <td>{@code SimpleMachineMetaTileEntity#is/setAllowInputFromOutputSideItems/Fluids}</td>
 * </tr>
 * <tr>
 * <td>变压器模式</td>
 * <td>{@code MetaTileEntityTransformer#isInverted/setTransformUp(boolean)}</td>
 * </tr>
 * <tr>
 * <td>修复维护问题</td>
 * <td>{@code CAPABILITY_MAINTENANCE} →
 * {@code IMaintenance#setMaintenanceFixed(int)}（0~5 全修）</td>
 * </tr>
 * </table>
 *
 * <p>
 * <b>已移除「维护仓贴胶带」按钮</b>：{@code IMaintenanceHatch#setTaped(boolean)} 只是一次性贴住，
 * 而该接口<b>没有任何读取"是否已贴"的方法</b> —— 界面既无法反映状态、也无法撤销，
 * 所以按需求只保留<b>多方块控制器</b>侧的「修复维护问题」
 * （维护问题的修复在控制器侧的 {@code IMaintenance}，不在维护仓）。
 * </p>
 *
 * <p>
 * 所有按钮<b>按下即生效</b>（服务端动作），因此界面里<b>没有</b>确认按钮。
 * </p>
 *
 * <p>
 * ⚠ {@code InteractionSyncHandler} 的回调在<b>客户端与服务端都会执行</b>
 * （本地先跑一次 + 同步到服务端再跑一次）。这里之所以安全，是因为 GT 的
 * {@code setOutputFacing* / setAutoOutput* / setAllowInputFromOutputSide* / toggleMuffled}
 * 全都自带 {@code !getWorld().isRemote} 守卫，客户端调用只会改本地字段、不会发包。
 * </p>
 */
public final class MachineToolboxUI implements IGuiHolder<PosGuiData> {

        /**
         * GUI 工厂名。
         *
         * <p>
         * ⚠ ModularUI 要求工厂名 ≤ 32 字符：{@code susyplusplus:configurator_toolbox} 是 33 个字符，
         * 会让 {@code GuiManager#registerFactory} 抛异常并导致 preInit 崩溃 —— 所以用短名。
         * </p>
         */
        public static final String FACTORY_NAME = Tags.MOD_ID + ":cfg_toolbox";

        private static final String PANEL_NAME = "configurator_toolbox";
        private static final int PANEL_WIDTH = 240;
        private static final int BTN_X = 10;
        private static final int BTN_W = PANEL_WIDTH - BTN_X * 2;
        private static final int BTN_H = 22;
        private static final int FIRST_Y = 30;
        private static final int GAP = 26;
        private static final int FOOTER = 24;

        @Override
        public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
                final MetaTileEntity mte = MachineFaceUI.machine(data);
                final EntityPlayer player = data.getPlayer();

                // 1) 只在机器支持时才准备对应按钮
                final List<ToolboxEntry> entries = new ArrayList<>();
                if (mte != null) {
                        // 音效：任何机器都能静音
                        entries.add(new ToolboxEntry("susyplusplus.gui.configurator.toolbox.sound",
                                        () -> state(mte.isMuffled()),
                                        machine -> machine.toggleMuffled()));

                        final IControllable controllable = capability(mte,
                                        GregtechTileCapabilities.CAPABILITY_CONTROLLABLE);
                        if (controllable != null) {
                                entries.add(new ToolboxEntry("susyplusplus.gui.configurator.toolbox.work",
                                                () -> {
                                                        IControllable target = capability(mte,
                                                                        GregtechTileCapabilities.CAPABILITY_CONTROLLABLE);
                                                        return target == null ? DASH : state(target.isWorkingEnabled());
                                                },
                                                machine -> {
                                                        IControllable target = capability(machine,
                                                                        GregtechTileCapabilities.CAPABILITY_CONTROLLABLE);
                                                        if (target != null) {
                                                                target.setWorkingEnabled(!target.isWorkingEnabled());
                                                        }
                                                }));
                        }

                        if (mte instanceof SimpleMachineMetaTileEntity) {
                                entries.add(new ToolboxEntry("susyplusplus.gui.configurator.toolbox.allow_input",
                                                () -> {
                                                        if (!(mte instanceof SimpleMachineMetaTileEntity)) {
                                                                return DASH;
                                                        }
                                                        SimpleMachineMetaTileEntity simple = (SimpleMachineMetaTileEntity) mte;
                                                        return I18n.format(simple.isAllowInputFromOutputSideItems()
                                                                        ? "susyplusplus.gui.configurator.state.allowed"
                                                                        : "susyplusplus.gui.configurator.state.denied");
                                                },
                                                machine -> {
                                                        if (machine instanceof SimpleMachineMetaTileEntity) {
                                                                SimpleMachineMetaTileEntity simple = (SimpleMachineMetaTileEntity) machine;
                                                                boolean allow = !simple
                                                                                .isAllowInputFromOutputSideItems();
                                                                simple.setAllowInputFromOutputSideItems(allow);
                                                                simple.setAllowInputFromOutputSideFluids(allow);
                                                        }
                                                }));
                        }

                        if (mte instanceof MetaTileEntityTransformer) {
                                entries.add(new ToolboxEntry("susyplusplus.gui.configurator.toolbox.transformer",
                                                () -> {
                                                        if (!(mte instanceof MetaTileEntityTransformer)) {
                                                                return DASH;
                                                        }
                                                        return I18n.format(
                                                                        ((MetaTileEntityTransformer) mte).isInverted()
                                                                                        ? "susyplusplus.gui.configurator.state.step_up"
                                                                                        : "susyplusplus.gui.configurator.state.step_down");
                                                },
                                                machine -> {
                                                        if (machine instanceof MetaTileEntityTransformer) {
                                                                MetaTileEntityTransformer transformer = (MetaTileEntityTransformer) machine;
                                                                transformer.setTransformUp(!transformer.isInverted());
                                                        }
                                                }));
                        }

                        // 只有多方块控制器实现了 IMaintenance；单方块机器没有该能力 → 按钮自动不出现
                        final IMaintenance maintenance = capability(mte,
                                        GregtechTileCapabilities.CAPABILITY_MAINTENANCE);
                        if (maintenance != null) {
                                entries.add(new ToolboxEntry("susyplusplus.gui.configurator.toolbox.maintenance_fix",
                                                () -> {
                                                        IMaintenance target = capability(mte,
                                                                        GregtechTileCapabilities.CAPABILITY_MAINTENANCE);
                                                        return target == null ? DASH
                                                                        : I18n.format("susyplusplus.gui.configurator.state.problems",
                                                                                        target.getNumMaintenanceProblems());
                                                },
                                                machine -> {
                                                        IMaintenance target = capability(machine,
                                                                        GregtechTileCapabilities.CAPABILITY_MAINTENANCE);
                                                        if (target != null) {
                                                                // 0~5 全调一次 = 修好全部维护问题
                                                                for (int i = 0; i < 6; i++) {
                                                                        target.setMaintenanceFixed(i);
                                                                }
                                                        }
                                                }));
                        }
                }

                // 2) 面板高度按实际按钮数计算
                int rows = Math.max(1, entries.size());
                int panelHeight = FIRST_Y + rows * GAP + FOOTER;
                ModularPanel panel = ModularPanel.defaultPanel(PANEL_NAME, PANEL_WIDTH, panelHeight);
                panel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BACKGROUND));
                // 光标停在非按钮区域时不要让主题的“悬停背景”盖住半透明底色
                panel.disableHoverBackground();
                panel.child(SuGuiFactories.title("susyplusplus.gui.configurator.toolbox.title").left(BTN_X).top(8));

                // 3) 依次排布（不支持的按钮根本不存在 → 不会留下空位）
                int y = FIRST_Y;
                for (ToolboxEntry entry : entries) {
                        panel.child(actionButton(entry, y, mte, player));
                        y += GAP;
                }

                panel.child(SuGuiFactories.text(mte == null ? "susyplusplus.gui.configurator.no_machine"
                                : "susyplusplus.gui.configurator.toolbox.hint").left(BTN_X).top(y + 4));

                return panel;
        }

        /** 一个按钮：标签（名称 + 当前状态）、服务端动作。 */
        private static ButtonWidget<?> actionButton(final ToolboxEntry entry, int y, final MetaTileEntity mte,
                        final EntityPlayer player) {
                ButtonWidget button = new ButtonWidget();
                button.left(BTN_X).top(y).width(BTN_W).height(BTN_H);
                button.background(new Rectangle().setColor(SuGuiFactories.COLOR_BUTTON));
                button.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_SELECTED));
                button.child(SuGuiFactories.buttonLabel(entry.labelKey, entry.stateSupplier).left(8).top(7));

                InteractionSyncHandler sync = new InteractionSyncHandler();
                sync.setOnMousePressed(clicked -> {
                        entry.action.run(mte);
                        player.sendStatusMessage(
                                        new TextComponentTranslation("susyplusplus.message.configurator.toolbox_done",
                                                        new TextComponentTranslation(entry.labelKey)),
                                        true);
                });
                button.syncHandler(sync);
                return button;
        }

        /** 面板会用到的小工具。 */
        private static final String DASH = "—";

        private static String state(boolean value) {
                return I18n.format(value ? "susyplusplus.gui.configurator.state.on"
                                : "susyplusplus.gui.configurator.state.off");
        }

        @SuppressWarnings("unchecked")
        private static <T> T capability(MetaTileEntity mte,
                        net.minecraftforge.common.capabilities.Capability<T> capability) {
                if (mte == null || capability == null) {
                        return null;
                }
                return mte.getCapability(capability, null);
        }

        /** 按钮定义。 */
        private static final class ToolboxEntry {

                final String labelKey;
                final Supplier<String> stateSupplier;
                final MachineAction action;

                ToolboxEntry(String labelKey, Supplier<String> stateSupplier, MachineAction action) {
                        this.labelKey = labelKey;
                        this.stateSupplier = stateSupplier;
                        this.action = action;
                }
        }

        /** 服务端动作（动作里重新取能力，避免持有过期引用）。 */
        private interface MachineAction {

                void run(MetaTileEntity machine);
        }
}
