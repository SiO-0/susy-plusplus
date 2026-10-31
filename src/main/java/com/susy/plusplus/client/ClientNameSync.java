package com.susy.plusplus.client;

import com.susy.plusplus.machine.MachineNameHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端：把服务端推来的机器名字应用到本地方块实体，并刷新悬浮名牌。
 *
 * <p>
 * 由 {@code PacketSyncMachineName.Handler} <b>按反射调用</b>（类名写死在那边），
 * 因此本类只会在客户端被加载。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ClientNameSync {

    private ClientNameSync() {
    }

    /** 由网络线程调用：切回客户端主线程再改方块实体。 */
    public static void apply(final int x, final int y, final int z, final String name) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        mc.addScheduledTask(new Runnable() {

            @Override
            public void run() {
                Minecraft client = Minecraft.getMinecraft();
                if (client.world == null) {
                    return;
                }
                MachineNameHelper.applyNameOnClient(client.world, new BlockPos(x, y, z), name);
            }
        });
    }
}
