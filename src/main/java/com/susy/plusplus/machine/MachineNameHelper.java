package com.susy.plusplus.machine;

import com.susy.plusplus.SusyPlusPlus;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * 给「已放置的 GT 机器」读写自定义名字。
 *
 * <h3>为什么要反射</h3>
 *
 * <p>
 * 名字保存在 {@code gregtech.api.metatileentity.MetaTileEntityHolder#customName}
 * （NBT 键 {@code CustomName}，见 {@code GregtechDataCodes.CUSTOM_NAME}），
 * 公开访问器是：
 * </p>
 *
 * <ul>
 * <li>{@code public void setCustomName(String)}</li>
 * <li>{@code public String getName()}</li>
 * </ul>
 *
 * <p>
 * 但 {@code MetaTileEntityHolder} 通过 {@code @Optional.Interface} 实现了 AE2 的
 * {@code IActionHost}，<b>直接引用该类型</b>会让编译期需要 AE2 的类（本模组刻意不依赖 AE2）。
 * 因此这里按 {@link GtMachineReflection} 的同一套思路：<b>按方法名反射 + 缓存 + 吞异常</b>，
 * 最坏情况只是功能失效并打一条 WARN，绝不会崩。
 * </p>
 *
 * <h3>同步</h3>
 *
 * <p>
 * {@code setCustomName} 在服务端只做 {@code markAsDirty()}，不会立刻告诉客户端，
 * 所以改完名字后这里再触发一次 {@code world.notifyBlockUpdate(pos, state, state, 3)}：
 * 方块实体的更新包会带上 {@code getUpdateTag()} → {@code writeInitialSyncData()}
 * （Holder 在其中写入 {@code CustomName}）→ 客户端 {@code receiveInitialSyncData()}
 * 里重新 {@code setCustomName}，从而<b>立刻刷新头顶的浮动名牌</b>（与 GT 原版放置时一致）。
 * </p>
 */
public final class MachineNameHelper {

    private static boolean resolved;
    private static Method setCustomName;
    private static Method getCustomName;

    private MachineNameHelper() {
    }

    private static void resolve(TileEntity holder) {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            setCustomName = holder.getClass().getMethod("setCustomName", String.class);
        } catch (NoSuchMethodException e) {
            SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] MachineNameHelper: {} has no setCustomName(String); renaming disabled.",
                    holder.getClass().getName());
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] MachineNameHelper: failed to resolve setCustomName.", t);
        }
        try {
            getCustomName = holder.getClass().getMethod("getName");
        } catch (Throwable ignored) {
            // 只读名字失败不影响重命名
        }
    }

    /**
     * 设置该位置机器的自定义名字，并同步给客户端。
     *
     * @param name 新名字；{@code null} 或空串表示<b>清除</b>（恢复默认机器名）
     * @return {@code true} 表示已写入并触发同步；{@code false} 表示该位置不是 GT 机器 / API 不可用
     */
    public static boolean setName(World world, BlockPos pos, String name) {
        if (world == null || pos == null || world.isRemote) {
            return false;
        }
        TileEntity holder = world.getTileEntity(pos);
        if (holder == null) {
            return false;
        }
        resolve(holder);
        if (setCustomName == null) {
            return false;
        }
        String value = name == null ? "" : name;
        try {
            setCustomName.invoke(holder, value);
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] MachineNameHelper: setCustomName failed at {}.", pos, t);
            return false;
        }
        // 立刻把新名字同步到客户端（否则头顶浮动名牌要等区块重载才更新）
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        return true;
    }

    /**
     * <b>客户端专用</b>：把服务端同步来的名字应用到本地机器，并强制刷新悬浮名牌。
     *
     * <h3>为什么不能只写字段</h3>
     *
     * <p>
     * GT 的 {@code MetaTileEntityHolder#updateNameTagParticle()} 只在
     * {@code nameTagParticle == null} 时<b>新建</b>粒子；已存在的粒子不会随
     * {@code customName} 字段变化而更新（javap 核实 2.8.7 字节码）。
     * 因此仅仅同步 NBT 字段的话，头顶文字会一直是旧名字，直到区块重载。
     * </p>
     *
     * <p>
     * 这里利用 {@code setCustomName} 的公开语义：先写入空串（{@code hasCustomName()} 变 false
     * → GT 回收旧粒子并把字段置 null），再写入目标名字（字段为 null → 新建粒子，内容即新名字）。
     * 全程只用公开 API + 反射，不碰 GT 内部类。
     * </p>
     */
    public static void applyNameOnClient(World world, BlockPos pos, String name) {
        if (world == null || pos == null || !world.isRemote) {
            return;
        }
        TileEntity holder = world.getTileEntity(pos);
        if (holder == null) {
            return;
        }
        resolve(holder);
        if (setCustomName == null) {
            return;
        }
        String target = name == null ? "" : name;
        try {
            // 1) 先清空：让 GT 回收旧的悬浮名牌粒子
            setCustomName.invoke(holder, "");
            // 2) 再写目标名：粒子字段已是 null，GT 会按新名字重建
            if (!target.isEmpty()) {
                setCustomName.invoke(holder, target);
            }
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] MachineNameHelper: client name refresh failed at {}.", pos, t);
        }
    }

    /**
     * 读取该位置机器的自定义名字。
     *
     * @return 自定义名字；没有自定义名字 / 不是 GT 机器时返回空串
     */
    public static String getName(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return "";
        }
        TileEntity holder = world.getTileEntity(pos);
        if (holder == null) {
            return "";
        }
        resolve(holder);
        if (getCustomName == null) {
            return "";
        }
        try {
            Object value = getCustomName.invoke(holder);
            return value instanceof String ? (String) value : "";
        } catch (Throwable t) {
            return "";
        }
    }
}
