package com.susy.plusplus.item;

import com.susy.plusplus.Tags;
import com.susy.plusplus.cover.SteamPumpCover;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;
import gregtech.common.items.behaviors.TooltipBehavior;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * 「蒸汽泵」物品（MetaItem，GT 风格）。
 *
 * <p>
 * 它本身只是一个承载 {@code CoverItemBehavior} 的覆盖板物品：
 * 真正的逻辑在 {@code com.susy.plusplus.cover.SteamPumpCover}，由
 * {@code CoverBehaviors.registerBehavior(...)} 在 {@link SuMetaItems#init()} 里绑定。
 * 安装方式与 GT 原版泵完全一致（手持右键机器 / 管道的某个面）。
 * </p>
 *
 * <p>
 * 模型：{@code createItemModelPath} 返回不含 {@code item/} 前缀的 {@code susyplusplus:steam_pump}，
 * 对应 {@code assets/susyplusplus/models/item/steam_pump.json}；其 {@code layer0}
 * <b>直接引用 GT 的 LV 电动泵贴图</b>
 * {@code gregtech:items/metaitems/electric.pump.lv}，因此不需要新增任何 PNG。
 * </p>
 */
public class ItemSteamPump extends StandardMetaItem {

    /** 唯一子物品。 */
    public final MetaItem<?>.MetaValueItem steamPump;

    public ItemSteamPump() {
        setRegistryName(Tags.MOD_ID, "steam_pump");
        this.steamPump = addItem(0, "steam_pump").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("susyplusplus.tooltip.steam_pump.rate", SteamPumpCover.MB_PER_TICK));
            lines.add(I18n.format("susyplusplus.tooltip.steam_pump.steam_only"));
            lines.add(I18n.format("susyplusplus.tooltip.steam_pump.install"));
        }));
    }

    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        return new ResourceLocation(Tags.MOD_ID, "steam_pump" + (postfix == null ? "" : postfix));
    }
}
