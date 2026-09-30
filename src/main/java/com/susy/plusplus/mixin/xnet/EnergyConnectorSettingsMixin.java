package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 能量连接器设置界面（{@code mcjty.xnet.apiimpl.energy.EnergyConnectorSettings}）的标签与 tooltip。
 *
 * <p>
 * 速率 tooltip 是拼接的：{@code "Max energy extraction rate" + "|(limited to" + rate + " per tick)"}，
 * 三段常量分别替换。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.energy.EnergyConnectorSettings", remap = false)
public class EnergyConnectorSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insert or extract mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.mode");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insertion priority"))
    private String susyplusplus$priority(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.priority");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Max energy extraction rate"))
    private String susyplusplus$extractRate(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.extract_rate");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Max energy insertion rate"))
    private String susyplusplus$insertRate(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.insert_rate");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "|(limited to"))
    private String susyplusplus$limitedTo(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.limited_to");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = " per tick)"))
    private String susyplusplus$perTick(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.per_tick");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Disable extraction if energy|is too low"))
    private String susyplusplus$minmaxExtract(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.minmax_ext");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Disable insertion if energy|is too high"))
    private String susyplusplus$minmaxInsert(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.energy.minmax_ins");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Rate"))
    private String susyplusplus$rateLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.label.rate");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Min"))
    private String susyplusplus$minLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.label.min");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Max"))
    private String susyplusplus$maxLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.label.max");
    }
}
