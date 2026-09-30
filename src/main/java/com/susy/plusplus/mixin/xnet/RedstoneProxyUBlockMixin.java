package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.config.SuConfig;

import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 红石代理方块（<b>触发方块更新</b>版）tooltip 的硬编码文本 → 可本地化键（②）。
 *
 * <p>
 * 目标类：{@code mcjty.xnet.blocks.redstoneproxy.RedstoneProxyUBlock}；
 * 硬编码字面量与不更新版相同，只有最后一行不同：
 * {@code "This version does a block update!"}。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.redstoneproxy.RedstoneProxyUBlock", remap = false)
public class RedstoneProxyUBlockMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Acts as a proxy block for"))
    private String susyplusplus$line1(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.line1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "redstone. XNet can connect to this"))
    private String susyplusplus$line2(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.line2");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "This version does a block update!"))
    private String susyplusplus$withUpdate(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.with_update");
    }

    private static String localize(String original, String key) {
        if (!SuConfig.enableThirdPartyPatches) {
            return original;
        }
        return I18n.format(key);
    }
}
