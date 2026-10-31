package com.susy.plusplus.pipe.steam;

import gregtech.api.pipenet.WorldPipeNet;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/**
 * 每个维度一份的蒸汽管道网络存档。
 *
 * <p>
 * 加载流程与 GT 的 {@code WorldFluidPipeNet#getWorldPipeNet} 完全一致
 * （已用 {@code javap -c} 核对 2.8.7 字节码）：
 * </p>
 *
 * <ol>
 * <li>{@code getDataID(基础名, world)} 得到带维度号的数据名；</li>
 * <li>{@code world.loadData(类, 数据名)}（SRG {@code func_72943_a}）；</li>
 * <li>取不到就 {@code new} 一个并 {@code world.setData(数据名, 实例)}（SRG {@code func_72823_a}）；</li>
 * <li>无论新旧都调用一次 {@code setWorldAndInit(world)} 绑定世界。</li>
 * </ol>
 */
public class WorldSteamPipeNet extends WorldPipeNet<SteamPipeProperties, SteamPipeNet> {

    /** 存档名基础串；{@code getDataID} 会再拼上维度。 */
    private static final String DATA_ID_BASE = "susyplusplus.steam_pipe_net";

    public WorldSteamPipeNet(String name) {
        super(name);
    }

    public static WorldSteamPipeNet get(World world) {
        String dataId = getDataID(DATA_ID_BASE, world);
        WorldSteamPipeNet net = (WorldSteamPipeNet) world.loadData(WorldSteamPipeNet.class, dataId);
        if (net == null) {
            net = new WorldSteamPipeNet(dataId);
            world.setData(dataId, net);
        }
        net.setWorldAndInit(world);
        return net;
    }

    @Override
    protected SteamPipeNet createNetInstance() {
        return new SteamPipeNet(this);
    }
}
