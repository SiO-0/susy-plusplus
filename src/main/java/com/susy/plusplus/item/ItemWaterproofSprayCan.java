package com.susy.plusplus.item;

import com.susy.plusplus.Tags;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;

import net.minecraft.util.ResourceLocation;

/**
 * “防水喷漆”物品。
 *
 * <p>
 * 沿用 GT 原版喷漆罐的物品框架（{@link StandardMetaItem} +
 * {@link WaterproofSprayBehaviour}），
 * 容量 300 次，单次消耗 1。
 * </p>
 *
 * <p>
 * <b>模型 / 贴图</b>：直接复用 GT 原版“蓝色喷漆罐”的模型与贴图，本模组不再自带任何模型资源。
 * </p>
 *
 * <p>
 * <b>关键约定</b>：GT 的 {@code createItemModelPath} 返回的 ResourceLocation <b>不带</b>
 * {@code item/} 前缀
 * ——查找物品模型时原版会自动补 {@code item/}。因此
 * {@code gregtech:metaitems/spray.can.dyes.blue} 实际对应
 * {@code assets/gregtech/models/item/metaitems/spray.can.dyes.blue.json}。
 * （若写成 {@code susyplusplus:item/xxx}，会去找 {@code models/item/item/xxx.json}，
 * 从而落到 {@code builtin/missing}，表现为“紫黑格”。）
 * </p>
 */
public class ItemWaterproofSprayCan extends StandardMetaItem {

    /** 唯一子物品（容量 300 次）。 */
    public final MetaItem<?>.MetaValueItem sprayCan;

    public ItemWaterproofSprayCan() {
        setRegistryName(Tags.MOD_ID, "waterproof_spray_can");
        this.sprayCan = addItem(0, "waterproof_spray_can")
                .setMaxStackSize(1)
                .addComponents(new WaterproofSprayBehaviour());
    }

    /**
     * 复用 GT 原版蓝色喷漆罐的模型（及其贴图）。
     *
     * <p>
     * 返回的 ResourceLocation 按 GT 约定<b>不含</b> {@code item/} 前缀。
     * </p>
     */
    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        String suffix = postfix == null ? "" : postfix;
        return new ResourceLocation("gregtech", "metaitems/spray.can.dyes.blue" + suffix);
    }
}
