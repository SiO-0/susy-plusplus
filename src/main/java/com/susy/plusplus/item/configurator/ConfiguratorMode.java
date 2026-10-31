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
    MACHINE_TOOLBOX(3, "machine_toolbox"),

    /** 4 = 容量限制（仅流体）。<b>追加在末尾</b>，保证旧存档的 {@code Mode} 数值不变。 */
    FLUID_CAPACITY(4, "fluid_capacity"),

    /**
     * 5 = 给已放置的机器重命名（等价于「在铁砧里改名后再放下」的效果）。
     *
     * <p>
     * 名字来源是<b>配置器自身的铁砧名字</b>（{@code display.Name}）：先在铁砧里把配置器改名，
     * 再普通右键机器即可把该名字写到机器上；配置器没有铁砧名字时右键则<b>清除</b>机器名字。
     * </p>
     */
    RENAME(5, "rename");

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
     * 只留下新模式的 {@code Mode} —— 但会<b>保留铁砧改名的 {@code display} 标签</b>，
     * 否则 {@link #RENAME} 模式永远拿不到名字（写模式会把改名结果一并抹掉）。
     * </p>
     */
    public void write(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        NBTTagCompound old = stack.getTagCompound();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(NBT_MODE, this.id);
        if (old != null && old.hasKey("display", 10)) {
            tag.setTag("display", old.getCompoundTag("display"));
        }
        stack.setTagCompound(tag);
    }
}
