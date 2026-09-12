package com.susy.plusplus.item.battery;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 电池盒内部 4 格库存。
 *
 * <p>
 * <b>数据直接存在电池盒物品自身的 NBT tag 里</b>（键 {@value #NBT_KEY}），
 * <b>而不是</b> Forge 的 {@code ForgeCaps} 能力数据里。
 * </p>
 *
 * <p>
 * 这是本类最重要的设计决定，原因来自 Forge 1.12.2 的实际实现
 * （见反编译源码 {@code build/rfg/minecraft-src/java/net/minecraft/item/Item.java}）：
 * </p>
 *
 * <pre>
 * public NBTTagCompound getNBTShareTag(ItemStack stack) {
 *     return stack.getTagCompound(); // 只有普通 tag，不含 ForgeCaps！
 * }
 * 
 * public void readNBTShareTag(ItemStack stack, NBTTagCompound nbt) {
 *     stack.setTagCompound(nbt); // 直接覆盖 tag
 * }
 * </pre>
 *
 * <p>
 * 而 {@code gregtech.api.items.metaitem.MetaItem} 并没有覆写 {@code getNBTShareTag}。
 * 也就是说：<b>凡是经由 share tag 同步的路径，{@code ForgeCaps} 都会丢失</b>，
 * 接收端的 ItemStack 会被重建且能力数据为空 —— 表现就是
 * "开关电池的释能模式后，UI 里的电池消失了"。
 * </p>
 *
 * <p>
 * 把库存放进普通 tag 后，它在<b>所有</b>路径下都随物品一起走：
 * </p>
 * <ul>
 * <li>{@code Item.getNBTShareTag} / {@code readNBTShareTag}（share tag ==
 * tagCompound）</li>
 * <li>{@code ItemStack.writeToNBT} / {@code ItemStack(NBTTagCompound)}（"tag"
 * 键）</li>
 * <li>{@code ItemStack.copy()}（会 {@code stackTagCompound.copy()}）</li>
 * </ul>
 *
 * <p>
 * 这也是 GT 自己的做法：{@code ElectricItem} 的电量同样直接读写
 * {@code itemStack.getTagCompound()}（键 {@code Charge} / {@code MaxCharge} /
 * {@code Infinite}）。
 * </p>
 *
 * <p>
 * <b>注意：</b>由于本库存是"4 个电池 ItemStack 的快照"，而电池放电是<b>就地修改</b>
 * 内部电池的 NBT（不会触发 {@link #onContentsChanged(int)}），
 * 因此 {@link BatteryCaseEnergyStorage} 在充/放电后必须显式调用 {@link #save()}，
 * 否则电量变化不会落盘（详见 {@link #save()}）。
 * </p>
 */
public class BatteryCaseInventory extends ItemStackHandler {

    /** 槽位数量。 */
    public static final int SIZE = 4;

    /** 在电池盒物品 NBT 中保存本库存的键。 */
    public static final String NBT_KEY = "BatteryCaseInv";

    /** 拥有本库存的电池盒 ItemStack；NBT 读写都经由它，不缓存 NBTTagCompound。 */
    private final ItemStack owner;

    public BatteryCaseInventory(ItemStack owner) {
        super(SIZE);
        this.owner = owner;
        load();
    }

    /** 从电池盒物品的 NBT 读回库存。 */
    private void load() {
        NBTTagCompound tag = owner.getTagCompound();
        if (tag != null && tag.hasKey(NBT_KEY, 10)) {
            super.deserializeNBT(tag.getCompoundTag(NBT_KEY));
        }
    }

    /**
     * 把库存写回电池盒物品的 NBT。
     *
     * <p>
     * 每次调用都重新 {@code owner.getTagCompound()}，因此即使外部替换了整个
     * NBTTagCompound 实例也不会写丢数据。
     * </p>
     */
    public void save() {
        NBTTagCompound tag = owner.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            owner.setTagCompound(tag);
        }
        tag.setTag(NBT_KEY, super.serializeNBT());
    }

    @Override
    protected void onContentsChanged(int slot) {
        save();
    }

    /** @return 盒内当前电压 tier；空盒返回 {@code -1}。 */
    public int getTier() {
        for (int i = 0; i < getSlots(); i++) {
            IElectricItem item = getBattery(getStackInSlot(i));
            if (item != null) {
                return item.getTier();
            }
        }
        return -1;
    }

    /** @return 4 格电池电量之和（EU）。 */
    public long getTotalCharge() {
        long total = 0L;
        for (int i = 0; i < getSlots(); i++) {
            IElectricItem item = getBattery(getStackInSlot(i));
            if (item != null) {
                total += item.getCharge();
            }
        }
        return total;
    }

    /** @return 4 格电池容量之和（EU）。 */
    public long getTotalMaxCharge() {
        long total = 0L;
        for (int i = 0; i < getSlots(); i++) {
            IElectricItem item = getBattery(getStackInSlot(i));
            if (item != null) {
                total += item.getMaxCharge();
            }
        }
        return total;
    }

    /** @return 当前电池数量（0..4）。 */
    public int getBatteryCount() {
        int count = 0;
        for (int i = 0; i < getSlots(); i++) {
            if (!getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** 取出该物品的"电池"能力（要求可对外供电）；非电池返回 {@code null}。 */
    public static IElectricItem getBattery(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        IElectricItem item = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (item == null || !item.canProvideChargeExternally()) {
            return null;
        }
        return item;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // 不允许把电池盒放进它自己（否则 UI 里会出现"电池盒套电池盒"，
        // 而且会造成自引用/无限 NBT 嵌套）
        if (stack.getItem() instanceof ItemBatteryCase) {
            return false;
        }
        IElectricItem candidate = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
        if (candidate == null || !candidate.canProvideChargeExternally()) {
            return false;
        }
        int tier = getTier();
        return tier < 0 || tier == candidate.getTier();
    }
}
