package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 流体连接器设置界面（{@code mcjty.xnet.apiimpl.fluids.FluidConnectorSettings}）的标签与 tooltip。
 *
 * <p>
 * {@code "Fluid extraction rate|(max " + maxrate + "mb)"} 是拼接出来的，
 * 所以三段常量分别替换；中文里统一写成 {@code 流体提取速率（最大 1000 mb）}。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.fluids.FluidConnectorSettings", remap = false)
public class FluidConnectorSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insert or extract mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.mode");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Number of ticks for each operation"))
    private String susyplusplus$speed(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.speed");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insertion priority"))
    private String susyplusplus$priority(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.priority");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Fluid extraction rate|(max "))
    private String susyplusplus$extractRate(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid.extract_rate");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Fluid insertion rate|(max "))
    private String susyplusplus$insertRate(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid.insert_rate");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "mb)"))
    private String susyplusplus$mb(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid.mb");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Keep this amount of|fluid in tank"))
    private String susyplusplus$minmaxExtract(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid.minmax_ext");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Disable insertion if|fluid level is too high"))
    private String susyplusplus$minmaxInsert(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid.minmax_ins");
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

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Filter"))
    private String susyplusplus$filterLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.label.filter");
    }
}
