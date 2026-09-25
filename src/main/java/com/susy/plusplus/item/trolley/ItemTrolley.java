package com.susy.plusplus.item.trolley;

import com.susy.plusplus.Tags;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;

import net.minecraft.util.ResourceLocation;

/**
 * 「手推车」物品（MetaItem，GT 风格）。
 *
 * <p>
 * 注册名 {@code susyplusplus:trolley}，<b>不可堆叠</b>，装载的机器写在它自己的 NBT 里
 * （见 {@link TrolleyData}）。
 * </p>
 *
 * <p>
 * 模型：{@code createItemModelPath} 按 GT 约定返回<b>不含 {@code item/} 前缀</b>的路径，
 * 因此 {@code susyplusplus:trolley} 对应
 * {@code assets/susyplusplus/models/item/trolley.json}，
 * 其 {@code layer0 = susyplusplus:item/trolley} 对应
 * {@code assets/susyplusplus/textures/item/trolley.png}。
 * </p>
 */
public class ItemTrolley extends StandardMetaItem {

    /** 唯一子物品。 */
    public final MetaItem<?>.MetaValueItem trolley;

    public ItemTrolley() {
        setRegistryName(Tags.MOD_ID, "trolley");
        this.trolley = addItem(0, "trolley")
                .setMaxStackSize(1)
                .addComponents(new TrolleyBehaviour());
    }

    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        return new ResourceLocation(Tags.MOD_ID, "trolley" + (postfix == null ? "" : postfix));
    }
}
