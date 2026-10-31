package com.susy.plusplus.network;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

/**
 * 服务端 → 客户端：把机器的最新自定义名字推给发起重命名的玩家。
 *
 * <h3>为什么需要这个包</h3>
 *
 * <p>
 * GT 的 {@code MetaTileEntityHolder#setCustomName(String)} 在服务端只
 * {@code markAsDirty()}；而客户端侧即使靠区块更新把 {@code CustomName} 写进了字段，
 * 头顶的悬浮名牌（{@code GTNameTagParticle}）也不会跟着变 ——
 * {@code updateNameTagParticle()} 里只有在 {@code nameTagParticle == null} 时才新建粒子，
 * 已存在的粒子被原样保留（javap 核实 2.8.7 的字节码）。
 * </p>
 *
 * <p>
 * 所以改名/清名后由服务端主动发这个包，客户端收到后在主线程调用
 * {@code MachineNameHelper#applyNameOnClient}：先用空名让 GT 回收旧粒子，
 * 再写入新名字触发新建粒子，从而<b>立刻</b>看到新名字。
 * </p>
 */
public class PacketSyncMachineName implements IMessage {

    private static final int MAX_NAME_LENGTH = 128;

    private BlockPos pos;
    private String name;

    /** netty 反序列化需要。 */
    public PacketSyncMachineName() {
    }

    public PacketSyncMachineName(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name == null ? "" : name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        // ⚠ 长度必须是**字节数**（UTF-8 中文 3 字节/字），否则中文会被截断成乱码。
        int byteLength = Math.max(0, Math.min(MAX_NAME_LENGTH * 4, buf.readInt()));
        byte[] bytes = new byte[byteLength];
        buf.readBytes(bytes);
        this.name = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BlockPos p = pos == null ? BlockPos.ORIGIN : pos;
        buf.writeInt(p.getX());
        buf.writeInt(p.getY());
        buf.writeInt(p.getZ());
        String value = name == null ? "" : name;
        if (value.length() > MAX_NAME_LENGTH) {
            value = value.substring(0, MAX_NAME_LENGTH);
        }
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static class Handler implements IMessageHandler<PacketSyncMachineName, IMessage> {

        /**
         * 客户端处理类名（用反射调用）。
         *
         * <p>
         * 直接把 {@code Minecraft} / 客户端类写进方法体会让该类在<b>服务端</b>
         * 加载/校验时也被链接（本模组已经在 TOP 集成上踩过
         * {@code NoClassDefFoundError} 的坑），所以这里按名反射。
         * </p>
         */
        private static final String CLIENT_CLASS = "com.susy.plusplus.client.ClientNameSync";

        @Override
        public IMessage onMessage(PacketSyncMachineName message, MessageContext ctx) {
            if (message.pos == null) {
                return null;
            }
            try {
                Class<?> cls = Class.forName(CLIENT_CLASS);
                cls.getMethod("apply", int.class, int.class, int.class, String.class)
                        .invoke(null, message.pos.getX(), message.pos.getY(), message.pos.getZ(),
                                message.name == null ? "" : message.name);
            } catch (Throwable ignored) {
                // 只在客户端存在；服务端/异常情况下静默忽略
            }
            return null;
        }
    }
}
