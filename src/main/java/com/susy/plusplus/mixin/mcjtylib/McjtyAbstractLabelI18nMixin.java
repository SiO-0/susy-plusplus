package com.susy.plusplus.mixin.mcjtylib;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 让 XNet 界面里那些"硬编码且不走 I18n"的控件文本显示成中文 —— 这是补丁链的最后一环。
 *
 * <h3>为什么需要它</h3>
 *
 * <p>
 * mcjtylib 的控件<b>从不调用 I18n</b>（全量扫描 {@code mcjtylib-refilmed-3.5.5.jar}：
 * 整个库只有 {@code BaseBlock} 引用过 I18n）。{@code AbstractLabel.draw} 直接把私有字段
 * {@code text} 丢给 {@code FontRenderer}，所以：
 * </p>
 *
 * <ul>
 * <li>控制器 GUI 的"频道类型"下拉框显示原始 ID（{@code xnet.item}）；</li>
 * <li>连接器设置里"模式"按钮显示 {@code Ins}/{@code Ext} —— 这不是字符串常量，
 * 而是 {@code AbstractEditorPanel#choices} 用
 * {@code StringUtils.capitalize(enum.toString().toLowerCase())} 现算出来的
 * （{@code INS → "Ins"}），{@code @ModifyConstant} 根本看不见它；</li>
 * <li>红石模式按钮的 {@code Ignored}/{@code Off}/{@code On}/{@code Pulse} 也走同一条路。</li>
 * </ul>
 *
 * <h3>本补丁怎么做</h3>
 *
 * <p>
 * 在 {@code AbstractLabel#setText(String)} 的<b>入参</b>上做 {@code @ModifyVariable}，
 * 交给 {@link XNetPatchText#localizeLabel(String)} 处理（前缀 {@code xnet.} + 白名单）。
 * </p>
 *
 * <p>
 * 关键点：<b>只改"显示文本"，不动"取值"。</b> {@code ChoiceLabel} 的取值存在它自己的
 * {@code currentChoice} 字段里，点击时 {@code addChoiceEvent} 回调拿到的仍是原始英文值
 * （{@code "Ins"}、{@code "Ignored"}…），服务端 {@code ItemMode.valueOf} /
 * {@code RSMode.valueOf} 照常工作 —— 不会出现"界面汉化了但设置保存不了/报错"。
 * </p>
 *
 * <p>
 * 白名单是严格精确匹配（不是模糊翻译），非白名单文本原样返回；未装 XNet 时由
 * {@code SuMixinPlugin} 直接跳过本补丁。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.lib.gui.widgets.AbstractLabel", remap = false)
public class McjtyAbstractLabelI18nMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true, remap = false)
    private String susyplusplus$localizeLabel(String text) {
        return XNetPatchText.localizeLabel(text);
    }
}
