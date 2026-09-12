package com.susy.plusplus.item;

import com.susy.plusplus.waterproof.IWaterproofMachine;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.items.metaitem.stats.IItemDurabilityManager;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GradientUtil;
import gregtech.common.items.MetaItems;
import gregtech.common.items.behaviors.AbstractUsableBehaviour;
import gregtech.core.sound.GTSoundEvents;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.Pair;

import java.awt.Color;
import java.util.List;

/**
 * 防水喷漆的行为，直接继承 GT 原版喷漆罐所使用的
 * {@link AbstractUsableBehaviour}（“容量 / 剩余次数”实现完全一致，NBT 键为
 * {@code GT.UsesLeft}）。
 *
 * <p>
 * 容量 300 次，单次消耗 1。剩余次数耗尽时替换为 GT 原版空喷漆罐
 * {@code MetaItems.SPRAY_EMPTY}（复用原版容器逻辑）。
 * </p>
 *
 * <p>
 * 注意：本类使用 Java 8 语法（本工程 {@code use_modern_java_syntax=false}）。
 * </p>
 */
public class WaterproofSprayBehaviour extends AbstractUsableBehaviour implements IItemDurabilityManager {

    /** 容量（使用次数）。 */
    public static final int CAPACITY = 300;

    /** 与防水漆液一致的颜色（0x2E6FA3），用于耐久条渐变。 */
    public static final int SPRAY_COLOR = 0x2E6FA3;

    private final Pair<Color, Color> durabilityBarColors;

    public WaterproofSprayBehaviour() {
        super(CAPACITY);
        this.durabilityBarColors = GradientUtil.getGradient(SPRAY_COLOR, 10);
    }

    /**
     * 从 ItemStack 上取出本行为实例（与 GT {@code ColorSprayBehaviour.getBehavior} 等价的写法）。
     *
     * @return 行为实例；若该物品不是我们的 MetaItem 则返回 {@code null}
     */
    public static WaterproofSprayBehaviour getBehaviour(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MetaItem)) {
            return null;
        }
        MetaItem<?> meta = (MetaItem<?>) stack.getItem();
        for (IItemBehaviour behaviour : meta.getBehaviours(stack)) {
            if (behaviour instanceof WaterproofSprayBehaviour) {
                return (WaterproofSprayBehaviour) behaviour;
            }
        }
        return null;
    }

    /** 剩余次数耗尽后被替换成的空容器（GT 原版空喷漆罐）。 */
    public static ItemStack emptyStack() {
        MetaItem<?>.MetaValueItem empty = MetaItems.SPRAY_EMPTY;
        return empty == null ? ItemStack.EMPTY : empty.getStackForm();
    }

    /** 消耗 1 次使用次数。 */
    public void consume(EntityPlayer player, EnumHand hand, ItemStack stack) {
        useItemDurability(player, hand, stack, emptyStack());
    }

    /** 播放喷漆音效（服务端调用会广播给客户端）。 */
    public static void playSpraySound(EntityPlayer player, World world) {
        world.playSound(null, player.posX, player.posY, player.posZ, GTSoundEvents.SPRAY_CAN_TOOL,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        lines.add(I18n.format("susyplusplus.tooltip.waterproof_spray_can.right_click"));
        lines.add(I18n.format("susyplusplus.tooltip.waterproof_spray_can.offhand"));
        lines.add(I18n.format("susyplusplus.tooltip.waterproof_spray_can.uses", getUsesLeft(itemStack)));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack itemStack) {
        return (double) getUsesLeft(itemStack) / (double) totalUses;
    }

    @Override
    public Pair<Color, Color> getDurabilityColorsForDisplay(ItemStack itemStack) {
        return durabilityBarColors;
    }

    @Override
    public boolean doDamagedStateColors(ItemStack itemStack) {
        return false;
    }

    /**
     * 在给定位置对机器施加防水（服务端）。
     *
     * @return 是否成功设置了防水（目标非机器 / 已是防水 / 客户端 均返回 {@code false}）
     */
    public static boolean applyWaterproofAt(World world, BlockPos pos) {
        if (world == null || world.isRemote || pos == null) {
            return false;
        }
        MetaTileEntity mte = WaterproofHelper.getMachine(world, pos);
        if (!(mte instanceof IWaterproofMachine)) {
            return false;
        }
        IWaterproofMachine machine = (IWaterproofMachine) mte;
        if (machine.isWaterproof()) {
            return false;
        }
        machine.setWaterproof(true);
        return true;
    }
}
