package com.susy.plusplus.machine;

import com.susy.plusplus.SusyPlusPlus;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 对 GT 里那些 <b>{@code private}/{@code protected} 成员</b>的反射访问工具。
 *
 * <h3>为什么用反射而不是 {@code @Shadow} / {@code @Accessor}</h3>
 *
 * <p>
 * 需要碰的成员在运行期 GT jar（{@code gregtech-1.12.2-2.8.10-beta.jar}）里是
 * （均已用 javap 核实）：
 * </p>
 *
 * <ul>
 * <li>{@code MetaTileEntityQuantumTank}：{@code private FluidStack lockedFluid}</li>
 * <li>{@code MetaTileEntityQuantumTank}：{@code protected void setLocked(boolean)}</li>
 * <li>{@code MultiblockControllerBase}：{@code protected void setFlipped(boolean)}</li>
 * </ul>
 *
 * <p>
 * Mixin 的 {@code @Shadow} <b>不能</b>用于 {@code private} 成员，而
 * {@code @Accessor}/{@code @Shadow} 一旦名字对不上会在<b>类加载期直接崩</b>。
 * 本模组的目标是"最坏情况只失效、不崩"，所以这里统一用
 * <b>缓存 + 吞异常的反射</b>：失败只打一条 WARN 并返回默认值。
 * 这些名字都属于 GT 自己的类、<b>不参与 Forge 混淆</b>，运行期名字不变。
 * </p>
 *
 * <p>
 * ⚠ 本类<b>不在</b> {@code com.susy.plusplus.mixin} 包内 —— 那个包被 Mixin 加入了
 * 类加载器排除名单，放在里面会被 {@code NoClassDefFoundError}（XNet 补丁踩过这个坑）。
 * </p>
 */
public final class GtMachineReflection {

    /** 量子缸的类名（用名字判断，避免编译期/运行期版本差异导致类加载失败）。 */
    private static final String QUANTUM_TANK =
            "gregtech.common.metatileentities.storage.MetaTileEntityQuantumTank";

    private static boolean lockedFluidResolved;
    private static Field lockedFluidField;
    private static Method setLockedMethod;

    private static boolean setFlippedResolved;
    private static Method setFlippedMethod;

    private GtMachineReflection() {
    }

    // ==========================================================================
    // 量子缸：锁定流体（LockedFluid）
    // ==========================================================================

    /**
     * 是否是"可锁定流体的机器"——目前只有 GT 的量子缸。
     *
     * <p>
     * 用<b>类名</b>判断而不是 {@code instanceof}：这样即使将来该类被移动/改名，
     * 也只是功能失效，不会在类加载期 {@code NoClassDefFoundError}。
     * </p>
     */
    public static boolean isLockableFluidMachine(MetaTileEntity mte) {
        return mte != null && QUANTUM_TANK.equals(mte.getClass().getName());
    }

    /**
     * 读取量子缸的锁定流体。
     *
     * @return 复制过的 {@link FluidStack}（可直接写进 NBT）；没有锁定/读不到时返回 {@code null}
     */
    public static FluidStack getLockedFluid(MetaTileEntity mte) {
        if (!isLockableFluidMachine(mte)) {
            return null;
        }
        resolveLockedFluid(mte.getClass());
        if (lockedFluidField == null) {
            return null;
        }
        try {
            Object value = lockedFluidField.get(mte);
            if (value instanceof FluidStack) {
                return ((FluidStack) value).copy();
            }
        } catch (Throwable ignored) {
            // 读取失败按"没有锁定"处理
        }
        return null;
    }

    /**
     * 写回量子缸的锁定流体。
     *
     * <p>
     * 顺序刻意与 GT 自己的界面回调（{@code handleLocking}）一致：
     * 先 {@code setLocked(stack != null)}，<b>再</b>直接给 {@code lockedFluid} 字段赋值
     * —— 因为 {@code setLocked(true)} 会用"罐里当前的流体"覆盖该字段，必须最后赋值。
     * </p>
     *
     * @param stack 要锁定的流体；{@code null} 表示解除锁定
     * @return 是否成功写入
     */
    public static boolean setLockedFluid(MetaTileEntity mte, FluidStack stack) {
        if (!isLockableFluidMachine(mte)) {
            return false;
        }
        resolveLockedFluid(mte.getClass());
        if (setLockedMethod == null || lockedFluidField == null) {
            return false;
        }
        try {
            setLockedMethod.invoke(mte, stack != null);
            lockedFluidField.set(mte, stack == null ? null : stack.copy());
            return true;
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Failed to apply locked fluid to {}: {}",
                    mte.getClass().getName(), t.toString());
            return false;
        }
    }

    private static void resolveLockedFluid(Class<?> mteClass) {
        if (lockedFluidResolved) {
            return;
        }
        lockedFluidResolved = true;
        try {
            Field field = mteClass.getDeclaredField("lockedFluid");
            field.setAccessible(true);
            lockedFluidField = field;
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] Quantum tank field 'lockedFluid' not found, fluid-lock copy disabled: {}",
                    t.toString());
        }
        try {
            Method method = mteClass.getDeclaredMethod("setLocked", boolean.class);
            method.setAccessible(true);
            setLockedMethod = method;
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Quantum tank method 'setLocked' not found: {}", t.toString());
        }
    }

    // ==========================================================================
    // 多方块控制器：翻转状态（IsFlipped）
    // ==========================================================================

    /** 写回多方块控制器的 {@code isFlipped}（{@code setFlipped} 是 {@code protected}）。 */
    public static boolean setFlipped(MultiblockControllerBase controller, boolean flipped) {
        if (controller == null) {
            return false;
        }
        if (controller.isFlipped() == flipped) {
            return true;
        }
        resolveSetFlipped(controller.getClass());
        if (setFlippedMethod == null) {
            return false;
        }
        try {
            setFlippedMethod.invoke(controller, flipped);
            return true;
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Failed to restore flipped state on {}: {}",
                    controller.getClass().getName(), t.toString());
            return false;
        }
    }

    private static void resolveSetFlipped(Class<?> controllerClass) {
        if (setFlippedResolved) {
            return;
        }
        setFlippedResolved = true;
        try {
            Method method = controllerClass.getDeclaredMethod("setFlipped", boolean.class);
            method.setAccessible(true);
            setFlippedMethod = method;
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Multiblock setFlipped not found, flipped copy disabled: {}",
                    t.toString());
        }
    }

    /** 安全地把朝向转成索引（{@code null} → 0 = DOWN，与 GT 的默认值一致）。 */
    public static int indexOf(EnumFacing facing) {
        return facing == null ? EnumFacing.DOWN.getIndex() : facing.getIndex();
    }
}
