package com.susy.plusplus.network;

import com.susy.plusplus.machine.MachineNameHelper;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

/**
 * 客户端 → 服务端：把「重命名」界面里输入的名字写入机器（空串 = 清除 CustomName）。
 *
 * <p>
 * 机器名保存在 {@code MetaTileEntityHolder#customName}（NBT {@code CustomName}），
 * 只能由服务端权威写入；界面本身是 ModularUI（客户端也有一份），
 * 所以按钮动作在客户端发包、服务端落盘。
 * </p>
 *
 * <p>
 * 写入成功后服务端会立刻回一条 {@link PacketSyncMachineName}，
 * 让<b>发起者本人的客户端</b>马上刷新悬浮名牌（GT 的
 * {@code MetaTileEntityHolder#updateNameTagParticle()} 只在
 * {@code nameTagParticle == null} 时新建，已有粒子不会随字段变化更新）。
 * </p>
 */
public class PacketRenameMachine implements IMessage {

    /** 名字长度上限（与 GT 的 NBT 字符串一致地宽松，但防止恶意超长包）。 */
    private static final int MAX_NAME_LENGTH = 128;

    private BlockPos pos;
    private String name;

    /** netty 反序列化需要。 */
    public PacketRenameMachine() {
    }

    public PacketRenameMachine(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name == null ? "" : name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        // ⚠ 长度必须是**字节数**：UTF-8 下一个中文字符占 3 字节，
        // 之前按"字符数"读会截断，中文因此变成乱码。
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

    public static class Handler implements IMessageHandler<PacketRenameMachine, IMessage> {

        @Override
        public IMessage onMessage(PacketRenameMachine message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            // 统一回到服务端主线程处理（SimpleNetworkWrapper 的回调线程不保证是主线程）
            player.getServer().addScheduledTask(new Runnable() {

                @Override
                public void run() {
                    handle(player, message);
                }
            });
            return null;
        }

        private static void handle(EntityPlayerMP player, PacketRenameMachine message) {
            if (player == null || message.pos == null) {
                return;
            }
            // ⚠ 这些方法/字段都声明在 Entity 上，必须把接收者转成 Entity，
            // 否则 RFG reobf 不会映射（见 PlayerHeld 的说明）。
            final Entity entity = player;
            final net.minecraft.world.World world = entity.getEntityWorld();

            // 距离校验：防止客户端伪造远处的坐标
            if (entity.getDistanceSq(message.pos.getX() + 0.5D, message.pos.getY() + 0.5D,
                    message.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            MetaTileEntity mte = WaterproofHelper.getMachine(world, message.pos);
            if (mte == null) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.no_machine"), true);
                return;
            }

            String name = message.name == null ? "" : message.name.trim();
            if (!MachineNameHelper.setName(world, message.pos, name)) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.rename_failed"), true);
                return;
            }

            // 立刻回同步包：让发起者的客户端重建悬浮名牌（见类注释）
            SuNetwork.sendTo(new PacketSyncMachineName(message.pos, name), player);

            if (name.isEmpty()) {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.renamed_cleared"), true);
            } else {
                player.sendStatusMessage(new TextComponentTranslation(
                        "susyplusplus.message.configurator.renamed", name), true);
            }
        }
    }
}
