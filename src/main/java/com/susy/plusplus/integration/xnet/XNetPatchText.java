package com.susy.plusplus.integration.xnet;

import com.susy.plusplus.config.SuConfig;

import net.minecraft.client.resources.I18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code mixin/xnet} 与 {@code mixin/mcjtylib} 下各个客户端补丁共用的文本本地化助手。
 *
 * <p>
 * XNet 的界面文本大量硬编码在字节码里（既不是 {@code I18n.format}、也没有 key），
 * 只能用两种办法改：{@code @ModifyConstant} 替换字符串常量，或在
 * {@code AbstractLabel#setText} 上做"显示层"替换。本类给两者提供取值。
 * 关掉 {@link SuConfig#enableThirdPartyPatches} 时一律原样返回，完全退回原状。
 * </p>
 *
 * <h3>⚠ 为什么这个类<b>不能</b>放在 {@code com.susy.plusplus.mixin} 包里</h3>
 *
 * <p>
 * Mixin 会把 {@code mixins.susyplusplus.json} 的 {@code "package"}
 * （即 {@code com.susy.plusplus.mixin}）整个加入<b>类加载器排除名单</b> ——
 * 该包下的类只能由 Mixin 子系统自己加载，普通 {@code LaunchClassLoader} 找不到它们。
 * </p>
 *
 * <p>
 * 实测报错（把助手放进去时，打开 XNet 控制器界面直接崩）：
 * </p>
 *
 * <pre>
 * java.lang.NoClassDefFoundError: com/susy/plusplus/mixin/xnet/XNetPatchText
 *   at mcjty.xnet.blocks.controller.gui.GuiController.constant$zcf000$susyplusplus$create(...)
 * Caused by: java.lang.ClassNotFoundException: com/susy/plusplus/mixin/xnet/XNetPatchText
 * </pre>
 *
 * <p>
 * 原因：{@code @ModifyConstant} 的替换代码是被<b>合并进目标类</b>（例如 XNet 的
 * {@code GuiController}）执行的，运行时由普通类加载器解析引用 —— 而它解析不到 mixin 包里的类。
 * </p>
 *
 * <p>
 * ⚠ 另外，本类只能在<b>客户端</b>被引用（它 import 了
 * {@code net.minecraft.client.resources.I18n}），所以不要从 {@code mixin/xnet} 里那些
 * "通用（服务端也会加载）"的补丁调用它 —— 那些补丁用的是
 * {@code net.minecraft.util.text.translation.I18n}。
 * </p>
 */
public final class XNetPatchText {

    /** XNet 频道类型 ID 的前缀（{@code xnet.item} / {@code xnet.energy} …）。 */
    private static final String XNET_PREFIX = "xnet.";

    /**
     * 「控件显示文本 → 语言键」白名单（严格精确匹配）。
     *
     * <p>
     * 为什么要它：{@code AbstractEditorPanel#choices(Enum...)} 是这么算显示名的 ——
     * </p>
     *
     * <pre>
     * strings[i++] = StringUtils.capitalize(s.toString().toLowerCase());
     * </pre>
     *
     * <p>
     * 即 {@code INS → "Ins"}、{@code PRIORITY → "Priority"}、{@code RND → "Rnd"} …
     * <b>这些字符串在字节码里根本不存在</b>（是运行时拼出来的），
     * 所以 {@code @ModifyConstant} 抓不到，只能在该控件 {@code setText} 时按白名单翻译。
     * 而"频道类型下拉框"显示的是 {@code xnet.*} 键，走上面的前缀规则。
     * </p>
     *
     * <p>
     * ⚠ 只翻译<b>显示</b>：{@code ChoiceLabel} 的取值存在自己的 {@code currentChoice} 字段里，
     * 选项名同时也是发给服务端的序列化值（服务端会 {@code ItemMode.valueOf} /
     * {@code RSMode.valueOf}），所以绝不能替换成中文 —— 白名单只作用在显示文本上。
     * </p>
     */
    private static final Map<String, String> LITERALS;

    static {
        Map<String, String> m = new HashMap<>();

        // 连接器模式：ItemMode / FluidMode / EnergyMode = { INS, EXT }
        m.put("Ins", "susyplusplus.patch.xnet.mode.ins");
        m.put("Ext", "susyplusplus.patch.xnet.mode.ext");

        // 物品栈模式：StackMode = { SINGLE, STACK, COUNT }
        m.put("Single", "susyplusplus.patch.xnet.item.stack_single");
        m.put("Stack", "susyplusplus.patch.xnet.item.stack_stack");
        m.put("Count", "susyplusplus.patch.xnet.item.stack_count");

        // 提取模式：ExtractMode = { FIRST, RND, ORDER }
        m.put("First", "susyplusplus.patch.xnet.item.extract_first");
        m.put("Rnd", "susyplusplus.patch.xnet.item.extract_rnd");
        m.put("Order", "susyplusplus.patch.xnet.item.extract_order");

        // 频道模式：ItemChannelSettings = { PRIORITY, ROUNDROBIN } / FluidChannelSettings = { PRIORITY, DISTRIBUTE }
        m.put("Priority", "susyplusplus.patch.xnet.chanmode.priority");
        m.put("Roundrobin", "susyplusplus.patch.xnet.chanmode.roundrobin");
        m.put("Distribute", "susyplusplus.patch.xnet.chanmode.distribute");

        // 逻辑模式：LogicMode = { SENSOR, OUTPUT }
        m.put("Sensor", "susyplusplus.patch.xnet.logic.sensor");
        m.put("Output", "susyplusplus.patch.xnet.logic.output");

        // 传感器类型：SensorMode = { OFF, ITEM, FLUID, ENERGY, RS }
        // 注意 "Off" 与红石模式的 "Off" 是同一个词，共用一条译名。
        m.put("Item", "susyplusplus.patch.xnet.sensor.item");
        m.put("Fluid", "susyplusplus.patch.xnet.sensor.fluid");
        m.put("Energy", "susyplusplus.patch.xnet.sensor.energy");
        m.put("Rs", "susyplusplus.patch.xnet.sensor.rs");

        // 红石模式选项名：这些字面量在 AbstractEditorPanel 里同时是"取值"，
        // 所以不能用 @ModifyConstant 替换（服务端 RSMode.valueOf 会炸），只能改显示。
        m.put("Ignored", "susyplusplus.patch.xnet.rs.ignored_label");
        m.put("Off", "susyplusplus.patch.xnet.rs.off_label");
        m.put("On", "susyplusplus.patch.xnet.rs.on_label");
        m.put("Pulse", "susyplusplus.patch.xnet.rs.pulse_label");

        // 短标签：这些原本已经用 @ModifyConstant 换掉了字符串常量，
        // 这里再挂一遍作为第二道保险（万一以后 XNet 改成运行时拼字符串，显示层也能兜住）。
        m.put("Pri", "susyplusplus.patch.xnet.label.pri");
        m.put("Rate", "susyplusplus.patch.xnet.label.rate");
        m.put("Min", "susyplusplus.patch.xnet.label.min");
        m.put("Max", "susyplusplus.patch.xnet.label.max");
        m.put("Filter", "susyplusplus.patch.xnet.label.filter");
        m.put("BL", "susyplusplus.patch.xnet.item.bl_label");
        m.put("Ore", "susyplusplus.patch.xnet.item.ore_label");
        m.put("Meta", "susyplusplus.patch.xnet.item.meta_label");
        m.put("NBT", "susyplusplus.patch.xnet.item.nbt_label");

        LITERALS = Collections.unmodifiableMap(m);
    }

    private XNetPatchText() {
    }

    /** 供 {@code @ModifyConstant} 处理函数使用：关开关时返回原文。 */
    public static String localize(String original, String key) {
        if (!SuConfig.enableThirdPartyPatches) {
            return original;
        }
        return I18n.format(key);
    }

    /**
     * 供 {@code AbstractLabel#setText} 钩子使用：翻译"控件显示文本"。
     *
     * <p>
     * 两条规则：① 以 {@code xnet.} 开头 → 直接当语言键翻译（频道类型下拉框）；
     * ② 命中 {@link #LITERALS} 白名单 → 翻成对应键。其余一律原样返回，
     * 因此不会误伤其它文本（例如数字、机器名、玩家输入）。
     * </p>
     */
    public static String localizeLabel(String text) {
        if (!SuConfig.enableThirdPartyPatches || text == null || text.isEmpty()) {
            return text;
        }
        if (text.startsWith(XNET_PREFIX)) {
            return I18n.format(text);
        }
        String key = LITERALS.get(text);
        return key == null ? text : I18n.format(key);
    }
}
