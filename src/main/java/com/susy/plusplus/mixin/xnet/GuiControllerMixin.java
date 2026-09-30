package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 把 XNet 控制器界面（{@code mcjty.xnet.blocks.controller.gui.GuiController}）里
 * <b>硬编码</b>的英文提示/按钮/标签改成可本地化的键。
 *
 * <p>
 * 这些字符串在 {@code xnet-1.12-1.8.4-ynet.jar} 的该类常量池里（javap 实测全部存在），
 * 完全不走 I18n，所以无论语言文件怎么补都还是英文。
 * </p>
 *
 * <p>
 * 手法：{@code @ModifyConstant(method = "*", ...)} 直接替换字符串常量，
 * <b>不依赖任何方法名</b> —— 因此既不需要 refmap，也不会因为 XNet 版本变动而指错方法；
 * 找不到目标时 Mixin 默认只记一条日志、不会崩（{@code require} 默认不强制）。
 * 关掉 {@code SuConfig.enableThirdPartyPatches} 即整体恢复原样。
 * </p>
 *
 * <p>
 * 注意：<b>不翻译</b>那些作为 GUI 命令标签 / NBT 键 / JSON 字段的常量
 * （如 {@code "channel"}、{@code "type"}、{@code "name"}、{@code "block"}），
 * 也<b>不动</b>纯符号按钮（{@code "x"}、{@code "C"}、{@code "?"}）。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.controller.gui.GuiController", remap = false)
public class GuiControllerMixin {

    // ---- 原有 3 条 ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "The block is now highlighted"))
    private String susyplusplus$highlighted(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.highlighted");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Copied channel"))
    private String susyplusplus$copiedChannel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.copied_channel");
    }

    /** 原版是 {@code "... " + (频道号 + 1) + "?"}，所以这里只翻译前缀（保留尾部空格）。 */
    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Really remove channel "))
    private String susyplusplus$removeChannelPrefix(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.remove_channel_prefix");
    }

    // ---- 标签 / 按钮 ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Channel"))
    private String susyplusplus$channelLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.channel_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Create"))
    private String susyplusplus$create(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.create");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Paste"))
    private String susyplusplus$paste(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.paste");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Cancel"))
    private String susyplusplus$cancel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.cancel");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "OK"))
    private String susyplusplus$ok(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.ok");
    }

    // ---- 连接器 / 方块信息 ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Connector:"))
    private String susyplusplus$connectorLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.connector_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Block:"))
    private String susyplusplus$blockLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.block_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Position:"))
    private String susyplusplus$positionLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.position_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "(doubleclick to highlight)"))
    private String susyplusplus$doubleclickHint(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.doubleclick_hint");
    }

    // ---- tooltip ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Enable processing on this channel"))
    private String susyplusplus$enableProcessing(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.enable_processing");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Channel name"))
    private String susyplusplus$channelNameTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.channel_name_tip");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Remove this channel"))
    private String susyplusplus$removeChannelTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.remove_channel_tip");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Copy this channel to"))
    private String susyplusplus$copyChannel1(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.copy_channel_1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Copy this connector"))
    private String susyplusplus$copyConnector1(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.copy_connector_1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "the clipboard"))
    private String susyplusplus$theClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.the_clipboard");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "to the clipboard"))
    private String susyplusplus$toTheClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.to_the_clipboard");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "from the clipboard"))
    private String susyplusplus$fromTheClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.from_the_clipboard");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Create a new channel"))
    private String susyplusplus$createChannel1(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.create_channel_1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Create a new connector"))
    private String susyplusplus$createConnector1(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.create_connector_1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Remove this connector"))
    private String susyplusplus$removeConnectorTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.remove_connector_tip");
    }

    // ---- 错误 / 提示信息 ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Nothing selected!"))
    private String susyplusplus$nothingSelected(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.nothing_selected");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Error copying to clipboard!"))
    private String susyplusplus$errorCopy(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.copy_clipboard");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Clipboard too large!"))
    private String susyplusplus$clipboardTooLarge(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.clipboard_too_large");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Unsupported channel type:"))
    private String susyplusplus$unsupportedType(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.unsupported_type");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Clipboard does not contain connector!"))
    private String susyplusplus$noConnectorInClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.clipboard_no_connector");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Clipboard does not contain channel!"))
    private String susyplusplus$noChannelInClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.clipboard_no_channel");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Error reading from clipboard!"))
    private String susyplusplus$errorReadClipboard(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.err.read_clipboard");
    }
}
