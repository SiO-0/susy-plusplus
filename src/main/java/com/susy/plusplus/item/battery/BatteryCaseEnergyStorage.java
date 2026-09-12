package com.susy.plusplus.item.battery;

import gregtech.api.GTValues;
import gregtech.api.capability.IElectricItem;

import net.minecraft.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * 电池盒的能量视图，实现 GT 的 {@link IElectricItem}。
 *
 * <ul>
 * <li>电量 / 容量 = 4 格电池之和（自然求和；不足 4 格时按实际数量计）</li>
 * <li>tier = 盒内电池的 tier（空盒按 ULV=0 处理，容量与电量均为 0）</li>
 * <li>充放电均逐格转发给内部电池；<b>抽取顺序为槽位 1 → 4</b>（策略 A，避免频繁切换）</li>
 * </ul>
 *
 * <p>
 * <b>充/放电后必须 {@code inventory.save()}</b>：内部电池的充放电是就地修改那个
 * ItemStack 的 NBT，不会触发 {@link BatteryCaseInventory#onContentsChanged(int)}，
 * 若不显式保存，电量变化不会写进电池盒物品的 NBT（详见 {@link BatteryCaseInventory}）。
 * </p>
 */
public class BatteryCaseEnergyStorage implements IElectricItem {

    private final BatteryCaseInventory inventory;

    public BatteryCaseEnergyStorage(BatteryCaseInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean canProvideChargeExternally() {
        return true;
    }

    @Override
    public boolean chargeable() {
        return true;
    }

    @Override
    public void addChargeListener(BiConsumer<ItemStack, Long> chargeListener) {
        // 无缓存实现，电量实时聚合，无需监听器。
    }

    @Override
    public long charge(long amount, int chargerTier, boolean ignoreTransferLimit, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        int tier = inventory.getTier();
        if (tier >= 0 && chargerTier < tier) {
            return 0L;
        }
        long remaining = amount;
        long charged = 0L;
        for (int i = 0; i < inventory.getSlots() && remaining > 0L; i++) {
            IElectricItem battery = BatteryCaseInventory.getBattery(inventory.getStackInSlot(i));
            if (battery == null) {
                continue;
            }
            long accepted = battery.charge(remaining, chargerTier, ignoreTransferLimit, simulate);
            charged += accepted;
            remaining -= accepted;
        }
        if (!simulate && charged > 0L) {
            // 内部电池 NBT 已就地改变，落盘到电池盒物品的 NBT。
            inventory.save();
        }
        return charged;
    }

    @Override
    public long discharge(long amount, int dischargerTier, boolean ignoreTransferLimit, boolean externally,
            boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        int tier = inventory.getTier();
        if (tier >= 0 && dischargerTier < tier) {
            return 0L;
        }
        long remaining = amount;
        long discharged = 0L;
        // 策略 A：按槽位 1 -> 4 顺序抽取
        for (int i = 0; i < inventory.getSlots() && remaining > 0L; i++) {
            IElectricItem battery = BatteryCaseInventory.getBattery(inventory.getStackInSlot(i));
            if (battery == null) {
                continue;
            }
            long taken = battery.discharge(remaining, dischargerTier, ignoreTransferLimit, externally, simulate);
            discharged += taken;
            remaining -= taken;
        }
        if (!simulate && discharged > 0L) {
            // 内部电池 NBT 已就地改变，落盘到电池盒物品的 NBT。
            inventory.save();
        }
        return discharged;
    }

    @Override
    public long getTransferLimit() {
        return GTValues.V[getTier()];
    }

    @Override
    public long getMaxCharge() {
        return inventory.getTotalMaxCharge();
    }

    @Override
    public long getCharge() {
        return inventory.getTotalCharge();
    }

    @Override
    public int getTier() {
        int tier = inventory.getTier();
        return tier < 0 ? GTValues.ULV : tier;
    }
}
