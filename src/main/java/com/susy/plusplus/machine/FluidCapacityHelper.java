package com.susy.plusplus.machine;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 「容量限制（仅流体）」用到的机器流体槽工具（功能三）。
 *
 * <h3>怎么拿到"真正的槽位对象"</h3>
 *
 * <p>
 * GT 的机器通过 {@code MetaTileEntity#getFluidInventory()} 暴露 {@code IFluidHandler}。
 * 多槽时它是 {@link IMultipleTankHandler}（{@code FluidTankList} 实现），
 * 其 {@code getFluidTanks()} 给出的 {@code MultiFluidTankEntry} 有一个
 * <b>public {@code getDelegate()}</b>（已用 javap 核实运行期 GT 2.8.10），
 * 拿到的就是真正的 {@code IFluidTank}（通常是 GT 的 {@code NotifiableFluidTank}，
 * 它是 {@link FluidTank} 的子类）。
 * </p>
 *
 * <h3>怎么改容量</h3>
 *
 * <p>
 * {@code FluidTank#capacity} 是 Forge 里的 {@code protected int} 字段，
 * GT 全库没有任何 {@code setCapacity} 调用，Forge 源码也不在参考路径中 ——
 * 因此这里用<b>反射写字段</b>（字段名 {@code capacity}，沿类继承链查找），
 * 失败只打一条 WARN 并返回 {@code false}，<b>不会崩</b>。
 * </p>
 */
public final class FluidCapacityHelper {

    /** {@code FluidTank.capacity} 的字段缓存（按类）。 */
    private static final Map<Class<?>, Field> CAPACITY_FIELDS = Collections
            .synchronizedMap(new WeakHashMap<>());

    private FluidCapacityHelper() {
    }

    // ==========================================================================
    // 枚举槽位
    // ==========================================================================

    /**
     * 取出机器所有"可改容量的"流体槽位。
     *
     * @return 真正的 {@link IFluidTank} 列表；机器没有流体槽时返回空列表
     */
    public static List<IFluidTank> tanks(MetaTileEntity mte) {
        List<IFluidTank> result = new ArrayList<>();
        if (mte == null) {
            return result;
        }
        IFluidHandler inventory;
        try {
            inventory = mte.getFluidInventory();
        } catch (Throwable t) {
            return result;
        }
        collect(inventory, result, 0);
        return result;
    }

    /**
     * 递归收集一个 {@code IFluidHandler} 里的所有"真正的槽位对象"。
     *
     * <h3>为什么要递归</h3>
     *
     * <p>
     * 机器/仓室的流体库存可能是好几层包装（都用 javap 在运行期 GT 2.8.10 核实过）：
     * </p>
     *
     * <ul>
     * <li>{@link IMultipleTankHandler}（{@code FluidTankList}）→
     * {@code getFluidTanks()} → {@code MultiFluidTankEntry#getDelegate()}（public）→ 真正的槽位；</li>
     * <li>{@code gregtech.api.capability.impl.FluidHandlerProxy}（<b>只实现 IFluidHandler</b>）——
     * 它有 <b>public</b> 字段 {@code input} / {@code output}。
     * 流体输入仓/输出仓这类"输入与输出分开"的部件就是被它包着的：
     * 只用 {@code instanceof IMultipleTankHandler} 判断会**漏掉整台仓室**
     * （这正是之前"输入仓不支持"的原因）；</li>
     * <li>单槽：库存本身就是 {@link IFluidTank}。</li>
     * </ul>
     *
     * <p>
     * 深度限制 4 层 + 去重，避免异常结构导致死循环或重复槽位。
     * </p>
     */
    private static void collect(IFluidHandler handler, List<IFluidTank> out, int depth) {
        if (handler == null || depth > 4) {
            return;
        }
        if (handler instanceof IMultipleTankHandler) {
            for (IMultipleTankHandler.MultiFluidTankEntry entry
                    : ((IMultipleTankHandler) handler).getFluidTanks()) {
                addIfAbsent(out, entry.getDelegate(), depth + 1);
            }
            return;
        }
        // 注意：FluidHandlerProxy 只实现 IFluidHandler，必须先于 IFluidTank 判断
        if (handler instanceof gregtech.api.capability.impl.FluidHandlerProxy) {
            gregtech.api.capability.impl.FluidHandlerProxy proxy
                    = (gregtech.api.capability.impl.FluidHandlerProxy) handler;
            collect(proxy.input, out, depth + 1);
            collect(proxy.output, out, depth + 1);
            return;
        }
        if (handler instanceof IFluidTank) {
            addIfAbsent(out, (IFluidTank) handler, depth + 1);
        }
    }

    private static void addIfAbsent(List<IFluidTank> out, IFluidTank tank, int depth) {
        if (tank == null || out.contains(tank)) {
            return;
        }
        out.add(tank);
    }

    /** 各槽当前容量。 */
    public static int[] capacitiesOf(List<IFluidTank> tanks) {
        int[] result = new int[tanks.size()];
        for (int i = 0; i < tanks.size(); i++) {
            result[i] = Math.max(0, tanks.get(i).getCapacity());
        }
        return result;
    }

    // ==========================================================================
    // 写容量
    // ==========================================================================

    /**
     * 把某个槽位的容量改成 {@code capacity}。
     *
     * @return 是否成功
     */
    public static boolean setCapacity(IFluidTank tank, int capacity) {
        if (tank == null) {
            return false;
        }
        if (tank.getCapacity() == capacity) {
            return true;
        }
        Field field = capacityField(tank.getClass());
        if (field == null) {
            return false;
        }
        try {
            field.setInt(tank, Math.max(0, capacity));
            trimToCapacity(tank);
            return true;
        } catch (Throwable t) {
            com.susy.plusplus.SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] Failed to set fluid capacity on {}: {}", tank.getClass().getName(),
                    t.toString());
            return false;
        }
    }

    /**
     * 把槽内**超出现有容量**的那部分流体销毁掉。
     *
     * <p>
     * 否则会出现"容量 1000、里面却有 5000"的非法状态（GT 自己的界面也都假设
     * {@code amount <= capacity}）。这里用 {@code drain} 多出来的量：
     * 对 {@code IFluidHandler} 而言 drain 出去不再收回来就是销毁。
     * </p>
     */
    private static void trimToCapacity(IFluidTank tank) {
        if (tank == null) {
            return;
        }
        net.minecraftforge.fluids.FluidStack fluid = tank.getFluid();
        int capacity = Math.max(0, tank.getCapacity());
        if (fluid == null || fluid.amount <= capacity) {
            return;
        }
        // 注意：IFluidTank 只有 drain(int, boolean)（drain(FluidStack, boolean) 属于能力层的 IFluidHandler），
        // 所以这里按"数量"抽掉超出部分；返回值不用 = 直接销毁。
        int excess = fluid.amount - capacity;
        tank.drain(excess, true);
    }

    private static Field capacityField(Class<?> type) {
        Field cached = CAPACITY_FIELDS.get(type);
        if (cached != null) {
            return cached;
        }
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("capacity");
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    CAPACITY_FIELDS.put(type, field);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // 继续往父类找
            } catch (Throwable t) {
                com.susy.plusplus.SusyPlusPlus.LOGGER.warn(
                        "[SusyPlusPlus] Fluid capacity field not accessible on {}: {}",
                        current.getName(), t.toString());
                break;
            }
            current = current.getSuperclass();
        }
        com.susy.plusplus.SusyPlusPlus.LOGGER.warn(
                "[SusyPlusPlus] No 'capacity' field found for {}, fluid capacity editing disabled for it",
                type.getName());
        CAPACITY_FIELDS.put(type, null);
        return null;
    }

    // ==========================================================================
    // 与 NBT 的配合
    // ==========================================================================

    /**
     * 第一次打开界面时记录"原容量"（只记一次）。
     *
     * @return 原容量数组
     */
    public static int[] ensureOriginalRecorded(MetaTileEntity mte) {
        int[] original = MachineExtraState.getOriginalCapacity(mte);
        if (original != null) {
            return original;
        }
        int[] current = capacitiesOf(tanks(mte));
        MachineExtraState.rememberOriginalCapacity(mte, current);
        return current;
    }

    /**
     * 保存用户填的当前容量（服务端）。
     *
     * <p>
     * 每个值都会被夹到 {@code [0, 原容量]}：需求要求"输入值不能超过原容量"。
     * </p>
     *
     * @return 实际写入的容量数组
     */
    public static int[] applyCapacities(MetaTileEntity mte, int[] values) {
        int[] original = ensureOriginalRecorded(mte);
        List<IFluidTank> tanks = tanks(mte);
        int[] applied = new int[tanks.size()];
        for (int i = 0; i < tanks.size(); i++) {
            int wanted = i < values.length ? values[i] : original[i];
            int max = i < original.length ? original[i] : tanks.get(i).getCapacity();
            int clamped = Math.max(0, Math.min(max, wanted));
            setCapacity(tanks.get(i), clamped);
            applied[i] = clamped;
        }
        MachineExtraState.setCurrentCapacity(mte, applied);
        return applied;
    }

    /**
     * 机器加载（{@code readFromNBT} 之后）时把保存的当前容量重新应用一遍。
     *
     * <p>
     * 没有保存过当前容量时什么都不做（保持 GT 自己的构造容量）。
     * </p>
     */
    public static void applyStoredCapacities(MetaTileEntity mte) {
        int[] current = MachineExtraState.getCurrentCapacity(mte);
        if (current == null) {
            return;
        }
        int[] original = MachineExtraState.getOriginalCapacity(mte);
        List<IFluidTank> tanks = tanks(mte);
        for (int i = 0; i < tanks.size() && i < current.length; i++) {
            int max = original != null && i < original.length ? original[i] : tanks.get(i).getCapacity();
            setCapacity(tanks.get(i), Math.max(0, Math.min(max, current[i])));
            // 读档后也可能出现"存量 > 新容量"（例如改小容量后立刻存档），同样要销毁超出部分
            trimToCapacity(tanks.get(i));
        }
    }
}
