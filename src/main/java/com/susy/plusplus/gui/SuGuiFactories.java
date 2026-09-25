package com.susy.plusplus.gui;

import com.susy.plusplus.Tags;
import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.item.SuMetaItems;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widget.Widget;

import java.util.function.Supplier;

/**
 * 配置器相关 GUI 的注册入口、公共常量与文字样式工具。
 *
 * <p>
 * <b>必须在公共 init 阶段调用 {@link #init()}</b>：ModularUI 的工厂靠"名字"在两端匹配
 * —— 服务端打开界面时只把工厂名发给客户端，客户端要在自己的 {@code GuiManager} 里
 * 找得到同名工厂。因此这两个工厂必须在客户端与服务端<b>都</b>构造出来。
 * </p>
 *
 * <p>
 * ⚠ ModularUI 对工厂名有硬性限制：<b>不得超过 32 个字符</b>
 * （{@code GuiManager#registerFactory} 会抛 {@code IllegalArgumentException}）。
 * 下面三个名字都在 32 以内。
 * </p>
 */
public final class SuGuiFactories {

    /** 半透明灰背景（ARGB：0x80 ≈ 50% 不透明）。 */
    public static final int COLOR_BACKGROUND = 0x80101010;

    /** 按钮底色。 */
    public static final int COLOR_BUTTON = 0x80404040;

    /** 按钮悬停底色。 */
    public static final int COLOR_HOVER = 0x80606060;

    /** 当前/选中项底色。 */
    public static final int COLOR_SELECTED = 0x8060A0C0;

    /** 已开启/确认类按钮底色。 */
    public static final int COLOR_ENABLED = 0x8030A030;

    /** 不可用底色。 */
    public static final int COLOR_DISABLED = 0x80303030;

    /** 机器面配置界面工厂。 */
    private static final SuPosGuiFactory FACE_UI = new SuPosGuiFactory(MachineFaceUI.FACTORY_NAME,
            data -> new MachineFaceUI(data));

    /** 机器工具箱界面工厂。 */
    private static final SuPosGuiFactory TOOLBOX_UI = new SuPosGuiFactory(MachineToolboxUI.FACTORY_NAME,
            data -> new MachineToolboxUI());

    private SuGuiFactories() {
    }

    /** 在 {@code preInit}（客户端与服务端都执行）调用。 */
    public static void init() {
        ConfiguratorMainUI.init();
        SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Configurator GUI factories: faceUI={}, toolboxUI={}",
                FACE_UI.getFactoryName(), TOOLBOX_UI.getFactoryName());
    }

    /** 服务端：打开「修改机器输出面」界面。 */
    public static void openFaceUI(EntityPlayerMP player, BlockPos pos) {
        FACE_UI.open(player, pos);
    }

    /** 服务端：打开「机器工具箱」界面。 */
    public static void openToolbox(EntityPlayerMP player, BlockPos pos) {
        TOOLBOX_UI.open(player, pos);
    }

    /**
     * 取玩家手上（主手 → 副手）的配置器；没有则返回 {@link ItemStack#EMPTY}。
     *
     * <p>
     * 服务端与客户端都能调用（{@code IGuiHolder#buildUI} 两端都会用到）。
     * </p>
     */
    public static ItemStack findConfigurator(EntityPlayer player) {
        if (player == null || SuMetaItems.CONFIGURATOR == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getHeldItemMainhand();
        if (!main.isEmpty() && SuMetaItems.CONFIGURATOR.isItemEqual(main)) {
            return main;
        }
        ItemStack off = player.getHeldItemOffhand();
        if (!off.isEmpty() && SuMetaItems.CONFIGURATOR.isItemEqual(off)) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    // ==========================================================================
    // 文字样式
    // ==========================================================================

    /**
     * 按钮文字：<b>白色 + 粗体</b>（§ 码，MUI2 最终交给字体渲染器处理）。
     *
     * <p>
     * 界面底色是深色半透明，默认深灰字看不清 —— 统一走这里；状态值接在名称后面。
     * </p>
     */
    public static Widget<?> buttonLabel(final String labelKey, final Supplier<String> stateSupplier) {
        return IKey.dynamic(() -> "§f§l" + I18n.format(labelKey)
                + (stateSupplier == null ? "" : "  " + stateSupplier.get())).asWidget();
    }

    /** 界面标题：白色粗体。 */
    public static Widget<?> title(final String labelKey) {
        return IKey.dynamic(() -> "§f§l" + I18n.format(labelKey)).asWidget();
    }

    /** 普通说明文字：浅灰（比底色亮，能看清但不抢眼）。 */
    public static Widget<?> text(final String labelKey) {
        return IKey.dynamic(() -> "§7" + I18n.format(labelKey)).asWidget();
    }

    /** 动态状态文字（白色粗体）。 */
    public static Widget<?> stateText(final Supplier<String> supplier) {
        return IKey.dynamic(() -> "§f§l" + supplier.get()).asWidget();
    }
}
