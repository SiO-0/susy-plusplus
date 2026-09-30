package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 连接器设置的公共部分（{@code mcjty.xnet.api.helper.AbstractConnectorSettings}）：
 * 侧面选择与"颜色使能"的 tooltip。
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.api.helper.AbstractConnectorSettings", remap = false)
public class AbstractConnectorSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Side from which to operate"))
    private String susyplusplus$side(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.side");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Enable on color"))
    private String susyplusplus$color(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.color");
    }
}
