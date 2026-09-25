package com.susy.plusplus.event;

import com.susy.plusplus.Tags;
import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.item.trolley.TrolleyHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 手推车与机器的交互（全部在服务端执行）。
 *
 * <table border="1">
 * <caption>交互表</caption>
 * <tr>
 * <th>操作</th>
 * <th>行为</th>
 * </tr>
 * <tr>
 * <td>Shift + 右键机器</td>
 * <td>搬起机器（零掉落），提示"已搬起机器"</td>
 * </tr>
 * <tr>
 * <td>右键</td>
 * <td>放下机器（仅在车上有机器时；空车放行，保留 GT 右键开 GUI）</td>
 * </tr>
 * </table>
 *
 * <h2>与 GT 工具的冲突（结论：无实质冲突）</h2>
 *
 * <p>
 * 已核对 GT 2.8.x 源码：{@code MetaTileEntity#onWrenchClick} <b>只旋转朝向、不拆机器</b>
 * （拆机器靠左键破坏）；撬棍只拆封面；软锤/硬锤/螺丝刀/剪线钳都要求手持对应工具
 *（{@code BlockMachine#onBlockActivated} 先用 {@code getToolClasses()} 分流）。
 * 手推车不声明任何工具类，因此永远走 {@code MetaTileEntity#onRightClick}，
 * 与上述工具互不干扰。
 * </p>
 *
 * <p>
 * 只有在真正执行了动作（{@link TrolleyHelper} 返回 {@code SUCCESS}）时才取消事件，
 * 避免影响 GT / 原版的默认交互（开 GUI、封面、命名牌等）。
 * </p>
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class TrolleyEventHandler {

    private TrolleyEventHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 另一个模组/本模组的其它物品（如配置器）已经处理过 → 不再插手
        if (event.isCanceled()) {
            return;
        }
        World world = event.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        if (SuMetaItems.TROLLEY == null) {
            return;
        }

        ItemStack trolley = findTrolley(player, event.getHand());
        if (trolley.isEmpty()) {
            return;
        }

        EnumActionResult result = player.isSneaking()
                ? TrolleyHelper.pickUp(world, event.getPos(), trolley, player)
                : TrolleyHelper.place(world, event.getPos(), event.getFace(), trolley, player);

        if (result == EnumActionResult.SUCCESS) {
            // 阻止 GT / 原版的默认交互（打开机器 GUI、封面、命名牌等）
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }
    }

    /** 主手优先、副手兜底（玩家可能把手推车拿在副手）。 */
    private static ItemStack findTrolley(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (isTrolley(held)) {
            return held;
        }
        ItemStack main = player.getHeldItemMainhand();
        if (isTrolley(main)) {
            return main;
        }
        ItemStack off = player.getHeldItemOffhand();
        return isTrolley(off) ? off : ItemStack.EMPTY;
    }

    private static boolean isTrolley(ItemStack stack) {
        return stack != null && !stack.isEmpty() && SuMetaItems.TROLLEY.isItemEqual(stack);
    }
}
