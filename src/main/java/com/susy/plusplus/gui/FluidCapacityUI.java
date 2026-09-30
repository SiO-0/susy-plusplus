package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;
import com.susy.plusplus.machine.FluidCapacityHelper;
import com.susy.plusplus.machine.MachineExtraState;
import com.susy.plusplus.network.PacketSetFluidCapacity;
import com.susy.plusplus.network.SuNetwork;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * 「容量限制（仅流体）」界面（配置器模式 4，普通右键容器/仓室后打开）。
 *
 * <h2>布局</h2>
 *
 * <p>
 * 每页固定 {@value #PAGE_SIZE} 行，每行：<b>左上角槽位号 + 可编辑容量输入框 + 只读原容量</b>。
 * 槽位多于 {@value #PAGE_SIZE} 时，底部出现<b>上一页 / 下一页</b>与页码；翻页会先
 * 把当前页输入框里的内容存进本界面的工作副本，再刷新显示，因此<b>跨页编辑不会丢</b>。
 * </p>
 *
 * <h2>支持范围</h2>
 *
 * <p>
 * 槽位枚举全部交给 {@link FluidCapacityHelper#tanks(MetaTileEntity)}：它会把
 * GT 的 {@code FluidTankList}（{@code IMultipleTankHandler}）、
 * {@code FluidHandlerProxy}（输入/输出分开包装的**仓室**，例如流体输入仓/输出仓）
 * 以及单槽库存统一拆成真正的 {@code IFluidTank} 列表。因此
 * 输入仓、输出仓、普通的单方块流体机器、量子缸、以及本模组的流体样品存储都能用。
 * </p>
 *
 * <h2>数据怎么送到服务端</h2>
 *
 * <p>
 * {@code InteractionSyncHandler} 的回调在两端各跑一次，但只有客户端拥有用户编辑过的输入框内容，
 * 所以确认时由客户端发 {@link PacketSetFluidCapacity}（携带**所有页**的最终值），
 * 服务端收到后再写机器。这样绕开了 ModularUI 3.0.4 / 3.1.6 在 {@code syncValue} 重载上的差异。
 * </p>
 */
public final class FluidCapacityUI implements IGuiHolder<PosGuiData> {

    /** 工厂名（≤32 字符，两端一致）。 */
    public static final String FACTORY_NAME = Tags.MOD_ID + ":cfg_fluid_cap";

    private static final String PANEL_NAME = "configurator_fluid_capacity";
    private static final int WIDTH = 240;
    private static final int ROW_H = 22;
    private static final int HEADER = 28;
    private static final int FOOTER = 34;
    /** 每页行数。 */
    private static final int PAGE_SIZE = 8;

    /** 本界面的"工作副本"：所有槽位的当前编辑值（客户端与服务端各持一份，以客户端为准）。 */
    private int[] edited;
    /** 当前页（0 起）。 */
    private int page;
    /** 总页数（至少 1）。 */
    private int pageCount = 1;
    /** 本页的输入框（索引 0..PAGE_SIZE-1）。 */
    private final List<TextFieldWidget> fields = new ArrayList<>();

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        final MetaTileEntity mte = MachineFaceUI.machine(data);
        final List<IFluidTank> tanks = FluidCapacityHelper.tanks(mte);
        final int tankCount = tanks.size();

        // 原容量（只读）：优先用我们记录过的；没记录过就先按当前容量显示，真正记录由服务端在确认时完成
        int[] originals = MachineExtraState.getOriginalCapacity(mte);
        if (originals == null) {
            originals = FluidCapacityHelper.capacitiesOf(tanks);
        }
        int[] currents = MachineExtraState.getCurrentCapacity(mte);
        if (currents == null) {
            currents = FluidCapacityHelper.capacitiesOf(tanks);
        }
        final int[] originalCaps = originals;
        final int[] currentCaps = currents;

        // 工作副本：以"当前容量"为初值，长度按槽位数
        this.edited = new int[tankCount];
        for (int i = 0; i < tankCount; i++) {
            this.edited[i] = i < currentCaps.length ? currentCaps[i] : 0;
        }
        this.page = 0;
        this.pageCount = Math.max(1, (tankCount + PAGE_SIZE - 1) / PAGE_SIZE);
        this.fields.clear();

        // 行数：**单页机器按实际槽位数建行**（只有 1 个槽就只显示 1 行，不再出现多余空框）；
        // 多页机器每页固定 8 行，最后一页不足的行会被禁用（不可编辑）。
        final int rowsBuilt = Math.min(PAGE_SIZE, Math.max(0, tankCount));
        int pagerH = tankCount > PAGE_SIZE ? 22 : 0;
        int height = HEADER + rowsBuilt * ROW_H + pagerH + FOOTER;
        ModularPanel panel = ModularPanel.defaultPanel(PANEL_NAME, WIDTH, height);
        panel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BACKGROUND));
        panel.disableHoverBackground();
        panel.child(SuGuiFactories.title("susyplusplus.gui.configurator.capacity.title").left(8).top(6));

        if (tankCount == 0) {
            panel.child(SuGuiFactories.text("susyplusplus.gui.configurator.no_fluid_tank")
                    .left(8).top(HEADER + 6));
        }

        // 固定 8 行：标签是动态的（每次绘制重新求值），因此翻页只需刷新输入框文本
        int y = HEADER;
        for (int row = 0; row < rowsBuilt; row++) {
            final int rowIndex = row;
            panel.child(IKey.dynamic(() -> {
                int global = page * PAGE_SIZE + rowIndex;
                if (global >= currentTankCount()) {
                    return "";
                }
                return "§f" + I18n.format("susyplusplus.gui.configurator.capacity.slot") + " " + (global + 1);
            }).asWidget().left(8).top(y + 6));

            TextFieldWidget field = new TextFieldWidget();
            field.left(64).top(y + 1).width(80).height(16);
            field.setMaxLength(9);
            field.setNumbers(0, Integer.MAX_VALUE);
            field.setText(rowText(row));
            fields.add(field);
            panel.child(field);

            panel.child(IKey.dynamic(() -> {
                int global = page * PAGE_SIZE + rowIndex;
                if (global >= currentTankCount()) {
                    return "";
                }
                return "§7" + I18n.format("susyplusplus.gui.configurator.capacity.original",
                        global < originalCaps.length ? originalCaps[global] : 0);
            }).asWidget().left(152).top(y + 6));
            y += ROW_H;
        }

        // 翻页（只在槽位多于一页时出现）
        if (tankCount > PAGE_SIZE) {
            ButtonWidget prev = pagerButton(8, y);
            prev.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.capacity.prev", null)
                    .left(6).top(7));
            InteractionSyncHandler prevSync = new InteractionSyncHandler();
            prevSync.setOnMousePressed(action -> turnPage(-1));
            prev.syncHandler(prevSync);
            panel.child(prev);

            ButtonWidget next = pagerButton(WIDTH - 72, y);
            next.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.capacity.next", null)
                    .left(6).top(7));
            InteractionSyncHandler nextSync = new InteractionSyncHandler();
            nextSync.setOnMousePressed(action -> turnPage(1));
            next.syncHandler(nextSync);
            panel.child(next);

            panel.child(IKey.dynamic(() -> "§f" + I18n.format(
                    "susyplusplus.gui.configurator.capacity.page", page + 1, pageCount))
                    .asWidget().left(96).top(y + 7));
            y += pagerH;
        }

        // 确认：客户端把所有页的最终值发给服务端
        ButtonWidget confirm = new ButtonWidget();
        confirm.left(WIDTH - 72).top(y + 6).width(64).height(20);
        confirm.background(new Rectangle().setColor(SuGuiFactories.COLOR_ENABLED));
        confirm.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        confirm.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.confirm", null)
                .left(10).top(7));
        InteractionSyncHandler confirmSync = new InteractionSyncHandler();
        confirmSync.setOnMousePressed(action -> {
            if (mte != null && mte.getWorld() != null && mte.getWorld().isRemote) {
                storeCurrentPage();
                SuNetwork.sendToServer(new PacketSetFluidCapacity(mte.getPos(), edited.clone()));
            }
            panel.closeIfOpen();
        });
        confirm.syncHandler(confirmSync);
        panel.child(confirm);

        // 取消：不写入，只退出
        ButtonWidget cancel = new ButtonWidget();
        cancel.left(8).top(y + 6).width(64).height(20);
        cancel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BUTTON));
        cancel.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        cancel.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.cancel", null)
                .left(10).top(7));
        InteractionSyncHandler cancelSync = new InteractionSyncHandler();
        cancelSync.setOnMousePressed(action -> panel.closeIfOpen());
        cancel.syncHandler(cancelSync);
        panel.child(cancel);

        return panel;
    }

    // ------------------------------------------------------------------ 分页

    private ButtonWidget<?> pagerButton(int x, int y) {
        ButtonWidget button = new ButtonWidget();
        button.left(x).top(y).width(64).height(20);
        button.background(new Rectangle().setColor(SuGuiFactories.COLOR_BUTTON));
        button.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        return button;
    }

    /** 翻页：先存当前页，再切换并刷新输入框文本（两端都会执行，逻辑确定所以结果一致）。 */
    private void turnPage(int delta) {
        if (pageCount <= 1) {
            return;
        }
        storeCurrentPage();
        page = (page + delta + pageCount) % pageCount;
        for (int row = 0; row < fields.size(); row++) {
            fields.get(row).setText(rowText(row));
            // 最后一页可能不满：越界的行禁用，避免出现"可编辑但没意义"的空框
            fields.get(row).setEnabled(page * PAGE_SIZE + row < currentTankCount());
        }
    }

    /** 把当前页输入框里的内容写回工作副本（越界与非法值一律忽略）。 */
    private void storeCurrentPage() {
        if (edited == null) {
            return;
        }
        for (int row = 0; row < fields.size(); row++) {
            int global = page * PAGE_SIZE + row;
            if (global >= edited.length) {
                break;
            }
            edited[global] = parse(fields.get(row), edited[global]);
        }
    }

    /** 第 {@code row} 行应显示的文本（越界时留空）。 */
    private String rowText(int row) {
        int global = page * PAGE_SIZE + row;
        if (edited == null || global >= edited.length) {
            return "";
        }
        return String.valueOf(edited[global]);
    }

    /** 供动态标签使用：当前机器的槽位数（由 edited 长度体现）。 */
    private int currentTankCount() {
        return edited == null ? 0 : edited.length;
    }

    /** 把输入框内容解析成非负整数；非法时回退到 fallback。 */
    private static int parse(TextFieldWidget field, int fallback) {
        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return fallback;
            }
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
