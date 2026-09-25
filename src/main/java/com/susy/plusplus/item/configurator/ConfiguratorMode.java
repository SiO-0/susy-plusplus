package com.susy.plusplus.item.configurator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 配置器的模式。
 *
 * <p>
 * 数值即写入物品 NBT（键 {@code "Mode"}）的值，<b>不可随意改动顺序</b>（存档兼容）。
 * </p>
 */
public enum ConfiguratorMode {

    /** 0 = 无（未选择任何模式）。 */
    NONE(0, "none"),

    /** 1 = 修改机器输出面。 */
    MODIFY_OUTPUT(1, "modify_output"),

    /** 2 = 复制机器配置。 */
    COPY_CONFIG(2, "copy_config"),

    /** 3 = 机器工具箱。 */
    MACHINE_TOOLBOX(3, "machine_toolbox");

    /** 物品 NBT 键。 */
    public static final String NBT_MODE = "Mode";

    private final int id;
    private final String key;

    ConfiguratorMode(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public int getId() {
        return this.id;
    }

    /** 本地化键后缀：{@code susyplusplus.configurator.mode.<key>}。 */
    public String getLangKey() {
        return "susyplusplus.configurator.mode." + this.key;
    }

    /** @return 配置器物品当前的模式；没有 NBT 或数值非法时返回 {@link #NONE}。 */
    public static ConfiguratorMode get(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return NONE;
        }
        return byId(stack.getTagCompound().getInteger(NBT_MODE));
    }

    public static ConfiguratorMode byId(int id) {
        for (ConfiguratorMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return NONE;
    }

    /**
     * 把模式写入物品 NBT。
     *
     * <p>
     * <b>切换模式时会清空配置器之前的 NBT</b>（包括已复制的 {@code CopiedConfig}），
     * 只留下新模式的 {@code Mode}。
     * </p>
     */
    public void write(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(NBT_MODE, this.id);
        stack.setTagCompound(tag);
    }
}
