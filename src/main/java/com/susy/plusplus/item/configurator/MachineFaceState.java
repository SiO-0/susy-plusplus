package com.susy.plusplus.item.configurator;

/**
 * 「修改机器输出面」界面里，单个面可切换的 4 种状态。
 *
 * <p>
 * 对应需求里的循环顺序：
 * </p>
 *
 * <ol>
 * <li>{@link #NONE} 无配置</li>
 * <li>{@link #FLUID} 流体自动输出</li>
 * <li>{@link #ITEM} 物品自动输出</li>
 * <li>{@link #BOTH} 流体和物品自动输出</li>
 * </ol>
 *
 * <p>
 * GT 的机器模型是「<b>一个</b>物品输出面 + <b>一个</b>流体输出面」两个独立字段
 * （{@code SimpleMachineMetaTileEntity#getOutputFacingItems/Fluids} +
 * {@code isAutoOutputItems/Fluids}），并不存在"每个面各有一份配置"。
 * 因此本枚举只是 UI 层的展示/循环模型：
 * </p>
 *
 * <ul>
 * <li>{@link #includesItems()} → 把该面设为<b>物品</b>输出面并打开物品自动输出；</li>
 * <li>{@link #includesFluids()} → 把该面设为<b>流体</b>输出面并打开流体自动输出；</li>
 * <li>两个都不包含 → 关闭对应自动输出（旧的输出面字段会被新的面覆盖，GT 天然只保留一个）。</li>
 * </ul>
 */
public enum MachineFaceState {

    NONE(0, "none"),
    FLUID(1, "fluid"),
    ITEM(2, "item"),
    BOTH(3, "both");

    private final int id;
    private final String key;

    MachineFaceState(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public int getId() {
        return this.id;
    }

    /** 本地化键：{@code susyplusplus.configurator.face.<key>}。 */
    public String getLangKey() {
        return "susyplusplus.configurator.face." + this.key;
    }

    public boolean includesItems() {
        return this == ITEM || this == BOTH;
    }

    public boolean includesFluids() {
        return this == FLUID || this == BOTH;
    }

    /** 按需求顺序循环：无 → 流体 → 物品 → 两者 → 无。 */
    public MachineFaceState next() {
        switch (this) {
            case NONE:
                return FLUID;
            case FLUID:
                return ITEM;
            case ITEM:
                return BOTH;
            case BOTH:
            default:
                return NONE;
        }
    }

    /**
     * 由「该面是否是物品/流体输出面」+「对应自动输出是否开启」推出展示状态。
     */
    public static MachineFaceState of(boolean itemsFace, boolean autoItems, boolean fluidsFace, boolean autoFluids) {
        boolean items = itemsFace && autoItems;
        boolean fluids = fluidsFace && autoFluids;
        if (items && fluids) {
            return BOTH;
        }
        if (items) {
            return ITEM;
        }
        if (fluids) {
            return FLUID;
        }
        return NONE;
    }
}
