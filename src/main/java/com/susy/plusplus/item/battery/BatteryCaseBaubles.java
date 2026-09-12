package com.susy.plusplus.item.battery;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.integration.baubles.BaubleBehavior;
import gregtech.integration.baubles.BaublesModule;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import baubles.api.BaubleType;

/**
 * Baubles（饰品栏）兼容。
 *
 * <p>
 * <b>本类必须仅在 Baubles 已加载时被调用</b>（见 {@code SuMetaItems.init}），
 * 因为它引用了 Baubles 的类（{@code baubles.api.*}）——这是与 GT
 * {@code gregtech.integration.baubles.BaublesModule} 相同的隔离做法。
 * </p>
 *
 * <p>
 * 复用 GT 的
 * {@link BaubleBehavior}（{@code IItemCapabilityProvider + ICapabilityProvider + IBauble}）。
 * </p>
 */
public final class BatteryCaseBaubles {

    private BatteryCaseBaubles() {
    }

    /** 给电池盒添加 {@code BaubleType.TRINKET} 饰品能力（与 GT 电池一致）。 */
    public static void addTrinket(MetaItem<?>.MetaValueItem item) {
        item.addComponents(new BaubleBehavior(BaubleType.TRINKET));
    }

    /** 玩家背包 + 饰品栏的合并视图（用于释能模式自动充能）。 */
    public static IInventory wrapInventory(EntityPlayer player) {
        return BaublesModule.getBaublesWrappedInventory(player);
    }
}
