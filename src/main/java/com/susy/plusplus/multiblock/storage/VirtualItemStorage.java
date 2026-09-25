package com.susy.plusplus.multiblock.storage;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

/**
 * 「单物品类型 + long 数量」的大容量物品库存（多方块板条箱的核心）。
 *
 * <p>
 * 直接照搬 GT 量子箱（{@code MetaTileEntityQuantumChest}）的存储范式：
 * 只保存一个"展示用" {@link ItemStack}（数量恒为 1）+ 一个 {@code long} 计数器。
 * 由于一个 {@code ItemStack} 的 count 是 <b>byte</b>（上限 127），
 * 原版容器同步绝不允许出现"一格一百万个"的物品堆，
 * 因此本类对外<b>始终把展示堆的数量钳到 {@value #DISPLAY_LIMIT}</b>，
 * 真正的数量只存在于 {@code stored} 计数器中 —— 自动化（漏斗/管道/AE2）
 * 通过 {@link #insertItem}/{@link #extractItem} 一次可以搬运任意多（受总量限制）。
 * </p>
 *
 * <h3>为什么 setStackInSlot 在服务端是空操作</h3>
 *
 * <p>
 * 原版 {@code Container#slotClick} 在"整槽替换"时会调用 {@code setStackInSlot}。
 * 如果把它理解为"把总量设为 stack.getCount()"，那么一次普通点击就会把百万存量
 * 覆盖成 64 —— 这正是 GT 量子箱<b>不用</b>原版槽位同步的原因。
 * 因此：
 * </p>
 *
 * <ul>
 * <li>服务端：{@code setStackInSlot} 直接忽略（只读+插入/抽取）；</li>
 * <li>客户端：只当作"显示缓存"写入，供 GUI 渲染与本地化物品名使用。</li>
 * </ul>
 */
public class VirtualItemStorage implements IItemHandlerModifiable {

    /** 展示堆的最大数量（原版一个槽位的合法上限）。 */
    public static final int DISPLAY_LIMIT = 64;

    private static final String NBT_STACK = "StoredItem";
    private static final String NBT_COUNT = "StoredCount";

    private final MetaTileEntity owner;
    private final long capacity;

    /** 展示用堆（数量恒为 1）。 */
    private ItemStack virtualStack = ItemStack.EMPTY;

    /** 真实存量。 */
    private long stored = 0L;

    /** 客户端显示缓存（由原版容器同步写入）。 */
    private ItemStack clientDisplay = ItemStack.EMPTY;

    public VirtualItemStorage(MetaTileEntity owner, long capacity) {
        this.owner = owner;
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getStoredCount() {
        return stored;
    }

    /** 服务端用：真实存量对应的"展示堆"（数量钳制，客户端拿不到）。 */
    public ItemStack getVirtualStack() {
        return virtualStack;
    }

    public boolean isEmpty() {
        return stored <= 0L || virtualStack.isEmpty();
    }

    // ------------------------------------------------------------------
    // IItemHandler / IItemHandlerModifiable
    // ------------------------------------------------------------------

    @Override
    public int getSlots() {
        return 1;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (isClientSide()) {
            return clientDisplay;
        }
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = virtualStack.copy();
        out.setCount((int) Math.min(stored, DISPLAY_LIMIT));
        return out;
    }

    /**
     * 槽位上限固定为 {@value #DISPLAY_LIMIT}：这样原版容器永远只看到合法（≤64）的堆，
     * 不会因为"一格一百万"而写出非法封包。真正的总量上限由 {@link #capacity} 负责。
     */
    @Override
    public int getSlotLimit(int slot) {
        return DISPLAY_LIMIT;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isEmpty()) {
            return true;
        }
        return ItemStack.areItemsEqual(virtualStack, stack) && ItemStack.areItemStackTagsEqual(virtualStack, stack);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        long free = capacity - stored;
        if (free <= 0L) {
            return stack;
        }
        int accepted = (int) Math.min(stack.getCount(), free);
        if (accepted <= 0) {
            return stack;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(accepted);

        if (!simulate) {
            if (isEmpty()) {
                ItemStack display = stack.copy();
                display.setCount(1);
                this.virtualStack = display;
            }
            this.stored += accepted;
            onChanged();
        }
        return remainder;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || isEmpty()) {
            return ItemStack.EMPTY;
        }
        int extracted = (int) Math.min(amount, stored);
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack out = virtualStack.copy();
        out.setCount(extracted);

        if (!simulate) {
            this.stored -= extracted;
            if (this.stored <= 0L) {
                this.stored = 0L;
                this.virtualStack = ItemStack.EMPTY;
            }
            onChanged();
        }
        return out;
    }

    /** 服务端忽略（见类注释）；客户端仅作显示缓存。 */
    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (isClientSide()) {
            this.clientDisplay = stack;
        }
    }

    // ------------------------------------------------------------------
    // 持久化 / 工具
    // ------------------------------------------------------------------

    public void writeToNBT(NBTTagCompound data) {
        if (isEmpty()) {
            data.removeTag(NBT_STACK);
            data.removeTag(NBT_COUNT);
            return;
        }
        NBTTagCompound stackTag = new NBTTagCompound();
        virtualStack.writeToNBT(stackTag);
        data.setTag(NBT_STACK, stackTag);
        data.setLong(NBT_COUNT, stored);
    }

    public void readFromNBT(NBTTagCompound data) {
        this.stored = 0L;
        this.virtualStack = ItemStack.EMPTY;
        if (data.hasKey(NBT_STACK, 10)) { // 10 = NBTTagCompound
            ItemStack stack = new ItemStack(data.getCompoundTag(NBT_STACK));
            if (!stack.isEmpty()) {
                stack.setCount(1);
                this.virtualStack = stack;
            }
        }
        long count = data.getLong(NBT_COUNT);
        if (this.virtualStack.isEmpty() || count <= 0L) {
            this.virtualStack = ItemStack.EMPTY;
            this.stored = 0L;
        } else {
            this.stored = Math.min(count, capacity);
        }
    }

    /** 破坏控制器时按需求「内容物随控制器消失」，不做任何掉落。 */
    public void clear() {
        this.stored = 0L;
        this.virtualStack = ItemStack.EMPTY;
        this.clientDisplay = ItemStack.EMPTY;
    }

    private void onChanged() {
        if (owner != null) {
            owner.markDirty();
        }
    }

    private boolean isClientSide() {
        return owner != null && owner.getWorld() != null && owner.getWorld().isRemote;
    }
}
