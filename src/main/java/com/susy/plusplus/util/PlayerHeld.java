package com.susy.plusplus.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

/**
 * 玩家手部 / 潜行状态访问器。
 *
 * <h3>为什么需要这个类</h3>
 *
 * <p>
 * {@code getHeldItem(EnumHand)}、{@code getHeldItemMainhand()}、
 * {@code getHeldItemOffhand()} 都声明在 {@link EntityLivingBase} 上，
 * {@code isSneaking()} 声明在 {@link Entity} 上；{@link EntityPlayer} 只是继承者。
 * </p>
 *
 * <p>
 * 本项目的重混淆工具（RFG reobf）<b>不会沿着类继承链查找映射</b>：
 * 如果接收者的静态类型写成 {@code EntityPlayer}，这些调用会被原样保留为 MCP 名字，
 * 运行期直接抛 {@code NoSuchMethodError}（例如
 * {@code EntityPlayer.getHeldItemMainhand()}、{@code EntityPlayerSP.getHeldItemMainhand()}）。
 * 已实际在整合包里踩到并确认（crash-reports 2026-10-18）。
 * </p>
 *
 * <p>
 * 解决办法：<b>把接收者先转成真正声明该方法的类型</b>
 * （{@code EntityLivingBase} / {@code Entity}），这样 reobf 一定能找到映射。
 * 全项目统一走本类，避免再出现同类问题。
 * </p>
 */
public final class PlayerHeld {

    private PlayerHeld() {
    }

    /** 主手物品。 */
    public static ItemStack main(EntityPlayer player) {
        return ((EntityLivingBase) player).getHeldItemMainhand();
    }

    /** 副手物品。 */
    public static ItemStack off(EntityPlayer player) {
        return ((EntityLivingBase) player).getHeldItemOffhand();
    }

    /** 指定手物品。 */
    public static ItemStack get(EntityPlayer player, EnumHand hand) {
        return ((EntityLivingBase) player).getHeldItem(hand);
    }

    /** 玩家是否潜行。 */
    public static boolean sneaking(EntityPlayer player) {
        return ((Entity) player).isSneaking();
    }
}
