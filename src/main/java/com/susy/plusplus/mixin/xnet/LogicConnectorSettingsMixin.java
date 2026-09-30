package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** XNet 逻辑连接器设置界面（{@code mcjty.xnet.apiimpl.logic.LogicConnectorSettings}）的标签与 tooltip。 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.logic.LogicConnectorSettings", remap = false)
public class LogicConnectorSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Sensor or Output mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.logic.mode");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Number of ticks for each check"))
    private String susyplusplus$speedCheck(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.logic.speed_check");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Number of ticks for each operation"))
    private String susyplusplus$speedOperation(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.speed");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Redstone:"))
    private String susyplusplus$redstoneLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.logic.redstone");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Redstone output value"))
    private String susyplusplus$redstoneOut(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.logic.rsout");
    }
}
