package com.susy.plusplus.network;

import com.susy.plusplus.gui.ConfiguratorMainUI;
import com.susy.plusplus.item.SuMetaItems;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

/**
 * 客户端 → 服务端：请求打开配置器主界面（Shift+V）。
 *
 * <p>
 * 无负载：服务端收到后自己校验「主手是否拿着配置器」，通过才打开界面。
 * </p>
 */
public class PacketOpenConfigurator implements IMessage {

    public PacketOpenConfigurator() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无负载
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无负载
    }

    public static class Handler implements IMessageHandler<PacketOpenConfigurator, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenConfigurator message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            // 统一回到服务端主线程处理（SimpleNetworkWrapper 的回调线程不保证是主线程）。
            player.getServer().addScheduledTask(new Runnable() {

                @Override
                public void run() {
                    handle(player);
                }
            });
            return null;
        }

        private static void handle(EntityPlayerMP player) {
            if (player == null || SuMetaItems.CONFIGURATOR == null) {
                return;
            }
            ItemStack held = player.getHeldItemMainhand();
            if (!isConfigurator(held)) {
                held = player.getHeldItemOffhand();
            }
            if (!isConfigurator(held)) {
                return;
            }
            // 主界面自己会从玩家手上读回配置器（以及它的当前模式）
            ConfiguratorMainUI.open(player);
        }

        private static boolean isConfigurator(ItemStack stack) {
            return stack != null && !stack.isEmpty() && SuMetaItems.CONFIGURATOR.isItemEqual(stack);
        }
    }
}
