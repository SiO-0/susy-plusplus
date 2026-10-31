package com.susy.plusplus.pipe.steam;

/**
 * 蒸汽管道的 5 种粗细，数值<b>完全照抄</b> GT 的 {@code FluidPipeType}
 * （2.8.7 源码核实）：
 *
 * <pre>
 * TINY   thickness 0.25  capacityMultiplier 1
 * SMALL  thickness 0.375 capacityMultiplier 2
 * NORMAL thickness 0.5   capacityMultiplier 6
 * LARGE  thickness 0.75  capacityMultiplier 12
 * HUGE   thickness 0.875 capacityMultiplier 24
 * </pre>
 *
 * <p>
 * 因此"蒸汽管道速率倍率同流体管道"这句话在本模组里就体现为
 * {@code 速率 = 压力基础速率 × capacityMultiplier}。
 * </p>
 */
public enum SteamPipeSize {

    TINY("tiny", 0.25f, 1),
    SMALL("small", 0.375f, 2),
    NORMAL("normal", 0.5f, 6),
    LARGE("large", 0.75f, 12),
    HUGE("huge", 0.875f, 24);

    public static final SteamPipeSize[] VALUES = values();

    private final String name;
    private final float thickness;
    private final int capacityMultiplier;

    SteamPipeSize(String name, float thickness, int capacityMultiplier) {
        this.name = name;
        this.thickness = thickness;
        this.capacityMultiplier = capacityMultiplier;
    }

    /** {@code tiny} / {@code small} / {@code normal} / {@code large} / {@code huge}。 */
    public String getName() {
        return name;
    }

    public float getThickness() {
        return thickness;
    }

    /** 与 GT 流体管道一致的规格倍率。 */
    public int getCapacityMultiplier() {
        return capacityMultiplier;
    }

    /** 在 {@link #VALUES} 中的下标（用于配方电路号与语言键）。 */
    public int getIndex() {
        return ordinal();
    }
}
