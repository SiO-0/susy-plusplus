package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 连接器界面（{@code mcjty.xnet.blocks.cables.GuiConnector}）里的硬编码标签与提示。
 *
 * <p>
 * 只在字符串常量上做手脚（{@code method = "*"}），不依赖任何方法名，
 * 因此不需要 refmap、也不会因为 XNet 小版本改动而指错方法。
 * 详见 {@code docs/third_party_patches.md} 第 1.3 节。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.cables.GuiConnector", remap = false)
public class GuiConnectorMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Set the name of this connector"))
    private String susyplusplus$nameTip(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.conn.name_tip");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Name:"))
    private String susyplusplus$nameLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.conn.name_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Directions:"))
    private String susyplusplus$directions(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.conn.directions");
    }
}
