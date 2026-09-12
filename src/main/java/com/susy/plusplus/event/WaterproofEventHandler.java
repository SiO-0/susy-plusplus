package com.susy.plusplus.event;

import com.susy.plusplus.Tags;
import com.susy.plusplus.item.WaterproofSprayBehaviour;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 防水喷漆的交互事件处理。
 *
 * <p>
 * 全部逻辑在服务端执行；客户端通过 Forge 默认同步机制看到结果
 * （防水状态本身存在机器 NBT 中）。
 * </p>
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WaterproofEventHandler {

    private WaterproofEventHandler() {
    }

    /**
     * 右键已放置的机器：设为防水。
     *
     * <p>
     * 仅在手持“防水喷漆”时生效，因此不会影响 GT 原有的扳手 / GUI / 旋转等交互。
     * </p>
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) {
            return;
        }
        EnumHand hand = event.getHand();
        ItemStack stack = player.getHeldItem(hand);
        WaterproofSprayBehaviour behaviour = WaterproofSprayBehaviour.getBehaviour(stack);
        if (behaviour == null) {
            return;
        }
        if (!WaterproofSprayBehaviour.applyWaterproofAt(world, event.getPos())) {
            return;
        }
        behaviour.consume(player, hand, stack);
        WaterproofSprayBehaviour.playSpraySound(player, world);
        // 阻止默认的方块/物品交互（例如打开机器 GUI）。
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }

    /**
     * 副手持有防水喷漆、主手放置机器后：自动设为防水。
     *
     * <p>
     * GT 原版本身就有“副手喷漆罐给放置的机器上色”的机制
     * （{@code BlockMachine#placeBlockAt}）；此处用 Forge 的
     * {@link BlockEvent.EntityPlaceEvent} 实现等价的自动防水。
     * </p>
     */
    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        World world = event.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        ItemStack offhand = player.getHeldItemOffhand();
        WaterproofSprayBehaviour behaviour = WaterproofSprayBehaviour.getBehaviour(offhand);
        if (behaviour == null) {
            return;
        }
        if (!WaterproofSprayBehaviour.applyWaterproofAt(world, event.getPos())) {
            return;
        }
        behaviour.consume(player, EnumHand.OFF_HAND, offhand);
        WaterproofSprayBehaviour.playSpraySound(player, world);
    }
}
