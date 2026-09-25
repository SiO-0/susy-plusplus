package com.susy.plusplus.event;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.Tags;
import com.susy.plusplus.gui.MachineFaceUI;
import com.susy.plusplus.gui.SuGuiFactories;
import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.item.configurator.ConfiguratorData;
import com.susy.plusplus.item.configurator.ConfiguratorMode;
import com.susy.plusplus.item.configurator.MachineConfig;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 配置器与机器的交互（全部在服务端执行）。
 *
 * <table border="1">
 * <caption>交互表</caption>
 * <tr>
 * <th>模式</th>
 * <th>Shift + 右键机器</th>
 * <th>普通右键机器</th>
 * </tr>
 * <tr>
 * <td>1 修改机器输出面</td>
 * <td>打开「输出面」界面（仅 {@code SimpleMachineMetaTileEntity}）</td>
 * <td>—</td>
 * </tr>
 * <tr>
 * <td>2 复制机器配置</td>
 * <td>把机器配置复制进配置器（提示"配置已复制"）</td>
 * <td>把配置器里的配置写进机器（提示"配置已粘贴"）</td>
 * </tr>
 * <tr>
 * <td>3 机器工具箱</td>
 * <td>—</td>
 * <td>打开「机器工具箱」界面</td>
 * </tr>
 * <tr>
 * <td>0 无</td>
 * <td>—</td>
 * <td>—</td>
 * </tr>
 * </table>
 *
 * <p>
 * 只有真正执行了动作时才取消事件（避免影响 GT 自己的扳手 / GUI / 旋转等交互）。
 * </p>
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ConfiguratorEventHandler {

    private ConfiguratorEventHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 已被其它处理器（例如手推车）处理过的右键不再插手
        if (event.isCanceled()) {
            return;
        }
        World world = event.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !(player instanceof EntityPlayerMP)) {
            return;
        }
        if (SuMetaItems.CONFIGURATOR == null) {
            return;
        }

        // 主手优先，副手兜底（玩家可能把配置器拿在副手）
        ItemStack stack = player.getHeldItem(event.getHand());
        if (!isConfigurator(stack)) {
            stack = player.getHeldItemMainhand();
        }
        if (!isConfigurator(stack)) {
            stack = player.getHeldItemOffhand();
        }
        if (!isConfigurator(stack)) {
            return;
        }

        ConfiguratorMode mode = ConfiguratorMode.get(stack);
        if (mode == ConfiguratorMode.NONE) {
            return;
        }

        MetaTileEntity mte = WaterproofHelper.getMachine(world, event.getPos());
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        boolean acted = false;
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Configurator right-click: pos={}, sneaking={}, mode={}, machine={}",
                event.getPos(), player.isSneaking(), mode, mte == null ? "none" : mte.getClass().getSimpleName());

        if (player.isSneaking()) {
            switch (mode) {
                case MODIFY_OUTPUT:
                    if (mte instanceof SimpleMachineMetaTileEntity) {
                        SusyPlusPlus.LOGGER.info(
                                "[SusyPlusPlus] Configurator: opening face UI at {} (factory={})",
                                event.getPos(), MachineFaceUI.FACTORY_NAME);
                        SuGuiFactories.openFaceUI(serverPlayer, event.getPos());
                    } else {
                        SusyPlusPlus.LOGGER.info(
                                "[SusyPlusPlus] Configurator: unsupported machine for output-face editing: {}",
                                mte == null ? "none" : mte.getClass().getName());
                        message(player, "susyplusplus.message.configurator.unsupported");
                    }
                    acted = true;
                    break;
                case COPY_CONFIG:
                    if (mte != null) {
                        ConfiguratorData.setCopiedConfig(stack, MachineConfig.capture(mte));
                        message(player, "susyplusplus.message.configurator.copied");
                    } else {
                        message(player, "susyplusplus.message.configurator.no_machine");
                    }
                    acted = true;
                    break;
                default:
                    break;
            }
        } else {
            switch (mode) {
                case COPY_CONFIG:
                    if (mte == null) {
                        message(player, "susyplusplus.message.configurator.no_machine");
                    } else {
                        MachineConfig config = ConfiguratorData.getCopiedConfig(stack);
                        if (config == null) {
                            message(player, "susyplusplus.message.configurator.no_copy");
                        } else {
                            config.apply(mte);
                            message(player, "susyplusplus.message.configurator.pasted");
                        }
                    }
                    acted = true;
                    break;
                case MACHINE_TOOLBOX:
                    if (mte != null) {
                        SuGuiFactories.openToolbox(serverPlayer, event.getPos());
                        acted = true;
                    }
                    break;
                default:
                    break;
            }
        }

        if (acted) {
            // 阻止 GT / 原版的默认交互（例如打开机器 GUI、扳手旋转）
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        }
    }

    private static boolean isConfigurator(ItemStack stack) {
        return stack != null && !stack.isEmpty() && SuMetaItems.CONFIGURATOR.isItemEqual(stack);
    }

    private static void message(EntityPlayer player, String langKey) {
        player.sendStatusMessage(new TextComponentTranslation(langKey), true);
    }
}
