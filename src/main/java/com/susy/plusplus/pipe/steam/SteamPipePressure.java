package com.susy.plusplus.pipe.steam;

/**
 * 蒸汽管道的两个压力等级。
 *
 * <p>
 * 每一级都有一套"微型 → 巨型"共 5 种粗细（见 {@link SteamPipeSize}），
 * 速率为：
 * </p>
 *
 * <pre>
 * 速率(L/t) = baseThroughput（该等级微型管道的速率） × 规格倍率(1/2/6/12/24)
 * </pre>
 *
 * <p>
 * 低压基准 64 L/t（微型即 64），高压基准 256 L/t（微型即 256）。
 * 于是：
 * </p>
 *
 * <pre>
 * 低压： 64 / 128 / 384 /  768 / 1536
 * 高压：256 / 512 / 1536 / 3072 / 6144
 * </pre>
 */
public enum SteamPipePressure {

    LOW_PRESSURE("low_pressure", 64),
    HIGH_PRESSURE("high_pressure", 256);

    public static final SteamPipePressure[] VALUES = values();

    private final String name;
    private final int baseThroughput;

    SteamPipePressure(String name, int baseThroughput) {
        this.name = name;
        this.baseThroughput = baseThroughput;
    }

    public String getName() {
        return name;
    }

    /** 该压力等级<b>微型</b>管道的速率（L/t）。 */
    public int getBaseThroughput() {
        return baseThroughput;
    }

    /** 指定规格下的速率（L/t）。 */
    public int getThroughput(SteamPipeSize size) {
        return baseThroughput * size.getCapacityMultiplier();
    }
}
