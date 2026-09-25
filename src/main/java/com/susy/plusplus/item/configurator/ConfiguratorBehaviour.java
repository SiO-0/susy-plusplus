package com.susy.plusplus.item.configurator;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.items.metaitem.stats.IItemComponent;

import java.util.List;

/**
 * 配置器的物品行为：tooltip + 右键。
 *
 * <p>
 * <b>tooltip</b>：显示当前模式（{@code Mode}）以及是否已复制了机器配置
 * （{@code CopiedConfig}）—— 这正是需求里"写入 NBT 后物品 tooltip 需要显示当前模式"。
 * </p>
 *
 * <p>
 * <b>右键</b>：空中右键不做任何事；与机器的交互（Shift + 右键 / 普通右键）由
 * {@code ConfiguratorEventHandler} 通过 Forge 的 {@code RightClickBlock} 事件处理 ——
 * 因为那里才能拿到方块坐标与面，并能在需要时取消 GT 自己的交互。
 * </p>
 */
public class ConfiguratorBehaviour implements IItemComponent, IItemBehaviour {

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        ConfiguratorMode mode = ConfiguratorMode.get(itemStack);

        lines.add(I18n.format("susyplusplus.tooltip.configurator.mode",
                I18n.format(mode.getLangKey())));
        // 当前模式对应的提示；NONE 时才提示"Shift+V 打开界面"
        // （原来这里还无条件再加一行同样的"打开界面"，导致 tooltip 出现重复行 —— 已移除）
        if (mode == ConfiguratorMode.NONE) {
            lines.add(I18n.format("susyplusplus.tooltip.configurator.open"));
        } else {
            lines.add(I18n.format("susyplusplus.tooltip.configurator.hint." + mode.name().toLowerCase()));
        }

        if (ConfiguratorData.hasCopiedConfig(itemStack)) {
            lines.add(I18n.format("susyplusplus.tooltip.configurator.has_copy"));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        // 空中右键：什么都不做（打开界面走 Shift+V，与机器交互走 RightClickBlock 事件）。
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }
}
