package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 编辑器面板（{@code mcjty.xnet.blocks.controller.gui.AbstractEditorPanel}）
 * 里红石模式按钮的 <b>tooltip</b>。
 *
 * <h3>⚠ 只替换 tooltip，绝不能替换选项名</h3>
 *
 * <p>
 * 源码（{@code redstoneMode(...)}）是：
 * </p>
 *
 * <pre>
 * ImageChoiceLabel redstoneMode = new ImageChoiceLabel(mc, gui)
 *         .addChoice("Ignored", "Redstone mode:\nIgnored", iconGuiElements, 1, 1)
 *         ...
 * redstoneMode.addChoiceEvent((parent, newChoice) -> update(tag, newChoice));
 * </pre>
 *
 * <p>
 * 选项名 {@code "Ignored"} 会经 {@code update(tag, newChoice)} 写进 {@code data}，
 * 送到服务端后由 {@code AbstractConnectorSettings#update} 执行
 * {@code RSMode.valueOf(((String) data.get(TAG_RS)).toUpperCase())} ——
 * 也就是说<b>它同时是序列化取值</b>。若把它替换成中文，
 * 服务端 {@code valueOf("忽略")} 会直接抛 {@code IllegalArgumentException}，设置失效甚至报错。
 * </p>
 *
 * <p>
 * 所以这里<b>只</b>替换那四条 tooltip 常量（纯显示）；选项名 {@code Ignored / Off / On / Pulse}
 * 的中文化交给显示层白名单
 * （{@link com.susy.plusplus.mixin.mcjtylib.McjtyAbstractLabelI18nMixin} +
 * {@link XNetPatchText#localizeLabel}），这样取值仍是英文、只改屏幕上的字。
 * </p>
 *
 * <p>
 * 原文 tooltip 里含<b>真实换行符</b>（{@code "Redstone mode:\nIgnored"}）；
 * 中文译文统一写成单行，避免踩 {@code .lang} 的 {@code \n}/{@code #PARSE_ESCAPES} 坑。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.controller.gui.AbstractEditorPanel", remap = false)
public class AbstractEditorPanelMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Redstone mode:\nIgnored"))
    private String susyplusplus$ignoredTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.rs.ignored");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Redstone mode:\nOff to activate"))
    private String susyplusplus$offTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.rs.off");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Redstone mode:\nOn to activate"))
    private String susyplusplus$onTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.rs.on");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Do one operation\non a pulse"))
    private String susyplusplus$pulseTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.rs.pulse");
    }
}
