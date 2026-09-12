package com.susy.plusplus.multiblock.wireless;

import net.minecraft.util.math.BlockPos;

/**
 * 无线能量传输塔的单个「目标」状态。
 *
 * <p>
 * 目标来自输入总线里<b>被铁砧重命名</b>过的无人机物品：物品名里写坐标，
 * 控制器解析出 {@link #pos} 后即为一个目标。
 * </p>
 *
 * <p>
 * 这个对象<b>不持久化</b>：每次扫描总线都会根据坐标重建（见
 * {@code MetaTileEntityWirelessEnergyTower#resolveTarget(BlockPos, List)}），
 * 重建时会按坐标复用原对象，从而保留进行中的传输进度。
 * </p>
 */
public class WirelessTowerTarget {

    /** 目标方块坐标（由无人机物品名称解析得到）。 */
    public final BlockPos pos;

    /** 剩余工作时间（tick）；{@code > 0} 表示正在传输。 */
    public int progress;

    /** 本次传输总时长（tick）= {@code ceil((5 + 距离/5) * 20)}。 */
    public int workTime;

    /** 最近一次实际注入目标的电量（EU），仅用于显示。 */
    public long lastInjected;

    /** 本次目标坐标处是否找不到可充电的机器（无效目标）。 */
    public boolean invalid;

    public WirelessTowerTarget(BlockPos pos) {
        this.pos = pos;
    }

    /** @return 是否正在进行一次传输。 */
    public boolean isTransferring() {
        return this.progress > 0;
    }

    /** 结束本次传输（仅在时间耗尽后调用）。 */
    public void reset() {
        this.progress = 0;
        this.workTime = 0;
    }
}
