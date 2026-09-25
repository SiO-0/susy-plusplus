package com.susy.plusplus.item.configurator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 配置器物品的 NBT 读写工具。
 *
 * <p>
 * 所有数据都写在<b>物品自身的 {@code tagCompound}</b> 里
 * （1.12.2 的 {@code Item#getNBTShareTag} 只返回 {@code getTagCompound()}，
 * 放 ForgeCaps 会在同步时丢失 —— 同电池盒踩过的坑）。
 * </p>
 *
 * <p>
 * NBT 结构：
 * </p>
 *
 * <pre>
 * {
 *   "Mode": 0|1|2|3,                 // 见 ConfiguratorMode
 *   "CopiedConfig": { ... }          // 仅模式 2（复制机器配置）会有，见 MachineConfig
 * }
 * </pre>
 */
public final class ConfiguratorData {

    /** 已复制的机器配置（{@link MachineConfig} 的序列化结果）。 */
    public static final String NBT_COPIED_CONFIG = "CopiedConfig";

    private ConfiguratorData() {
    }

    /** 切换模式：<b>会清空配置器之前的全部 NBT</b>，只留下新的 {@code Mode}。 */
    public static void setMode(ItemStack stack, ConfiguratorMode mode) {
        mode.write(stack);
    }

    /** 写入已复制的配置（不会动 {@code Mode}）。 */
    public static void setCopiedConfig(ItemStack stack, MachineConfig config) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(NBT_COPIED_CONFIG, config.writeToNBT());
    }

    /** @return 已复制的配置；没有时返回 {@code null}。 */
    public static MachineConfig getCopiedConfig(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(NBT_COPIED_CONFIG)) {
            return null;
        }
        MachineConfig config = MachineConfig.readFromNBT(tag.getCompoundTag(NBT_COPIED_CONFIG));
        return config.isEmpty() ? null : config;
    }

    /** 是否有已复制的配置。 */
    public static boolean hasCopiedConfig(ItemStack stack) {
        return getCopiedConfig(stack) != null;
    }
}
