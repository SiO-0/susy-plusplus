package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** XNet 物品频道设置界面（{@code mcjty.xnet.apiimpl.items.ItemChannelSettings}）的 tooltip。 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.items.ItemChannelSettings", remap = false)
public class ItemChannelSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Item distribution mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item_channel.mode");
    }
}
