package com.susy.plusplus.pipe.steam;

/**
 * 蒸汽管道的节点数据（存在 {@code PipeNet} 的每个节点上，并随世界保存）。
 *
 * <p>
 * 只保留两项：<b>压力等级的基础速率</b>（区分低压 / 高压）与
 * <b>本根管道的速率</b>（压力 × 规格倍率）。
 * </p>
 *
 * <p>
 * {@link #equals(Object)} 只比较压力等级：同一压力的不同粗细可以连成一张网，
 * 整条路径的瓶颈由每根管道自己的速率自然决定（最细的那根限速），
 * 这与 GT 流体管道的行为一致。低压与高压则不允许连成一张网。
 * </p>
 */
public final class SteamPipeProperties {

    private final int pressureBaseThroughput;
    private final int throughput;

    public SteamPipeProperties(int pressureBaseThroughput, int throughput) {
        this.pressureBaseThroughput = Math.max(1, pressureBaseThroughput);
        this.throughput = Math.max(1, throughput);
    }

    /** 压力等级的基础速率（低压 64 / 高压 256）。 */
    public int getPressureBaseThroughput() {
        return pressureBaseThroughput;
    }

    /** 本根管道每 tick 的传输量（L/t）。 */
    public int getThroughput() {
        return throughput;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SteamPipeProperties)) {
            return false;
        }
        return pressureBaseThroughput == ((SteamPipeProperties) obj).pressureBaseThroughput;
    }

    @Override
    public int hashCode() {
        return pressureBaseThroughput;
    }

    @Override
    public String toString() {
        return "SteamPipeProperties[pressureBase=" + pressureBaseThroughput + ", throughput=" + throughput + " L/t]";
    }
}
