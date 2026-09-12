package com.susy.plusplus.item.battery;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.items.metaitem.stats.IItemCapabilityProvider;
import gregtech.api.items.metaitem.stats.IItemComponent;
import gregtech.api.util.TextFormattingUtil;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.List;

/**
 * 电池盒的核心组件（MetaItem 组件）。
 *
 * <p>
 * 同时承担：能力提供（{@link IItemCapabilityProvider}）、物品行为（右键 / 每 tick / tooltip），
 * 以及物品 UI（{@link ItemUIFactory}）。
 * </p>
 *
 * <p>
 * <b>数据存哪里：</b>盒内 4 格电池保存在<b>电池盒物品自身的 NBT tag</b> 里
 * （{@link BatteryCaseInventory#NBT_KEY}），而不是 Forge 的 {@code ForgeCaps}。
 * 原因见 {@link BatteryCaseInventory} 的类注释 ——
 * 1.12.2 的 {@code Item.getNBTShareTag} 只返回 {@code stack.getTagCompound()}，
 * 不含 {@code ForgeCaps}，把数据放 ForgeCaps 会在 share tag 同步时丢失。
 * </p>
 *
 * <p>
 * <b>UI 使用的是 GT 实际发布包中的旧版 GUI API</b>
 * （{@code gregtech.api.gui.ModularUI} + {@link PlayerInventoryHolder}），
 * 而不是 master 源码里的 {@code gregtech.api.mui.*} —— 后者在 2.8.x 发布版中并不存在。
 * </p>
 *
 * <p>
 * 本类使用 Java 8 语法（本工程 {@code use_modern_java_syntax=false}），
 * 且不使用 {@code org.jetbrains.annotations}（本工程
 * {@code use_jetbrains_annotations=false}）。
 * </p>
 */
public class BatteryCaseBehaviour implements IItemComponent, IItemCapabilityProvider, IItemBehaviour, ItemUIFactory {

    /** NBT 键：释能模式（与 GT 电池 {@code ElectricStats} 一致）。 */
    public static final String DISCHARGE_MODE_KEY = "DischargeMode";

    /** UI 尺寸：GT 标准 176x166，玩家背包固定在 y=84。 */
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int PLAYER_INV_Y = 84;

    // ------------------------------------------------------------------ 能力

    @Override
    public ICapabilityProvider createProvider(ItemStack itemStack) {
        // 库存必须绑定到具体的 ItemStack，才能把数据读写进它的 NBT。
        return new BatteryCaseProvider(itemStack);
    }

    /** 从 ItemStack 取回电池盒库存（通过 ITEM_HANDLER 能力）。 */
    public static BatteryCaseInventory getInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Object handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof BatteryCaseInventory) {
            return (BatteryCaseInventory) handler;
        }
        return null;
    }

    // ------------------------------------------------------------------ 右键

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            // 潜行右键：切换释能模式（与 GT 电池 ElectricStats 行为一致）
            if (!world.isRemote) {
                boolean enabled = isInDischargeMode(stack);
                setInDischargeMode(stack, !enabled);
                player.sendStatusMessage(new TextComponentTranslation(
                        "metaitem.electric.discharge_mode." + (enabled ? "disabled" : "enabled")), true);
            }
            return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
        }
        // 普通右键：打开 UI（服务端打开，客户端显示）
        if (!world.isRemote) {
            PlayerInventoryHolder.openHandItemUI(player, hand);
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    // ------------------------------------------------------------------ 释能模式

    public static boolean isInDischargeMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(DISCHARGE_MODE_KEY);
    }

    /**
     * 设置释能模式。
     *
     * <p>
     * <b>注意：</b>这里<b>不会</b>在 {@code tag} 变空时把整个 tag 置 null ——
     * 盒内 4 格电池的库存（{@link BatteryCaseInventory#NBT_KEY}）就存在同一个 tag 里，
     * 置 null 会把电池一起清掉。
     * </p>
     */
    public static void setInDischargeMode(ItemStack stack, boolean enabled) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            if (!enabled) {
                return;
            }
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(DISCHARGE_MODE_KEY, enabled);
    }

    /**
     * 释能模式下：把电池盒电量自动充给背包（装了 Baubles 则并入饰品栏）中其它可充电物品。
     * 与 GT {@code ElectricStats#onUpdate} 相同，仅把能量来源换成电池盒。
     */
    @Override
    public void onUpdate(ItemStack caseStack, Entity entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (!isInDischargeMode(caseStack)) {
            return;
        }
        IElectricItem source = caseStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (source == null || source.getCharge() <= 0L) {
            return;
        }

        IInventory inventory = player.inventory;
        if (Loader.isModLoaded("baubles")) {
            inventory = BatteryCaseBaubles.wrapInventory(player);
        }

        long transferLimit = source.getTransferLimit();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty() || slotStack == caseStack) {
                continue;
            }
            IElectricItem target = slotStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
            if (target == null || target.canProvideChargeExternally()) {
                continue;
            }
            long charged = chargeElectricItem(transferLimit, source, target);
            if (charged > 0L) {
                transferLimit -= charged;
                if (transferLimit <= 0L) {
                    break;
                }
            }
        }
    }

    /** 与 GT {@code ElectricStats#chargeElectricItem} 相同的两阶段充能写法。 */
    private static long chargeElectricItem(long maxDischargeAmount, IElectricItem source, IElectricItem target) {
        long maxDischarged = source.discharge(maxDischargeAmount, source.getTier(), false, false, true);
        long maxReceived = target.charge(maxDischarged, source.getTier(), false, true);
        if (maxReceived > 0L) {
            long resultDischarged = source.discharge(maxReceived, source.getTier(), false, true, false);
            target.charge(resultDischarged, source.getTier(), false, false);
            return resultDischarged;
        }
        return 0L;
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        BatteryCaseInventory inventory = getInventory(itemStack);
        long charge = inventory == null ? 0L : inventory.getTotalCharge();
        long maxCharge = inventory == null ? 0L : inventory.getTotalMaxCharge();
        int tier = inventory == null ? -1 : inventory.getTier();
        int count = inventory == null ? 0 : inventory.getBatteryCount();

        lines.add(I18n.format("susyplusplus.tooltip.battery_case.charge",
                TextFormattingUtil.formatNumbers(charge), TextFormattingUtil.formatNumbers(maxCharge)));
        if (tier >= 0 && tier < GTValues.VN.length) {
            lines.add(I18n.format("susyplusplus.tooltip.battery_case.tier", GTValues.VNF[tier]));
        } else {
            lines.add(I18n.format("susyplusplus.tooltip.battery_case.tier_empty"));
        }
        lines.add(I18n.format(isInDischargeMode(itemStack) ? "metaitem.electric.discharge_mode.enabled"
                : "metaitem.electric.discharge_mode.disabled"));
        lines.add(I18n.format("susyplusplus.tooltip.battery_case.count", count, BatteryCaseInventory.SIZE));
        lines.add(I18n.format("susyplusplus.tooltip.battery_case.open_ui"));
        lines.add(I18n.format("susyplusplus.tooltip.battery_case.toggle_mode"));
        lines.add(I18n.format("susyplusplus.tooltip.battery_case.same_tier"));
    }

    // ------------------------------------------------------------------ UI

    /**
     * 构建物品 UI：标题 + 4 个电池槽（1 行 4 列）+ 玩家背包。
     *
     * <p>
     * 槽位使用 {@link SlotWidget} 绑定到物品的 {@link BatteryCaseInventory}，
     * 放置限制由 {@link BatteryCaseInventory#isItemValid(int, ItemStack)} 保证（仅同 tier
     * 电池）。
     * 槽位变动会写进电池盒物品的 NBT（{@link BatteryCaseInventory#onContentsChanged(int)}），
     * 并通过 {@code holder.markAsDirty()} 让服务端把新 NBT 同步给客户端。
     * </p>
     */
    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer player) {
        ItemStack stack = holder.getCurrentItem();
        BatteryCaseInventory found = getInventory(stack);
        final BatteryCaseInventory inventory = found == null ? new BatteryCaseInventory(stack) : found;

        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, GUI_WIDTH, GUI_HEIGHT)
                .label(8, 6, stack.getDisplayName(), 0x404040);

        // 4 格排成一行，水平居中：(176 - 4 * 18) / 2 = 52
        for (int i = 0; i < inventory.getSlots(); i++) {
            SlotWidget slot = new SlotWidget(inventory, i, 52 + i * 18, 24)
                    .setBackgroundTexture(GuiTextures.SLOT)
                    .setChangeListener(new Runnable() {

                        @Override
                        public void run() {
                            holder.markAsDirty();
                        }
                    });
            builder.widget(slot);
        }

        builder.bindPlayerInventory(player.inventory, PLAYER_INV_Y);
        return builder.build(holder, player);
    }

    // ------------------------------------------------------------------ 能力提供者

    /**
     * 单个 ItemStack 的能力载体：同时提供
     * {@code CAPABILITY_ELECTRIC_ITEM}（电池盒能量视图）与 {@code ITEM_HANDLER_CAPABILITY}（4
     * 格库存）。
     *
     * <p>
     * <b>刻意不实现 {@code INBTSerializable}</b>：数据不进 {@code ForgeCaps}，而是由
     * {@link BatteryCaseInventory} 直接读写该 ItemStack 的 NBT tag。
     * 这样即使物品只经由 share tag 同步（1.12.2 的 share tag 只有 tagCompound），
     * 盒内电池也不会丢。
     * </p>
     */
    public static class BatteryCaseProvider implements ICapabilityProvider {

        private final BatteryCaseInventory inventory;
        private final BatteryCaseEnergyStorage energy;

        public BatteryCaseProvider(ItemStack itemStack) {
            this.inventory = new BatteryCaseInventory(itemStack);
            this.energy = new BatteryCaseEnergyStorage(inventory);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM ||
                    capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            if (capability == GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM) {
                return GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM.cast(energy);
            }
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
            }
            return null;
        }
    }
}
