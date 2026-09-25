package com.susy.plusplus.machine;

import gregtech.api.capability.impl.ItemHandlerList;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Collections;

/**
 * 存储检测器对外暴露的「聚合库存」——<b>直接套用 GT 自己的
 * {@link ItemHandlerList}</b>（GT 的工作台 {@code MetaTileEntityWorkbench} 用它把
 * "自己的库存 + 周围容器的库存"合成一个可读写库存，见其 {@code connectedInventory}）。
 *
 * <p>
 * 本类只是 {@link ItemHandlerList} 的一层薄包装：<b>对象身份保持稳定</b>
 * （漏斗/管道可能缓存它），而内部的列表在每次扫描后由
 * {@link StorageScannerMachine} 重新装配（一个已扫描容器 = 一个子处理器）。
 * </p>
 *
 * <p>
 * 因为 {@code ItemHandlerList} 是 {@code IItemHandlerModifiable}，所以：
 * </p>
 *
 * <ul>
 * <li>GUI 槽位绑到它上面就能<b>直接显示真实物品</b>（服务端容器同步天然生效，
 * 不再需要自造的只读视图 + 客户端镜像）；</li>
 * <li><b>既能取、也能放</b>（与 GT 工作台的存储空间语义一致：放回时写进对应的真实容器）。</li>
 * </ul>
 */
public class AggregateItemHandler implements IItemHandlerModifiable {

    /** 空列表（扫描前/范围内无容器）。 */
    private static final ItemHandlerList EMPTY = new ItemHandlerList(Collections.emptyList());

    private ItemHandlerList list = EMPTY;

    /** 由机器在每次扫描后重新装配。 */
    public void setList(ItemHandlerList list) {
        this.list = list == null ? EMPTY : list;
    }

    public ItemHandlerList getList() {
        return list;
    }

    @Override
    public int getSlots() {
        return list.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return list.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        list.setStackInSlot(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return list.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return list.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return list.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return list.isItemValid(slot, stack);
    }
}
