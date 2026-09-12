package com.susy.plusplus.item.battery;

import com.susy.plusplus.Tags;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * “电池盒”物品（MetaItem，GT 风格）。
 *
 * <p>
 * 注册名 {@code susyplusplus:battery_case}，不可堆叠，携带内部 4 格库存与能量能力。
 * </p>
 *
 * <p>
 * 模型：{@code createItemModelPath} 按 GT 约定返回 <b>不含 {@code item/} 前缀</b> 的路径，
 * 因此 {@code susyplusplus:battery_case} 对应
 * {@code assets/susyplusplus/models/item/battery_case.json}，
 * 其 {@code layer0 = susyplusplus:item/battery_case} 对应
 * {@code assets/susyplusplus/textures/item/battery_case.png}。
 * </p>
 */
public class ItemBatteryCase extends StandardMetaItem {

    /** 唯一子物品。 */
    public final MetaItem<?>.MetaValueItem batteryCase;

    public ItemBatteryCase() {
        setRegistryName(Tags.MOD_ID, "battery_case");
        this.batteryCase = addItem(0, "battery_case")
                .setMaxStackSize(1)
                .addComponents(new BatteryCaseBehaviour());
    }

    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        return new ResourceLocation(Tags.MOD_ID, "battery_case" + (postfix == null ? "" : postfix));
    }

    // 注意：不要覆写 getNBTShareTag / readNBTShareTag。
    // Forge 1.12.2 的 ItemStack 默认实现已把 ForgeCaps（内含本物品的
    // BatteryCaseProvider 序列化结果）一并写入 share tag 并同步到客户端；
    // 自行覆写反而会丢掉 ForgeCaps，导致客户端 tooltip / UI 看不到盒内电池。
}
