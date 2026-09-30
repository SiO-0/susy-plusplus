package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** XNet 流体频道设置界面（{@code mcjty.xnet.apiimpl.fluids.FluidChannelSettings}）的 tooltip。 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.fluids.FluidChannelSettings", remap = false)
public class FluidChannelSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Fluid distribution mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.fluid_channel.mode");
    }
}
