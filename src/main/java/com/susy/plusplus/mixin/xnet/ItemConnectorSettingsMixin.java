package com.susy.plusplus.mixin.xnet;

import com.susy.plusplus.integration.xnet.XNetPatchText;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * XNet 物品连接器设置界面（{@code mcjty.xnet.apiimpl.items.ItemConnectorSettings}）
 * 的标签与 tooltip。
 *
 * <p>
 * 只翻译显示文本；{@code "mode" / "priority" / "count" / "speed" / "flt" / "od" /
 * "meta" / "nbt" / "stack" / "extract" / "blacklist" / "extract_amount"} 等是
 * <b>GUI 命令标签</b>，绝不能动。
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.xnet.apiimpl.items.ItemConnectorSettings", remap = false)
public class ItemConnectorSettingsMixin {

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insert or extract mode"))
    private String susyplusplus$mode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.mode");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Single item, stack, or count"))
    private String susyplusplus$stack(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.stack");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Amount of items to extract|per operation"))
    private String susyplusplus$extractAmount(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.extract_amount");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Number of ticks for each operation"))
    private String susyplusplus$speed(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.speed");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Insertion priority"))
    private String susyplusplus$priority(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.settings.priority");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Amount in destination inventory|to keep"))
    private String susyplusplus$countKeep(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.count_keep");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Max amount in destination|inventory"))
    private String susyplusplus$countMax(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.count_max");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Extract mode (first available,|random slot or round robin)"))
    private String susyplusplus$extractMode(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.extract_mode");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Enable blacklist mode"))
    private String susyplusplus$blacklist(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.blacklist");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Ore dictionary matching"))
    private String susyplusplus$oredict(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.oredict");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Metadata matching"))
    private String susyplusplus$meta(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.meta");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "NBT matching"))
    private String susyplusplus$nbt(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.nbt");
    }

    // ---- 小标签 ----

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Pri"))
    private String susyplusplus$priLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.label.pri");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "BL"))
    private String susyplusplus$blLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.bl_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Ore"))
    private String susyplusplus$oreLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.ore_label");
    }

    @ModifyConstant(method = "*", constant = @Constant(stringValue = "Meta"))
    private String susyplusplus$metaLabel(String original) {
        return XNetPatchText.localize(original, "susyplusplus.patch.xnet.item.meta_label");
    }
}
