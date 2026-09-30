package com.susy.plusplus.machine;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 本模组给<b>机器</b>附加的额外状态（功能二 / 功能三）：
 *
 * <ul>
 * <li>{@code noDuplicateImport}：不允许不同槽位被动输入相同物品；</li>
 * <li>{@code originalFluidCapacity[]} / {@code fluidCapacity[]}：
 * 每个流体槽位的"原容量"与"当前容量"。</li>
 * </ul>
 *
 * <h3>存放位置</h3>
 *
 * <p>
 * 运行期用 {@link WeakHashMap} 挂在 {@link MetaTileEntity} 实例上（不改变 GT 的类结构）；
 * 持久化靠 NBT，由 {@code MetaTileEntityExtraStateMixin} 在
 * {@code MetaTileEntity#writeToNBT/readFromNBT} 的首尾注入读写。
 * 键名：
 * </p>
 *
 * <ul>
 * <li>{@value #NBT_NO_DUPLICATE_IMPORT}</li>
 * <li>{@value #NBT_ORIGINAL_FLUID_CAPACITY}（int 数组，只在第一次写入时记录）</li>
 * <li>{@value #NBT_FLUID_CAPACITY}（int 数组，当前容量）</li>
 * </ul>
 *
 * <p>
 * 客户端可见性：{@code noDuplicateImport} 额外走 GT 自己的
 * {@code writeInitialSyncData / receiveInitialSyncData} 同步，
 * 因此工具箱里的开关状态在客户端也正确；改变时由 UI 回调在<b>两端各执行一次</b>
 * （{@code InteractionSyncHandler} 的语义）保证即时刷新。
 * </p>
 */
public final class MachineExtraState {

    /** 不允许不同槽位被动输入相同物品。 */
    public static final String NBT_NO_DUPLICATE_IMPORT = "SuNoDuplicateImport";

    /** 各流体槽位的<b>原</b>容量（第一次写入后不再改变）。 */
    public static final String NBT_ORIGINAL_FLUID_CAPACITY = "OriginalFluidCapacity";

    /** 各流体槽位的当前容量。 */
    public static final String NBT_FLUID_CAPACITY = "FluidCapacity";

    private static final Map<MetaTileEntity, Data> DATA = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static final Map<IItemHandler, IItemHandler> WRAPPERS = Collections
            .synchronizedMap(new WeakHashMap<>());

    private MachineExtraState() {
    }

    private static final class Data {

        boolean noDuplicateImport;
        /** 原容量；{@code null} 表示还没记录过。 */
        int[] originalFluidCapacity;
        /** 当前容量；{@code null} 表示未修改过（等于原容量）。 */
        int[] fluidCapacity;
        /** 已包好的物品库存包装器（按底层 handler 缓存，避免每次 getCapability 都新建）。 */
        IItemHandler wrappedHandler;
        IItemHandler wrappedSource;
    }

    private static Data data(MetaTileEntity mte) {
        if (mte == null) {
            return null;
        }
        return DATA.computeIfAbsent(mte, k -> new Data());
    }

    // ==========================================================================
    // 功能二：不允许不同槽位被动输入相同物品
    // ==========================================================================

    public static boolean isNoDuplicateImport(MetaTileEntity mte) {
        Data data = mte == null ? null : DATA.get(mte);
        return data != null && data.noDuplicateImport;
    }

    public static void setNoDuplicateImport(MetaTileEntity mte, boolean value) {
        Data data = data(mte);
        if (data == null) {
            return;
        }
        data.noDuplicateImport = value;
        if (mte.getWorld() != null && !mte.getWorld().isRemote) {
            mte.markDirty();
        }
    }

    /**
     * 给"外部自动化拿到的物品库存"套一层过滤（功能二）。
     *
     * <p>
     * <b>只包装通过 {@code getCapability(ITEM_HANDLER, side)} 拿到的那个 handler</b>
     * ——GUI 槽位用的是机器<b>内部</b>的 handler 对象（{@code createUITemplate} 直接传引用），
     * 因此玩家手动放物品完全不受影响，天然满足"只拦截被动输入"。
     * </p>
     *
     * @return 开关关闭时原样返回 {@code handler}
     */
    public static IItemHandler wrapItemHandler(MetaTileEntity mte, IItemHandler handler) {
        if (mte == null || handler == null || !isNoDuplicateImport(mte)) {
            return handler;
        }
        Data data = data(mte);
        if (data == null) {
            return handler;
        }
        if (data.wrappedHandler != null && data.wrappedSource == handler) {
            return data.wrappedHandler;
        }
        IItemHandler wrapped = new NoDuplicateItemHandler(mte, handler);
        data.wrappedSource = handler;
        data.wrappedHandler = wrapped;
        return wrapped;
    }

    // ==========================================================================
    // 功能三：流体槽位容量
    // ==========================================================================

    /** 该机器是否记录过原容量。 */
    public static boolean hasOriginalCapacity(MetaTileEntity mte) {
        Data data = mte == null ? null : DATA.get(mte);
        return data != null && data.originalFluidCapacity != null;
    }

    /**
     * 记录"原容量"（<b>只在第一次调用时生效</b>）。
     *
     * <p>
     * 需求要求：右侧标注的原容量永远是第一次记录的那个值 —— 所以这里一旦记过就直接返回。
     * </p>
     */
    public static void rememberOriginalCapacity(MetaTileEntity mte, int[] capacities) {
        Data data = data(mte);
        if (data == null || capacities == null) {
            return;
        }
        if (data.originalFluidCapacity != null) {
            return;
        }
        data.originalFluidCapacity = capacities.clone();
        if (mte.getWorld() != null && !mte.getWorld().isRemote) {
            mte.markDirty();
        }
    }

    /** @return 原容量数组（副本）；没记录过时返回 {@code null}。 */
    public static int[] getOriginalCapacity(MetaTileEntity mte) {
        Data data = mte == null ? null : DATA.get(mte);
        return data == null || data.originalFluidCapacity == null
                ? null
                : data.originalFluidCapacity.clone();
    }

    /** @return 当前容量数组（副本）；没改过时返回 {@code null}。 */
    public static int[] getCurrentCapacity(MetaTileEntity mte) {
        Data data = mte == null ? null : DATA.get(mte);
        return data == null || data.fluidCapacity == null ? null : data.fluidCapacity.clone();
    }

    /** 设置当前容量（会同时保证原容量已记录）。 */
    public static void setCurrentCapacity(MetaTileEntity mte, int[] capacities) {
        Data data = data(mte);
        if (data == null || capacities == null) {
            return;
        }
        data.fluidCapacity = capacities.clone();
        if (mte.getWorld() != null && !mte.getWorld().isRemote) {
            mte.markDirty();
        }
    }

    // ==========================================================================
    // 持久化
    // ==========================================================================

    public static void writeToNbt(MetaTileEntity mte, NBTTagCompound tag) {
        Data data = mte == null ? null : DATA.get(mte);
        if (data == null || tag == null) {
            return;
        }
        if (data.noDuplicateImport) {
            tag.setBoolean(NBT_NO_DUPLICATE_IMPORT, true);
        }
        if (data.originalFluidCapacity != null) {
            tag.setIntArray(NBT_ORIGINAL_FLUID_CAPACITY, data.originalFluidCapacity.clone());
        }
        if (data.fluidCapacity != null) {
            tag.setIntArray(NBT_FLUID_CAPACITY, data.fluidCapacity.clone());
        }
    }

    // ==========================================================================
    // 初次同步（客户端也要知道开关状态）
    // ==========================================================================

    /**
     * 写入初次同步数据（服务端）。
     *
     * <p>
     * 必须与 {@link #readInitialSync} 严格成对：两边都注入在
     * {@code writeInitialSyncData / receiveInitialSyncData} 的 <b>HEAD</b>，
     * 因此顺序一致、不会打乱 GT 自己写的内容。
     * </p>
     */
    public static void writeInitialSync(MetaTileEntity mte, PacketBuffer buf) {
        buf.writeBoolean(isNoDuplicateImport(mte));
    }

    /** 读取初次同步数据（客户端）。 */
    public static void readInitialSync(MetaTileEntity mte, PacketBuffer buf) {
        Data data = data(mte);
        if (data != null) {
            data.noDuplicateImport = buf.readBoolean();
        }
    }

    public static void readFromNbt(MetaTileEntity mte, NBTTagCompound tag) {
        Data data = data(mte);
        if (data == null || tag == null) {
            return;
        }
        data.noDuplicateImport = tag.getBoolean(NBT_NO_DUPLICATE_IMPORT);
        if (tag.hasKey(NBT_ORIGINAL_FLUID_CAPACITY)) {
            data.originalFluidCapacity = tag.getIntArray(NBT_ORIGINAL_FLUID_CAPACITY);
        }
        if (tag.hasKey(NBT_FLUID_CAPACITY)) {
            data.fluidCapacity = tag.getIntArray(NBT_FLUID_CAPACITY);
        }
    }

    /** 物品库存包装器（供 {@code getCapability} 注入使用）。 */
    public static final class NoDuplicateItemHandler implements IItemHandler {

        private final MetaTileEntity machine;
        private final IItemHandler delegate;

        NoDuplicateItemHandler(MetaTileEntity machine, IItemHandler delegate) {
            this.machine = machine;
            this.delegate = delegate;
        }

        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return delegate.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (hasSameItemInAnotherSlot(slot, stack)) {
                // 直接拒收：返回未插入的整堆，自动化会当"塞不进去"处理
                return stack;
            }
            return delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(slot, amount, simulate);
        }

        /** 是否在"别的槽位"里已经存在同种物品（NBT 也要相同）。 */
        private boolean hasSameItemInAnotherSlot(int insertSlot, ItemStack stack) {
            int slots = delegate.getSlots();
            for (int i = 0; i < slots; i++) {
                if (i == insertSlot) {
                    continue;
                }
                ItemStack existing = delegate.getStackInSlot(i);
                if (existing == null || existing.isEmpty()) {
                    continue;
                }
                if (ItemStack.areItemsEqual(stack, existing)
                        && ItemStack.areItemStackTagsEqual(stack, existing)) {
                    return true;
                }
            }
            return false;
        }
    }
}
