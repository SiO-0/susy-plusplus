package com.susy.plusplus.waterproof;

/**
 * 防水状态查询接口。
 *
 * <p>
 * 由 Mixin 注入到 {@code gregtech.api.metatileentity.MetaTileEntity}，
 * 因此任意 GT 机器的 MetaTileEntity 都可通过 {@code instanceof IWaterproofMachine} 访问。
 * </p>
 *
 * <p>
 * 该状态被以下逻辑读取：
 * </p>
 * <ul>
 * <li>遇水/地形爆炸判定（Mixin 覆写 {@code getIsWeatherOrTerrainResistant()}）</li>
 * <li>The One Probe 显示</li>
 * <li>防水喷漆右键设置</li>
 * <li>副手放置自动设置</li>
 * </ul>
 */
public interface IWaterproofMachine {

    /** @return 该机器当前是否为防水状态 */
    boolean isWaterproof();

    /** 设置防水状态（会被持久化到 NBT）。 */
    void setWaterproof(boolean waterproof);
}
