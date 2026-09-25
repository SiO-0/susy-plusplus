package com.susy.plusplus.item.configurator;

import com.susy.plusplus.Tags;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;

import net.minecraft.util.ResourceLocation;

/**
 * 「配置器」物品（MetaItem，GT 风格）。
 *
 * <p>
 * 注册名 {@code susyplusplus:configurator}，<b>不可堆叠</b>，模式与已复制的配置都写在
 * 它自己的 NBT 里（见 {@link ConfiguratorMode} / {@link ConfiguratorData}）。
 * </p>
 *
 * <p>
 * 模型：{@code createItemModelPath} 按 GT 约定返回<b>不含 {@code item/} 前缀</b>的路径，
 * 因此 {@code susyplusplus:configurator} 对应
 * {@code assets/susyplusplus/models/item/configurator.json}，
 * 其 {@code layer0 = susyplusplus:item/configurator} 对应
 * {@code assets/susyplusplus/textures/item/configurator.png}。
 * </p>
 */
public class ItemConfigurator extends StandardMetaItem {

    /** 唯一子物品。 */
    public final MetaItem<?>.MetaValueItem configurator;

    public ItemConfigurator() {
        setRegistryName(Tags.MOD_ID, "configurator");
        this.configurator = addItem(0, "configurator")
                .setMaxStackSize(1)
                .addComponents(new ConfiguratorBehaviour());
    }

    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        return new ResourceLocation(Tags.MOD_ID, "configurator" + (postfix == null ? "" : postfix));
    }
}
