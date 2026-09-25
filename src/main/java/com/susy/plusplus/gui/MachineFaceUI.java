package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;
import com.susy.plusplus.item.configurator.MachineFaceState;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;

/**
 * 「修改机器输出面」界面（配置器模式 1，Shift + 右键机器后打开）。
 *
 * <h2>按钮布局（按需求）</h2>
 *
 * <pre>
 * 第一行：X  A  X  X
 * 第二行：B  C  D  E
 * 第三行：X  G  X  X
 * </pre>
 *
 * <ul>
 * <li>C = 机器主面（{@code MetaTileEntity#getFrontFacing()}），<b>可写</b>；</li>
 * <li>A = 上面（{@code EnumFacing.UP}）、G = 下面（{@code DOWN}）；</li>
 * <li>E = 后面（主面的对面）；B / D = 主面水平旋转得到的左右侧
 * （用 {@code rotateYCCW()} / {@code rotateY()}，仅是按钮标注的左右约定）；</li>
 * <li>X 不存在（空气，仅用于说明位置）。</li>
 * </ul>
 *
 * <h2>状态模型</h2>
 *
 * <p>
 * 每个面 4 态循环：无 → 流体 → 物品 → 两者 → 无。GT 本体只有「一个物品输出面 +
 * 一个流体输出面」，因此把某面设为含"物品"的态时其它面的物品位会被自动清掉（流体同理）。
 * </p>
 *
 * <p>
 * 改动先在界面内累积（{@code pendingMask}，12 位 = 6 面 × 2 位），按「确认」才写进机器并退出；
 * 「返回」按钮回主界面。
 * </p>
 *
 * <h2>为什么不用 {@code syncValue} 同步掩码</h2>
 *
 * <p>
 * 原本用 {@code PanelSyncManager#syncValue(String, SyncHandler)} 把掩码推给客户端，但该
 * <b>2 参重载只在编译期的 ModularUI 3.0.4 中存在</b>，运行时的 3.1.6 只保留了 3 参重载
 * —— 结果就是打开界面时直接抛
 * {@code NoSuchMethodError: PanelSyncManager.syncValue(String, SyncHandler)}。
 * </p>
 *
 * <p>
 * 现在改为<b>完全不用同步值</b>：{@code InteractionSyncHandler} 的语义是
 * 「<b>本地先执行一次 + 再把点击同步到服务端执行一次</b>」，所以按钮回调在客户端与服务端
 * 各跑一遍，两端的 {@code pendingMask} 会因为同一套确定性逻辑而保持一致
 * —— 客户端立刻刷新标签，服务端拿到权威掩码用于「确认」。副作用（改机器、开上级界面）
 * 只在服务端做，客户端只负责关闭当前界面。
 * </p>
 */
public final class MachineFaceUI implements IGuiHolder<PosGuiData> {

    /**
     * GUI 工厂名。
     *
     * <p>
     * ⚠ ModularUI 要求工厂名 ≤ 32 字符（本名 31，安全）；再长会被
     * {@code GuiManager#registerFactory} 拒绝并导致 preInit 崩溃。
     * </p>
     */
    public static final String FACTORY_NAME = Tags.MOD_ID + ":configurator_faces";

    private static final String PANEL_NAME = "configurator_faces";
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 152;

    private static final int BTN_W = 40;
    private static final int BTN_H = 22;
    private static final int COL_GAP = 46;
    private static final int ROW_GAP = 28;
    private static final int GRID_X = 12;
    private static final int GRID_Y = 28;

    /** 6 个面在按钮布局中的坐标：{列, 行}（第一行 = 0）。 */
    private static int[] gridOf(EnumFacing face) {
        switch (face) {
            case UP:
                return new int[] { 1, 0 };
            case DOWN:
                return new int[] { 1, 2 };
            case NORTH:
                return new int[] { 0, 1 };
            case SOUTH:
                return new int[] { 2, 1 };
            case WEST:
                return new int[] { 3, 1 };
            case EAST:
            default:
                return new int[] { 1, 1 };
        }
    }

    /** 12 位掩码：面序号 × 2 + (0=物品, 1=流体)。 */
    private int pendingMask;

    public MachineFaceUI(PosGuiData data) {
        MetaTileEntity mte = machine(data);
        if (mte instanceof SimpleMachineMetaTileEntity) {
            SimpleMachineMetaTileEntity machine = (SimpleMachineMetaTileEntity) mte;
            for (EnumFacing face : EnumFacing.VALUES) {
                if (face == machine.getOutputFacingItems()) {
                    setBit(face, 0, machine.isAutoOutputItems());
                }
                if (face == machine.getOutputFacingFluids()) {
                    setBit(face, 1, machine.isAutoOutputFluids());
                }
            }
        }
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        final MetaTileEntity mte = machine(data);
        final ModularPanel panel = ModularPanel.defaultPanel(PANEL_NAME, PANEL_WIDTH, PANEL_HEIGHT);
        panel.background(new Rectangle().setColor(SuGuiFactories.COLOR_BACKGROUND));
        // 光标停在非按钮区域时，不要让主题的"悬停背景"盖住半透明底色
        panel.disableHoverBackground();

        panel.child(SuGuiFactories.title("susyplusplus.gui.configurator.faces.title")
                .left(GRID_X).top(6));

        final EnumFacing front = mte != null && mte.hasFrontFacing() ? mte.getFrontFacing() : EnumFacing.NORTH;

        for (final EnumFacing face : EnumFacing.VALUES) {
            int[] grid = gridOf(relativeFace(face, front));
            ButtonWidget button = new ButtonWidget();
            button.left(GRID_X + grid[0] * COL_GAP).top(GRID_Y + grid[1] * ROW_GAP).width(BTN_W).height(BTN_H);
            button.background(new Rectangle().setColor(SuGuiFactories.COLOR_BUTTON));
            button.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
            // 标签动态读取本地 pendingMask（客户端每次绘制都会重新求值）；白色粗体
            button.child(SuGuiFactories.stateText(() -> I18n.format(faceState(face).getLangKey()))
                    .left(4).top(8));

            final int faceIndex = face.getIndex();
            InteractionSyncHandler cycle = new InteractionSyncHandler();
            // 客户端 + 服务端各执行一次（同一确定性逻辑）→ 两端掩码一致，无需额外同步
            cycle.setOnMousePressed(action -> this.pendingMask = cycleFace(this.pendingMask, faceIndex));
            button.syncHandler(cycle);
            panel.child(button);
        }

        // 主面提示（浅灰）
        panel.child(IKey.dynamic(() -> "§7" + I18n.format("susyplusplus.gui.configurator.faces.hint",
                I18n.format(faceNameKey(front)))).asWidget()
                .left(GRID_X).top(GRID_Y + 3 * ROW_GAP - 2));

        // 确认
        ButtonWidget confirm = new ButtonWidget();
        confirm.left(PANEL_WIDTH - BTN_W - GRID_X).top(PANEL_HEIGHT - BTN_H - 8).width(BTN_W + 8).height(BTN_H);
        confirm.background(new Rectangle().setColor(SuGuiFactories.COLOR_ENABLED));
        confirm.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        confirm.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.confirm", null).left(10).top(8));

        InteractionSyncHandler apply = new InteractionSyncHandler();
        apply.setOnMousePressed(action -> {
            // 回调两端都会跑：只有服务端能真正改机器（客户端改的是本地假 TE）
            if (data.getPlayer() instanceof EntityPlayerMP) {
                applyToMachine(mte, this.pendingMask);
            }
            panel.closeIfOpen();
        });
        confirm.syncHandler(apply);
        panel.child(confirm);

        // 「返回」按钮：回上一级（主界面）。
        // 说明：MUI2 3.0.4 里 esc 由屏幕自己处理（关闭当前界面），
        // 想让它"返回主界面"需要客户端的按键回调，因此这里提供显式的返回入口。
        ButtonWidget back = new ButtonWidget();
        back.left(GRID_X).top(PANEL_HEIGHT - BTN_H - 8).width(BTN_W + 8).height(BTN_H);
        back.background(new Rectangle().setColor(SuGuiFactories.COLOR_BUTTON));
        back.hoverBackground(new Rectangle().setColor(SuGuiFactories.COLOR_HOVER));
        back.child(SuGuiFactories.buttonLabel("susyplusplus.gui.configurator.back", null).left(10).top(8));
        InteractionSyncHandler backSync = new InteractionSyncHandler();
        backSync.setOnMousePressed(action -> {
            // 回调两端都会跑：客户端侧 player 是 EntityPlayerSP，直接强转会 ClassCastException，
            // 所以用 instanceof 判断——只有服务端负责"打开主界面"，两端都关掉当前界面。
            if (data.getPlayer() instanceof EntityPlayerMP) {
                ConfiguratorMainUI.open((EntityPlayerMP) data.getPlayer());
            }
            panel.closeIfOpen();
        });
        back.syncHandler(backSync);
        panel.child(back);

        return panel;
    }

    // ------------------------------------------------------------------ 掩码

    private static int bitIndex(EnumFacing face, int slot) {
        return face.getIndex() * 2 + slot;
    }

    private void setBit(EnumFacing face, int slot, boolean value) {
        int index = bitIndex(face, slot);
        if (value) {
            this.pendingMask |= (1 << index);
        } else {
            this.pendingMask &= ~(1 << index);
        }
    }

    private boolean getBit(EnumFacing face, int slot) {
        return (this.pendingMask & (1 << bitIndex(face, slot))) != 0;
    }

    private MachineFaceState faceState(EnumFacing face) {
        return MachineFaceState.of(true, getBit(face, 0), true, getBit(face, 1));
    }

    /** 循环某面到下一个状态，并保证"最多一个物品输出面 + 一个流体输出面"。 */
    private static int cycleFace(int mask, int faceIndex) {
        EnumFacing face = EnumFacing.VALUES[faceIndex];
        boolean items = (mask & (1 << (faceIndex * 2))) != 0;
        boolean fluids = (mask & (1 << (faceIndex * 2 + 1))) != 0;
        MachineFaceState next = MachineFaceState.of(true, items, true, fluids).next();

        int result = mask;
        if (next.includesItems()) {
            result = setBitRaw(result, faceIndex, 0, true);
            result = clearOtherFaces(result, faceIndex, 0);
        } else {
            result = setBitRaw(result, faceIndex, 0, false);
        }
        if (next.includesFluids()) {
            result = setBitRaw(result, faceIndex, 1, true);
            result = clearOtherFaces(result, faceIndex, 1);
        } else {
            result = setBitRaw(result, faceIndex, 1, false);
        }
        return result;
    }

    private static int setBitRaw(int mask, int faceIndex, int slot, boolean value) {
        int index = faceIndex * 2 + slot;
        return value ? (mask | (1 << index)) : (mask & ~(1 << index));
    }

    private static int clearOtherFaces(int mask, int keepFaceIndex, int slot) {
        int result = mask;
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            if (i != keepFaceIndex) {
                result = setBitRaw(result, i, slot, false);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ 写入机器

    /** 把掩码写进机器（全部走 {@code SimpleMachineMetaTileEntity} 的公开 setter）。 */
    private static void applyToMachine(MetaTileEntity mte, int mask) {
        if (!(mte instanceof SimpleMachineMetaTileEntity)) {
            return;
        }
        SimpleMachineMetaTileEntity machine = (SimpleMachineMetaTileEntity) mte;

        EnumFacing itemsFace = null;
        EnumFacing fluidsFace = null;
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            if ((mask & (1 << (i * 2))) != 0) {
                itemsFace = EnumFacing.VALUES[i];
            }
            if ((mask & (1 << (i * 2 + 1))) != 0) {
                fluidsFace = EnumFacing.VALUES[i];
            }
        }

        boolean autoItems = itemsFace != null;
        boolean autoFluids = fluidsFace != null;
        if (autoItems) {
            machine.setOutputFacingItems(itemsFace);
        }
        if (autoFluids) {
            machine.setOutputFacingFluids(fluidsFace);
        }
        machine.setAutoOutputItems(autoItems);
        machine.setAutoOutputFluids(autoFluids);
    }

    // ------------------------------------------------------------------ 工具

    /** 把「机器实际朝向」映射到 UI 的相对位置（只影响按钮位置，不影响语义）。 */
    private static EnumFacing relativeFace(EnumFacing gridFace, EnumFacing front) {
        switch (gridFace) {
            case EAST:
                return front;
            case WEST:
                return front.getOpposite();
            case NORTH:
                return front.rotateYCCW();
            case SOUTH:
                return front.rotateY();
            case UP:
                return EnumFacing.UP;
            case DOWN:
            default:
                return EnumFacing.DOWN;
        }
    }

    private static String faceNameKey(EnumFacing face) {
        switch (face) {
            case UP:
                return "susyplusplus.gui.configurator.face.up";
            case DOWN:
                return "susyplusplus.gui.configurator.face.down";
            case NORTH:
                return "susyplusplus.gui.configurator.face.north";
            case SOUTH:
                return "susyplusplus.gui.configurator.face.south";
            case WEST:
                return "susyplusplus.gui.configurator.face.west";
            case EAST:
            default:
                return "susyplusplus.gui.configurator.face.east";
        }
    }

    /** 从 GUI 数据取机器（客户端/服务端都有该位置的 TileEntity）。 */
    static MetaTileEntity machine(PosGuiData data) {
        TileEntity tileEntity = data.getTileEntity();
        if (tileEntity instanceof IGregTechTileEntity) {
            return ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
        }
        return null;
    }
}
