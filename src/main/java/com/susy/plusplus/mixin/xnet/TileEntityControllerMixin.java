package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.config.SuConfig;

import net.minecraft.util.text.translation.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 控制器（服务端）里<b>硬编码</b>的错误提示 → 可本地化键（②）。
 *
 * <p>
 * 目标类：{@code mcjty.xnet.blocks.controller.TileEntityController}。
 * 它把这些英文串直接塞进 {@code PacketControllerError("...")} 发给玩家，
 * 所以补语言文件也没用。这里在<b>发出去之前</b>换成翻译后的文本。
 * </p>
 *
 * <p>
 * ⚠ 这是服务端（通用）mixin，所以用 <b>服务端可用</b>的
 * {@link I18n}（{@code net.minecraft.util.text.translation.I18n}），
 * 而不是客户端专属的 {@code net.minecraft.client.resources.I18n} ——
 * 单人游戏里整合服务端与客户端同语言，因此中文环境会正常显示中文。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.controller.TileEntityController", remap = false)
public class TileEntityControllerMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Error copying connector!"))
    private String susyplusplus$copyConnector(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.copy_connector");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Channel does not support this!"))
    private String susyplusplus$channelUnsupported(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.channel_unsupported");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Invalid connector json!"))
    private String susyplusplus$invalidConnector(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.invalid_connector");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Wrong channel type!"))
    private String susyplusplus$wrongChannelType(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.wrong_channel_type");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Advanced connector is needed!"))
    private String susyplusplus$needAdvanced(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.need_advanced");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Error pasting clipboard data!"))
    private String susyplusplus$pasteFailed(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.paste_failed");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Invalid channel json!"))
    private String susyplusplus$invalidChannel(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.invalid_channel");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Not everything could be pasted!"))
    private String susyplusplus$partialPaste(String original) {
        return localize(original, "susyplusplus.patch.xnet.err.partial_paste");
    }

    private static String localize(String original, String key) {
        if (!SuConfig.enableThirdPartyPatches) {
            return original;
        }
        return I18n.translateToLocal(key);
    }
}
