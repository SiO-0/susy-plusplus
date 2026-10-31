package com.susy.plusplus.pipe.steam;

import gregtech.api.pipenet.block.IPipeType;

/**
 * 蒸汽管道的 10 种组合：2 个压力等级 × 5 种粗细。
 *
 * <p>
 * 之所以做成一个 10 项的枚举，是因为 GT 的 {@code BlockPipe} 要求
 * {@code PipeType} 是 {@code Enum}，且每种组合要能独立序列化/比较。
 * 每个枚举常量对应<b>一个方块实例</b>（低压 5 个 + 高压 5 个），
 * 与 GT "每种粗细一个方块" 的组织方式相同。
 * </p>
 *
 * <p>
 * 速率与粗细的数值全部来自 {@link SteamPipePressure}（基础速率）与
 * {@link SteamPipeSize}（GT 流体管道的倍率），本枚举不重复硬编码。
 * </p>
 */
public enum SteamPipeType implements IPipeType<SteamPipeProperties> {

    LOW_TINY(SteamPipePressure.LOW_PRESSURE, SteamPipeSize.TINY),
    LOW_SMALL(SteamPipePressure.LOW_PRESSURE, SteamPipeSize.SMALL),
    LOW_NORMAL(SteamPipePressure.LOW_PRESSURE, SteamPipeSize.NORMAL),
    LOW_LARGE(SteamPipePressure.LOW_PRESSURE, SteamPipeSize.LARGE),
    LOW_HUGE(SteamPipePressure.LOW_PRESSURE, SteamPipeSize.HUGE),

    HIGH_TINY(SteamPipePressure.HIGH_PRESSURE, SteamPipeSize.TINY),
    HIGH_SMALL(SteamPipePressure.HIGH_PRESSURE, SteamPipeSize.SMALL),
    HIGH_NORMAL(SteamPipePressure.HIGH_PRESSURE, SteamPipeSize.NORMAL),
    HIGH_LARGE(SteamPipePressure.HIGH_PRESSURE, SteamPipeSize.LARGE),
    HIGH_HUGE(SteamPipePressure.HIGH_PRESSURE, SteamPipeSize.HUGE);

    public static final SteamPipeType[] VALUES = values();

    private final SteamPipePressure pressure;
    private final SteamPipeSize size;
    private final SteamPipeProperties properties;

    SteamPipeType(SteamPipePressure pressure, SteamPipeSize size) {
        this.pressure = pressure;
        this.size = size;
        this.properties = new SteamPipeProperties(pressure.getBaseThroughput(), pressure.getThroughput(size));
    }

    public SteamPipePressure getPressure() {
        return pressure;
    }

    public SteamPipeSize getSize() {
        return size;
    }

    /** 每 tick 传输量（L/t）= 压力基础速率 × 规格倍率。 */
    public int getThroughput() {
        return properties.getThroughput();
    }

    /**
     * {@link IPipeType#getName()}：{@code low_pressure_tiny} 之类。
     *
     * <p>
     * 方块注册名再用 {@link #getRegistryName()} 拼成
     * {@code low_pressure_steam_pipe_tiny}，语言键后缀也是这个串。
     * </p>
     */
    @Override
    public String getName() {
        return pressure.getName() + "_" + size.getName();
    }

    /** 方块注册名（不含命名空间）：{@code low_pressure_steam_pipe_tiny}。 */
    public String getRegistryName() {
        return pressure.getName() + "_steam_pipe_" + size.getName();
    }

    @Override
    public float getThickness() {
        return size.getThickness();
    }

    /**
     * GT 用这个方法把材质属性乘以规格倍率；本模组速率已由枚举固定，
     * 这里返回与当前规格一致的节点数据。
     */
    @Override
    public SteamPipeProperties modifyProperties(SteamPipeProperties properties) {
        return this.properties;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    /** 同一压力等级才能连成一张网（见 {@link SteamPipeProperties#equals}）。 */
    public boolean samePressure(SteamPipeType other) {
        return other != null && other.pressure == pressure;
    }

    public SteamPipeProperties createProperties() {
        return properties;
    }
}
