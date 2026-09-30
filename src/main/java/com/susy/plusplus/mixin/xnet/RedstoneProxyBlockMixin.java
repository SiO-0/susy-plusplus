package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.config.SuConfig;

import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 红石代理方块（不触发方块更新版）tooltip 的硬编码文本 → 可本地化键（②）。
 *
 * <p>
 * 目标类：{@code mcjty.xnet.blocks.redstoneproxy.RedstoneProxyBlock}；
 * 实测硬编码字面量：{@code "Acts as a proxy block for"}、
 * {@code "redstone. XNet can connect to this"}、
 * {@code "This version does no block update!"}（这一行原版带 {@code TextFormatting.YELLOW}，
 * 我们只替换文本本身，颜色照旧）。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.blocks.redstoneproxy.RedstoneProxyBlock", remap = false)
public class RedstoneProxyBlockMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Acts as a proxy block for"))
    private String susyplusplus$line1(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.line1");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "redstone. XNet can connect to this"))
    private String susyplusplus$line2(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.line2");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "This version does no block update!"))
    private String susyplusplus$noUpdate(String original) {
        return localize(original, "susyplusplus.patch.xnet.proxy.no_update");
    }

    private static String localize(String original, String key) {
        if (!SuConfig.enableThirdPartyPatches) {
            return original;
        }
        return I18n.format(key);
    }
}
