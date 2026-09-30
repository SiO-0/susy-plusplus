package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 路由器界面（{@code mcjty.xnet.blocks.router.GuiRouter}）里的三个表头标签。
 *
 * <p>
 * 源码里就是 {@code new Label(mc, this).setText("Ch" / "Pos" / "Index")}，
 * 用的是自己的方法（{@code createLocalChannelPanel} 等），不参与混淆、
 * 但也没有走 I18n，因此必须替换常量。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.router.GuiRouter", remap = false)
public class GuiRouterMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Ch"))
    private String susyplusplus$ch(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.router.ch");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Pos"))
    private String susyplusplus$pos(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.router.pos");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Index"))
    private String susyplusplus$index(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.router.index");
    }
}
