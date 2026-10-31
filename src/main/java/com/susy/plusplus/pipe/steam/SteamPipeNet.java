package com.susy.plusplus.pipe.steam;

import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 蒸汽管道的网络（连通性 / 节点数据 / 存档）。
 *
 * <p>
 * <b>注意：本网络不负责流体传输。</b> 流体的实际搬运在
 * {@link TileEntitySteamPipe} 里逐 tick 完成（见该类的说明），
 * 这里只做 GT {@code PipeNet} 要求的节点登记与 {@code NBT} 序列化。
 * </p>
 *
 * <p>
 * 之所以要自己写一个网络而不是复用 {@code FluidPipeNet}：后者的节点数据类型是
 * {@code FluidPipeProperties} 且与 GT 的流体管道方块/材质强绑定，
 * 无法挂在非材质管道上。
 * </p>
 */
public class SteamPipeNet extends PipeNet<SteamPipeProperties> {

    private static final String TAG_PRESSURE_BASE = "PressureBase";
    private static final String TAG_THROUGHPUT = "Throughput";

    public SteamPipeNet(WorldPipeNet<SteamPipeProperties, SteamPipeNet> worldData) {
        super(worldData);
    }

    @Override
    protected void writeNodeData(SteamPipeProperties nodeData, NBTTagCompound tag) {
        tag.setInteger(TAG_PRESSURE_BASE, nodeData.getPressureBaseThroughput());
        tag.setInteger(TAG_THROUGHPUT, nodeData.getThroughput());
    }

    @Override
    protected SteamPipeProperties readNodeData(NBTTagCompound tag) {
        return new SteamPipeProperties(
                tag.getInteger(TAG_PRESSURE_BASE),
                tag.getInteger(TAG_THROUGHPUT));
    }

    /**
     * 只有同一压力等级的节点才能连成一张网。
     *
     * <p>
     * {@code SteamPipeProperties#equals} 只比较压力等级，所以同一压力下
     * 不同粗细的管道可以互通（整条路径由最细的那根限速），
     * 低压与高压则永不同网。
     * </p>
     */
    @Override
    protected boolean areNodesCustomContactable(SteamPipeProperties selfData, SteamPipeProperties otherData,
                                                PipeNet<SteamPipeProperties> otherNet) {
        return selfData != null && selfData.equals(otherData);
    }
}
