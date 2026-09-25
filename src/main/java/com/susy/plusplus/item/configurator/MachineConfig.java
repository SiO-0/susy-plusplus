package com.susy.plusplus.item.configurator;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.common.metatileentities.electric.MetaTileEntityTransformer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * 「复制机器配置」用的快照。
 *
 * <p>
 * <b>只复制"机器各面朝向等配置"</b>，具体包括（每一类都独立记 {@code has*} 标记，
 * 机器不支持时该类的数据会被整体忽略）：
 * </p>
 *
 * <table border="1">
 * <caption>复制范围</caption>
 * <tr>
 * <th>项目</th>
 * <th>来源 API（均已在本项目编译用的 GT jar 中核实）</th>
 * </tr>
 * <tr>
 * <td>正面朝向</td>
 * <td>{@code MetaTileEntity#hasFrontFacing/getFrontFacing/isValidFrontFacing/setFrontFacing}</td>
 * </tr>
 * <tr>
 * <td>喷漆颜色</td>
 * <td>{@code MetaTileEntity#isPainted/getPaintingColor/setPaintingColor(int)}</td>
 * </tr>
 * <tr>
 * <td>音效（静音）</td>
 * <td>{@code MetaTileEntity#isMuffled/toggleMuffled()}（GT 只有开关，没有 setter）</td>
 * </tr>
 * <tr>
 * <td>物品/流体输出面</td>
 * <td>{@code SimpleMachineMetaTileEntity#getOutputFacingItems/Fluids + setOutputFacingItems/Fluids}</td>
 * </tr>
 * <tr>
 * <td>自动输出开关</td>
 * <td>{@code isAutoOutputItems/Fluids + setAutoOutputItems/Fluids}</td>
 * </tr>
 * <tr>
 * <td>输出口是否允许输入</td>
 * <td>{@code isAllowInputFromOutputSideItems/Fluids + setAllowInputFromOutputSideItems/Fluids}</td>
 * </tr>
 * <tr>
 * <td>变压器升/降压</td>
 * <td>{@code MetaTileEntityTransformer#isInverted/setTransformUp(boolean)}</td>
 * </tr>
 * </table>
 *
 * <p>
 * <b>刻意不复制</b>：方块 id、坐标、能量缓存、物品缓存、流体缓存、机器内部的
 * 各类库存（如 {@code ChargerInventory}、{@code CircuitInventory}）、封面（cover）等 ——
 * 这些要么属于存档身份，要么属于运行时状态。
 * </p>
 */
public final class MachineConfig {

    // ---- NBT 键（用我们自己的命名，避免与机器 NBT 混淆）----
    private static final String NBT_HAS_FRONT = "HasFront";
    private static final String NBT_FRONT = "Front";
    private static final String NBT_HAS_PAINTING = "HasPainting";
    private static final String NBT_PAINTING = "Painting";
    private static final String NBT_HAS_MUFFLED = "HasMuffled";
    private static final String NBT_MUFFLED = "Muffled";
    private static final String NBT_HAS_TRANSFORMER = "HasTransformer";
    private static final String NBT_INVERTED = "Inverted";
    private static final String NBT_HAS_OUTPUT = "HasOutput";
    private static final String NBT_OUT_ITEMS = "OutItems";
    private static final String NBT_OUT_FLUIDS = "OutFluids";
    private static final String NBT_AUTO_ITEMS = "AutoItems";
    private static final String NBT_AUTO_FLUIDS = "AutoFluids";
    private static final String NBT_ALLOW_ITEMS = "AllowItems";
    private static final String NBT_ALLOW_FLUIDS = "AllowFluids";

    private final boolean hasFront;
    private final int front;
    private final boolean hasPainting;
    private final int painting;
    private final boolean hasMuffled;
    private final boolean muffled;
    private final boolean hasTransformer;
    private final boolean inverted;
    private final boolean hasActiveOutput;
    private final int outItems;
    private final int outFluids;
    private final boolean autoItems;
    private final boolean autoFluids;
    private final boolean allowItems;
    private final boolean allowFluids;

    private MachineConfig(boolean hasFront, int front, boolean hasPainting, int painting, boolean hasMuffled,
            boolean muffled, boolean hasTransformer, boolean inverted, boolean hasActiveOutput, int outItems,
            int outFluids, boolean autoItems, boolean autoFluids, boolean allowItems, boolean allowFluids) {
        this.hasFront = hasFront;
        this.front = front;
        this.hasPainting = hasPainting;
        this.painting = painting;
        this.hasMuffled = hasMuffled;
        this.muffled = muffled;
        this.hasTransformer = hasTransformer;
        this.inverted = inverted;
        this.hasActiveOutput = hasActiveOutput;
        this.outItems = outItems;
        this.outFluids = outFluids;
        this.autoItems = autoItems;
        this.autoFluids = autoFluids;
        this.allowItems = allowItems;
        this.allowFluids = allowFluids;
    }

    /** 从机器上采集配置（shift + 右键机器时调用）。 */
    public static MachineConfig capture(MetaTileEntity mte) {
        boolean hasFront = mte.hasFrontFacing();
        int front = hasFront ? mte.getFrontFacing().getIndex() : 0;

        boolean hasPainting = mte.isPainted();
        int painting = mte.getPaintingColor();
        boolean muffled = mte.isMuffled();

        boolean hasTransformer = mte instanceof MetaTileEntityTransformer;
        boolean inverted = hasTransformer && ((MetaTileEntityTransformer) mte).isInverted();

        boolean hasOutput = mte instanceof SimpleMachineMetaTileEntity;
        int outItems = 0;
        int outFluids = 0;
        boolean autoItems = false;
        boolean autoFluids = false;
        boolean allowItems = false;
        boolean allowFluids = false;
        if (hasOutput) {
            SimpleMachineMetaTileEntity machine = (SimpleMachineMetaTileEntity) mte;
            outItems = machine.getOutputFacingItems().getIndex();
            outFluids = machine.getOutputFacingFluids().getIndex();
            autoItems = machine.isAutoOutputItems();
            autoFluids = machine.isAutoOutputFluids();
            allowItems = machine.isAllowInputFromOutputSideItems();
            allowFluids = machine.isAllowInputFromOutputSideFluids();
        }

        return new MachineConfig(hasFront, front, hasPainting, painting, true, muffled, hasTransformer, inverted,
                hasOutput, outItems, outFluids, autoItems, autoFluids, allowItems, allowFluids);
    }

    /**
     * 把配置写回机器（普通右键粘贴时调用）。
     *
     * <p>
     * 全部通过机器的公开 setter 完成，<b>不会</b>把 NBT 直接塞进机器 —— 这样 GT 自己的
     * 客户端同步（{@code writeCustomData}）与 {@code notifyBlockUpdate()} 都会照常执行。
     * </p>
     */
    public void apply(MetaTileEntity mte) {
        // 1) 正面朝向
        if (hasFront && mte.hasFrontFacing()) {
            EnumFacing facing = EnumFacing.VALUES[clampIndex(front)];
            if (mte.getFrontFacing() != facing && mte.isValidFrontFacing(facing)) {
                mte.setFrontFacing(facing);
            }
        }

        // 2) 喷漆颜色
        if (hasPainting) {
            mte.setPaintingColor(painting);
        }

        // 3) 音效（GT 只有开关，没有 setter）
        if (hasMuffled && mte.isMuffled() != muffled) {
            mte.toggleMuffled();
        }

        // 4) 变压器升/降压
        if (hasTransformer && mte instanceof MetaTileEntityTransformer) {
            MetaTileEntityTransformer transformer = (MetaTileEntityTransformer) mte;
            if (transformer.isInverted() != inverted) {
                transformer.setTransformUp(inverted);
            }
        }

        // 5) 输出面 / 自动输出 / 输出口输入限制
        if (hasActiveOutput && mte instanceof SimpleMachineMetaTileEntity) {
            SimpleMachineMetaTileEntity machine = (SimpleMachineMetaTileEntity) mte;
            machine.setOutputFacingItems(EnumFacing.VALUES[clampIndex(outItems)]);
            machine.setOutputFacingFluids(EnumFacing.VALUES[clampIndex(outFluids)]);
            machine.setAutoOutputItems(autoItems);
            machine.setAutoOutputFluids(autoFluids);
            machine.setAllowInputFromOutputSideItems(allowItems);
            machine.setAllowInputFromOutputSideFluids(allowFluids);
        }
    }

    /** 是否什么都没采集到（用于判断"配置器里没有可用配置"）。 */
    public boolean isEmpty() {
        return !hasFront && !hasPainting && !hasTransformer && !hasActiveOutput;
    }

    public boolean hasActiveOutputConfig() {
        return hasActiveOutput;
    }

    private static int clampIndex(int index) {
        return Math.max(0, Math.min(EnumFacing.VALUES.length - 1, index));
    }

    // ------------------------------------------------------------------ NBT

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(NBT_HAS_FRONT, hasFront);
        tag.setInteger(NBT_FRONT, front);
        tag.setBoolean(NBT_HAS_PAINTING, hasPainting);
        tag.setInteger(NBT_PAINTING, painting);
        tag.setBoolean(NBT_HAS_MUFFLED, hasMuffled);
        tag.setBoolean(NBT_MUFFLED, muffled);
        tag.setBoolean(NBT_HAS_TRANSFORMER, hasTransformer);
        tag.setBoolean(NBT_INVERTED, inverted);
        tag.setBoolean(NBT_HAS_OUTPUT, hasActiveOutput);
        tag.setInteger(NBT_OUT_ITEMS, outItems);
        tag.setInteger(NBT_OUT_FLUIDS, outFluids);
        tag.setBoolean(NBT_AUTO_ITEMS, autoItems);
        tag.setBoolean(NBT_AUTO_FLUIDS, autoFluids);
        tag.setBoolean(NBT_ALLOW_ITEMS, allowItems);
        tag.setBoolean(NBT_ALLOW_FLUIDS, allowFluids);
        return tag;
    }

    public static MachineConfig readFromNBT(NBTTagCompound tag) {
        return new MachineConfig(
                tag.getBoolean(NBT_HAS_FRONT), tag.getInteger(NBT_FRONT),
                tag.getBoolean(NBT_HAS_PAINTING), tag.getInteger(NBT_PAINTING),
                tag.getBoolean(NBT_HAS_MUFFLED), tag.getBoolean(NBT_MUFFLED),
                tag.getBoolean(NBT_HAS_TRANSFORMER), tag.getBoolean(NBT_INVERTED),
                tag.getBoolean(NBT_HAS_OUTPUT), tag.getInteger(NBT_OUT_ITEMS), tag.getInteger(NBT_OUT_FLUIDS),
                tag.getBoolean(NBT_AUTO_ITEMS), tag.getBoolean(NBT_AUTO_FLUIDS),
                tag.getBoolean(NBT_ALLOW_ITEMS), tag.getBoolean(NBT_ALLOW_FLUIDS));
    }
}
