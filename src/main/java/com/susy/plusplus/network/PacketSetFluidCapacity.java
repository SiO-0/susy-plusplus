package com.susy.plusplus.network;

import com.susy.plusplus.machine.FluidCapacityHelper;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

/**
 * 客户端 → 服务端：把「容量限制（仅流体）」界面里填的每个流体槽位容量写入机器。
 *
 * <p>
 * 为什么不用 ModularUI 的同步值：编译期的 3.0.4 与运行期的 3.1.6 在
 * {@code PanelSyncManager#syncValue} 的重载上有过差异（同项目里
 * {@code MachineFaceUI} 已经踩过 {@code NoSuchMethodError} 的坑），
 * 所以这里沿用"自己发包"的方式，协议完全由本模组掌控。
 * </p>
 */
public class PacketSetFluidCapacity implements IMessage {

    private BlockPos pos;
    private int[] values;

    /** netty 反序列化需要。 */
    public PacketSetFluidCapacity() {
    }

    public PacketSetFluidCapacity(BlockPos pos, int[] values) {
        this.pos = pos;
        this.values = values;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        int length = Math.max(0, Math.min(256, buf.readInt()));
        this.values = new int[length];
        for (int i = 0; i < length; i++) {
            this.values[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        int length = values == null ? 0 : values.length;
        buf.writeInt(length);
        for (int i = 0; i < length; i++) {
            buf.writeInt(values[i]);
        }
    }

    public static class Handler implements IMessageHandler<PacketSetFluidCapacity, IMessage> {

        @Override
        public IMessage onMessage(PacketSetFluidCapacity message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(new Runnable() {

                @Override
                public void run() {
                    handle(player, message);
                }
            });
            return null;
        }

        private static void handle(EntityPlayerMP player, PacketSetFluidCapacity message) {
            if (player == null || message.pos == null || message.values == null) {
                return;
            }
            MetaTileEntity mte = WaterproofHelper.getMachine(player.world, message.pos);
            if (mte == null) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.no_machine"), true);
                return;
            }
            if (FluidCapacityHelper.tanks(mte).isEmpty()) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.no_fluid_tank"), true);
                return;
            }
            FluidCapacityHelper.applyCapacities(mte, message.values);
            player.sendStatusMessage(new TextComponentTranslation(
                    "susyplusplus.message.configurator.capacity_set"), true);
        }
    }
}
