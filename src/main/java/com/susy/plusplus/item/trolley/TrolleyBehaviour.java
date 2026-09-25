package com.susy.plusplus.item.trolley;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.items.metaitem.stats.IItemComponent;

import java.util.List;

/**
 * 手推车的物品行为：tooltip + 右键。
 *
 * <p>
 * tooltip 按需求分两种状态：
 * </p>
 *
 * <ul>
 * <li>空车：<code>susyplusplus.tooltip.trolley.empty</code>（"空"）；</li>
 * <li>已装载：<code>susyplusplus.tooltip.trolley.loaded</code>（"已装载：%s"），
 * 名字用 <b>键</b> 在客户端翻译（搬起发生在服务端，服务端没有语言表）；</li>
 * <li>固定两行使用说明：<code>...pickup</code> / <code>...place</code>。</li>
 * </ul>
 *
 * <p>
 * 与机器的交互（Shift + 右键搬起 / 右键放下）由 {@code TrolleyEventHandler} 通过
 * Forge 的 {@code RightClickBlock} 事件处理 —— 那里才能拿到坐标与点击面，
 * 并能在必要时取消 GT 自己的交互。空中右键什么都不做。
 * </p>
 */
public class TrolleyBehaviour implements IItemComponent, IItemBehaviour {

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        String displayKey = TrolleyData.getDisplayKey(itemStack);
        if (displayKey == null) {
            lines.add(I18n.format("susyplusplus.tooltip.trolley.empty"));
        } else {
            lines.add(I18n.format("susyplusplus.tooltip.trolley.loaded", I18n.format(displayKey)));
        }
        lines.add(I18n.format("susyplusplus.tooltip.trolley.pickup"));
        lines.add(I18n.format("susyplusplus.tooltip.trolley.place"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        // 空中右键：什么也不做（放下走 RightClickBlock 事件）
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }
}
