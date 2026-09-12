package com.susy.plusplus.multiblock.wireless;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IElectricItem;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.blocks.StoneVariantBlock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

import supersymmetry.common.item.SuSyMetaItems;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无线能量传输塔（Wireless Energy Transmission Tower）。
 *
 * <p>
 * 注册名 {@code susyplusplus:wireless_energy_tower}，本地化键
 * {@code susyplusplus.machine.wireless_energy_tower.name}
 * （由 {@code MetaTileEntity#getMetaName()} 推导）。
 * </p>
 *
 * <h2>玩法</h2>
 * <ol>
 * <li>用<b>铁砧</b>把 Susy-Core 的货运无人机重命名为坐标（例如 {@code 120 64 -350}），
 * 放进输入总线 —— 物品名就是坐标，控制器直接读取
 * {@link ItemStack#getDisplayName()}（即 {@code display.Name} NBT）。</li>
 * <li>输入总线里另外放电池（GT 的 {@link IElectricItem} 电池）：<b>电池总容量 = 主方块电量上限</b>
 * （最多 {@value #MAX_BATTERIES} 个），<b>传输电压 = 最低电池电压</b>。</li>
 * <li>主方块自身有电量（GUI 中显示）：由<b>能源仓</b>从电网充入，<b>以及</b>总线里的电池放电充入。</li>
 * <li>每 {@value #SCAN_INTERVAL} tick 重扫一次总线（电池数 / 无人机数）。</li>
 * <li>某目标机器电量低于其最大电量的 50% 时开始一次传输：时长
 * {@code ceil((5 + 欧氏距离/5) * 20)} tick；<b>只在最后 3 秒</b>
 * （{@value #INJECT_WINDOW} tick）才每 tick 输出
 * {@code 传输电压 × 电池个数 × }{@value #OUTPUT_MULTIPLIER}（EU/t），
 * 再受主方块电量与目标剩余空间限制。</li>
 * </ol>
 *
 * <h2>结构（5 层 × 每层 5 行 × 每行 9 字符）</h2>
 * 
 * <pre>
 * y=0（最底层）              y=1/2/3（中间三层，完全相同）   y=4（最顶层）
 *   R # # # R # # # R           # # # # # # # # #              R # # # R # # # R
 *   R # # # R # # # R           # # # # # # # # #              R # # # R # # # R
 *   R R R R R R R R R           R X X X R X X X R              R R R R R R R R R
 *   R R R R R # # # R           R # # # R # # # #              R R S R R # # # R
 *   # R R R # # # # #           R R R R R # # # #              # R R R # # # # #
 * </pre>
 *
 * <p>
 * {@code R} = 脱氧钢机械方块（{@code BlockMetalCasing.MetalCasingType.STEEL_SOLID}，
 * 中文“脱氧钢机械方块”）或仓室；
 * {@code X} =
 * 淡色混凝土（{@code StoneType.CONCRETE_LIGHT}，{@code StoneVariant.SMOOTH}）；
 * {@code S} = 控制器（顶层第 4 行第 3 列）；{@code #} = 空气。
 * 第一个 {@code aisle()} 是最底层，最后一个 {@code aisle()} 是最顶层。
 * </p>
 *
 * <p>
 * <b>不耗电</b>：没有配方逻辑，机器运行本身不消耗 EU（只有它自己存的那份电在传输时减少）。
 * <b>需要维护</b>：沿用 {@link MultiblockWithDisplayBase} 的默认维护机制
 * （结构里需要 1 个维护仓）。
 * </p>
 *
 * <p>
 * <b>关于 UI API</b>：此处使用的是本模组编译/运行所依赖的 GTCEu 版本的
 * {@code addDisplayText / addErrorText / addWarningText(List<ITextComponent>)}，
 * 而非更新版 master 源码里的 {@code MultiblockUIBuilder}（那个类在 jar 里并不存在）。
 * </p>
 */
public class MetaTileEntityWirelessEnergyTower extends MultiblockWithDisplayBase {

    /** 输入总线里最多计入容量 / 检出的电池数量。 */
    public static final int MAX_BATTERIES = 16;

    /** 最多标记无人机（= 最多目标）数量。 */
    public static final int MAX_TARGETS = 9;

    /** 扫描总线的间隔（tick）。 */
    public static final int SCAN_INTERVAL = 20;

    /** 只在传输的**最后 3 秒**才输出能量（60 tick）。 */
    public static final int INJECT_WINDOW = 60;

    /** 输出倍率：每 tick 输出 = 传输电压 × 电池个数 × {@value #OUTPUT_MULTIPLIER}。 */
    public static final long OUTPUT_MULTIPLIER = 64L;

    /** 传输基础时长：5 秒。 */
    private static final double BASE_SECONDS = 5.0D;

    /** 每 5 格距离增加 1 秒。 */
    private static final double BLOCKS_PER_SECOND = 5.0D;

    private static final String NBT_ENERGY = "TowerEnergyStored";

    /**
     * 从物品名里抽取整数：
     * 支持 {@code "12 64 -30"}、{@code "12,64,-30"}、{@code "x12 y64 z-30"} 等写法。
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

    /** Susy-Core 的三种货运无人机（懒加载）。 */
    private static ItemStack[] droneForms;

    /** 输入能源仓的聚合容器。 */
    private EnergyContainerList inputEnergyHatches;

    /** 主方块 GUI 里的电量。 */
    private long energyStored;

    /** 主方块电量上限 = 输入总线内电池的总容量（最多 {@value #MAX_BATTERIES} 个）。 */
    private long energyCapacity;

    /** 传输电压 = 最低电池电压（{@code GTValues.V[tier]}）。 */
    private long transferVoltage;

    /** 当前计入容量的电池数量（也决定每秒输出多少个「电压包」）。 */
    private int batteryCount;

    /** 总线上的无人机总数（含未标记的）。 */
    private int droneCount;

    /** 名称无法解析出坐标的无人机数量。 */
    private int invalidNameCount;

    /** 解析出了坐标、但那里没有可充电机器的目标数量。 */
    private int offlineTargetCount;

    /** 正在传输的目标数量。 */
    private int activeTargetCount;

    /** 扫描计时（不依赖 {@code getOffsetTimer()}，避免 API 版本差异）。 */
    private int scanTimer;

    /** 有效目标（由标记无人机解析而来，最多 {@value #MAX_TARGETS} 个）。 */
    private final List<WirelessTowerTarget> targets = new ArrayList<>();

    /** 是否有任一目标正在传输（服务端权威值）。 */
    private boolean working;

    public MetaTileEntityWirelessEnergyTower(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityWirelessEnergyTower(this.metaTileEntityId);
    }

    // ==========================================================================
    // 结构
    // ==========================================================================

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("R###R###R", "R###R###R", "RRRRRRRRR", "RRRRR###R", "#RRR#####")
                .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
                .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
                .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
                .aisle("R###R###R", "R###R###R", "RRRRRRRRR", "RRSRR###R", "#RRR#####")
                .where('R', casingOrHatchPredicate())
                .where('X', states(concreteState()))
                .where('#', air())
                .where('S', selfPredicate())
                .build();
    }

    /** 淡色混凝土（X）。 */
    private static IBlockState concreteState() {
        return MetaBlocks.STONE_BLOCKS.get(StoneVariantBlock.StoneVariant.SMOOTH)
                .getState(StoneVariantBlock.StoneType.CONCRETE_LIGHT);
    }

    /** 脱氧钢机械方块（R）。 */
    private static IBlockState casingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID);
    }

    /**
     * R 的判定：脱氧钢机械方块，或以下仓室。
     *
     * <ul>
     * <li>{@code MAINTENANCE_HATCH} —— 本机需要维护</li>
     * <li>{@code IMPORT_ITEMS} —— 放标记无人机与电池（最多 4 个，共 16 格）</li>
     * <li>{@code INPUT_ENERGY} —— 从电网给主方块充电（可选，也可只用电池）</li>
     * </ul>
     */
    private TraceabilityPredicate casingOrHatchPredicate() {
        return states(casingState())
                .or(maintenancePredicate())
                .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(4))
                .or(abilities(MultiblockAbility.INPUT_ENERGY).setMaxGlobalLimited(2));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.inputEnergyHatches = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.targets.clear();
        rescanInputBus();
    }

    @Override
    public void invalidateStructure() {
        this.inputEnergyHatches = null;
        this.targets.clear();
        this.batteryCount = 0;
        this.droneCount = 0;
        this.invalidNameCount = 0;
        this.offlineTargetCount = 0;
        this.activeTargetCount = 0;
        this.energyCapacity = 0L;
        this.transferVoltage = 0L;
        this.working = false;
        // 刻意保留 energyStored：拆掉结构重搭时不该吞掉塔里的电。
        super.invalidateStructure();
    }

    // ==========================================================================
    // 每 tick 逻辑
    // ==========================================================================

    @Override
    protected void updateFormedValid() {
        if (getWorld().isRemote) {
            return;
        }

        // 每 20 tick 重扫总线，并把电池的电放进来
        if (++this.scanTimer >= SCAN_INTERVAL) {
            this.scanTimer = 0;
            rescanInputBus();
            drainBatteriesIntoBuffer();
        }

        chargeFromEnergyHatches();
        tickTargets();
    }

    /** 扫描输入总线：统计电池（数量 / 总容量 / 最低电压）与标记无人机（坐标目标）。 */
    private void rescanInputBus() {
        this.batteryCount = 0;
        this.droneCount = 0;
        this.invalidNameCount = 0;

        long capacity = 0L;
        int lowestTier = Integer.MAX_VALUE;
        List<WirelessTowerTarget> resolved = new ArrayList<>();

        for (IItemHandlerModifiable bus : getAbilities(MultiblockAbility.IMPORT_ITEMS)) {
            for (int slot = 0; slot < bus.getSlots(); slot++) {
                ItemStack stack = bus.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                if (isDrone(stack)) {
                    this.droneCount++;
                    BlockPos pos = parseCoordinates(stack);
                    if (pos == null) {
                        // 未标记（名称里凑不出 x y z）：算作无效目标
                        this.invalidNameCount++;
                        continue;
                    }
                    if (resolved.size() < MAX_TARGETS) {
                        resolved.add(resolveTarget(pos, resolved));
                    }
                    continue;
                }

                IElectricItem electricItem = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
                if (electricItem == null || !electricItem.canProvideChargeExternally()) {
                    continue;
                }
                if (this.batteryCount < MAX_BATTERIES) {
                    capacity += electricItem.getMaxCharge();
                    lowestTier = Math.min(lowestTier, electricItem.getTier());
                }
                this.batteryCount++;
            }
        }

        this.energyCapacity = capacity;
        this.transferVoltage = lowestTier == Integer.MAX_VALUE ? 0L
                : GTValues.V[Math.max(0, Math.min(GTValues.V.length - 1, lowestTier))];
        if (this.energyStored > this.energyCapacity) {
            this.energyStored = this.energyCapacity;
        }

        this.targets.clear();
        this.targets.addAll(resolved);
    }

    /** 按坐标复用旧目标对象（保留传输进度），找不到就新建。 */
    private WirelessTowerTarget resolveTarget(BlockPos pos, List<WirelessTowerTarget> resolved) {
        for (WirelessTowerTarget previous : this.targets) {
            if (previous.pos.equals(pos) && !resolved.contains(previous)) {
                return previous;
            }
        }
        return new WirelessTowerTarget(pos);
    }

    /** 能源仓供电：把电网里的电搬进主方块电量。 */
    private void chargeFromEnergyHatches() {
        if (this.inputEnergyHatches == null || this.energyStored >= this.energyCapacity) {
            return;
        }
        long room = this.energyCapacity - this.energyStored;
        long request = Math.min(room, this.inputEnergyHatches.getEnergyStored());
        if (request <= 0L) {
            return;
        }
        long changed = -this.inputEnergyHatches.changeEnergy(-request);
        if (changed > 0L) {
            this.energyStored += Math.min(changed, room);
            markDirty();
        }
    }

    /** 总线里的电池放电充入主方块电量（每 {@value #SCAN_INTERVAL} tick 一次，避免频繁写 NBT）。 */
    private void drainBatteriesIntoBuffer() {
        if (this.energyStored >= this.energyCapacity) {
            return;
        }
        for (IItemHandlerModifiable bus : getAbilities(MultiblockAbility.IMPORT_ITEMS)) {
            for (int slot = 0; slot < bus.getSlots(); slot++) {
                if (this.energyStored >= this.energyCapacity) {
                    return;
                }
                ItemStack stack = bus.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                IElectricItem electricItem = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
                if (electricItem == null || !electricItem.canProvideChargeExternally()) {
                    continue;
                }
                long room = this.energyCapacity - this.energyStored;
                // externally = true：只有“电池类”物品才会被外部放电
                long drained = electricItem.discharge(room, Integer.MAX_VALUE, true, true, false);
                if (drained > 0L) {
                    this.energyStored += drained;
                    markDirty();
                }
            }
        }
    }

    /** 推进每个目标的传输计时；传输期间每 tick 持续输出能量。 */
    private void tickTargets() {
        if (this.targets.isEmpty()) {
            this.activeTargetCount = 0;
            this.offlineTargetCount = 0;
            setWorking(false);
            return;
        }

        int active = 0;
        int offline = 0;
        // 最后 3 秒的持续输出功率：传输电压 × 电池个数 × 64（EU/t）
        long perTick = getOutputPerTick();

        for (WirelessTowerTarget target : this.targets) {
            if (target.isTransferring()) {
                // 只在最后 3 秒（INJECT_WINDOW tick）持续输出
                if (perTick > 0L && target.progress <= INJECT_WINDOW) {
                    pushEnergy(target, perTick);
                }
                target.progress--;
                if (target.progress > 0) {
                    active++;
                }
                continue;
            }

            EnergyHandle handle = findEnergyHandle(target.pos);
            if (handle == null || handle.container.getEnergyCapacity() <= 0L) {
                target.invalid = true;
                offline++;
                continue;
            }
            target.invalid = false;

            long capacity = handle.container.getEnergyCapacity();
            long stored = handle.container.getEnergyStored();
            // 目标电量不足一半才开始工作
            if (stored * 2 >= capacity) {
                continue;
            }
            // 没有电池（电压/电池数为 0）时不空转
            if (perTick <= 0L) {
                continue;
            }

            target.workTime = getWorkTime(target.pos);
            target.progress = target.workTime;
            target.lastInjected = 0L;
            active++;
        }

        this.activeTargetCount = active;
        this.offlineTargetCount = offline;
        setWorking(active > 0);
    }

    /**
     * 向目标持续输出：每 tick 送出
     * {@code 传输电压 × 电池个数 × }{@value #OUTPUT_MULTIPLIER}（EU/t）。
     *
     * <p>
     * 实际送出量再受两个上限约束：<b>主方块当前电量</b>与<b>目标还能接收的量</b>。
     * </p>
     */
    private void pushEnergy(WirelessTowerTarget target, long perTick) {
        if (perTick <= 0L || this.energyStored <= 0L) {
            return;
        }
        EnergyHandle handle = findEnergyHandle(target.pos);
        if (handle == null) {
            target.invalid = true;
            return;
        }
        long room = handle.container.getEnergyCanBeInserted();
        if (room <= 0L) {
            return;
        }
        long amount = Math.min(Math.min(perTick, room), this.energyStored);
        long sent = transferEnergy(handle, amount);
        if (sent > 0L) {
            this.energyStored -= sent;
            target.lastInjected += sent;
            markDirty();
        }
    }

    /**
     * 以「传输电压」（最低电池电压）向目标注入能量。
     *
     * <p>
     * 用 {@link IEnergyContainer#acceptEnergyFromNetwork(EnumFacing, long, long)}
     * 而不是
     * {@code addEnergy}：电压/电流都交给目标机器自己的判定，这样「传输电压 = 最低电池电压」
     * 才有实际意义（电压高于目标输入电压时，对方会拒绝而不是被强塞）。
     * </p>
     */
    private long transferEnergy(EnergyHandle handle, long amount) {
        long voltage = Math.max(1L, this.transferVoltage);
        long injected = 0L;
        for (int guard = 0; guard < 64 && injected < amount; guard++) {
            long amperage = Math.max(1L, (amount - injected) / voltage);
            long inputAmperage = handle.container.getInputAmperage();
            if (inputAmperage > 0L) {
                amperage = Math.min(amperage, inputAmperage);
            }
            long used = handle.container.acceptEnergyFromNetwork(handle.side, voltage, amperage);
            if (used <= 0L) {
                break;
            }
            injected += used * voltage;
        }
        return Math.min(injected, amount);
    }

    /** 传输时长 = {@code ceil((5 + 欧氏距离/5) * 20)} tick。 */
    private int getWorkTime(BlockPos pos) {
        BlockPos self = getPos();
        double dx = self.getX() - pos.getX();
        double dy = self.getY() - pos.getY();
        double dz = self.getZ() - pos.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return (int) Math.ceil((BASE_SECONDS + distance / BLOCKS_PER_SECOND) * 20.0D);
    }

    /** 找到目标位置可接受能量的机器（返回容器 + 用于判定的面）。 */
    private EnergyHandle findEnergyHandle(BlockPos pos) {
        World world = getWorld();
        if (world == null || !world.isBlockLoaded(pos)) {
            return null;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            return null;
        }
        IEnergyContainer fallback = null;
        EnumFacing fallbackSide = null;
        for (EnumFacing side : EnumFacing.VALUES) {
            IEnergyContainer container = tileEntity
                    .getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, side);
            if (container == null) {
                continue;
            }
            if (container.getEnergyCanBeInserted() > 0L) {
                return new EnergyHandle(container, side);
            }
            if (fallback == null) {
                fallback = container;
                fallbackSide = side;
            }
        }
        return fallback == null ? null : new EnergyHandle(fallback, fallbackSide);
    }

    /**
     * 更新「工作中」状态，并把它同步给客户端。
     *
     * <p>
     * 关键：不能只在客户端读 {@link #working}（客户端的它永远是 false）。
     * 基类的 {@code setLastActive(...)} 会把状态写进 {@code IS_WORKING} 同步包，
     * 客户端在 {@code receiveCustomData} 里把它存进 {@link #lastActive}，
     * 于是正面 overlay 的 active 动画与 GUI 状态在两端都正确。
     * </p>
     */
    private void setWorking(boolean working) {
        if (this.working == working) {
            return;
        }
        this.working = working;
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) {
            setLastActive(working);
        }
    }

    /** 是否正在传输（两端一致：都以同步过的 {@link #lastActive} 为准）。 */
    @Override
    public boolean isActive() {
        return super.isActive() && this.lastActive;
    }

    // ==========================================================================
    // 供 GUI / TOP 读取的进度
    // ==========================================================================

    /**
     * 输出功率（EU/t）= 传输电压 × 电池个数 × {@value #OUTPUT_MULTIPLIER}。
     *
     * <p>
     * 注意：这个功率只在传输的<b>最后 3 秒</b>真的生效（见 {@link #INJECT_WINDOW}）。
     * </p>
     */
    public long getOutputPerTick() {
        return this.transferVoltage * Math.max(0, this.batteryCount) * OUTPUT_MULTIPLIER;
    }

    /** 当前是否正处在「最后 3 秒」，即真的在往外送电。 */
    public boolean isOutputting() {
        for (WirelessTowerTarget target : this.targets) {
            if (target.isTransferring() && target.progress <= INJECT_WINDOW) {
                return true;
            }
        }
        return false;
    }

    /** 当前正在传输的目标已进行的 tick 数；没有正在传输的目标时为 0。 */
    public int getTransferProgress() {
        for (WirelessTowerTarget target : this.targets) {
            if (target.isTransferring()) {
                return target.workTime - target.progress;
            }
        }
        return 0;
    }

    /** 当前正在传输的目标的总时长（tick）；没有正在传输的目标时为 0。 */
    public int getTransferWorkTime() {
        for (WirelessTowerTarget target : this.targets) {
            if (target.isTransferring()) {
                return target.workTime;
            }
        }
        return 0;
    }

    public long getStoredEnergy() {
        return this.energyStored;
    }

    public long getEnergyCapacity() {
        return this.energyCapacity;
    }

    public int getDroneCount() {
        return this.droneCount;
    }

    // ==========================================================================
    // 无人机与坐标
    // ==========================================================================

    /** 是否是 Susy-Core 的货运无人机。 */
    private static boolean isDrone(ItemStack stack) {
        if (droneForms == null) {
            droneForms = new ItemStack[] {
                    SuSyMetaItems.BASIC_CARGO_DRONE.getStackForm(),
                    SuSyMetaItems.ADVANCED_CARGO_DRONE.getStackForm(),
                    SuSyMetaItems.ELITE_CARGO_DRONE.getStackForm()
            };
        }
        for (ItemStack form : droneForms) {
            if (!form.isEmpty() && stack.isItemEqual(form)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从无人机物品名解析坐标。
     *
     * <p>
     * 铁砧改名写入的是 {@code display.Name}，{@link ItemStack#getDisplayName()} 会直接返回它，
     * 因此服务端也能读到。名称里需要出现至少 3 个整数（x y z）。
     * </p>
     *
     * @return 坐标；名称里凑不出 3 个整数时返回 {@code null}
     */
    private static BlockPos parseCoordinates(ItemStack stack) {
        if (!stack.hasDisplayName()) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(stack.getDisplayName());
        int[] values = new int[3];
        int found = 0;
        while (found < 3 && matcher.find()) {
            try {
                values[found++] = Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {
                // 超范围数字：当作没有这个数
            }
        }
        return found == 3 ? new BlockPos(values[0], values[1], values[2]) : null;
    }

    // ==========================================================================
    // GUI
    // ==========================================================================

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (!isStructureFormed()) {
            return;
        }

        textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.energy",
                formatNumbers(this.energyStored), formatNumbers(this.energyCapacity)));

        textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.batteries",
                this.batteryCount, MAX_BATTERIES));

        if (this.transferVoltage > 0L) {
            textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.voltage",
                    formatNumbers(this.transferVoltage)));
        }

        textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.targets",
                this.activeTargetCount, this.droneCount));

        int progress = getTransferProgress();
        int workTime = getTransferWorkTime();
        if (workTime > 0) {
            textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.progress",
                    progress, workTime));
            if (isOutputting()) {
                textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.output",
                        formatNumbers(getOutputPerTick())));
            }
        } else {
            textList.add(new TextComponentTranslation("susyplusplus.multiblock.wireless_energy_tower.idling"));
        }
    }

    @Override
    protected void addErrorText(List<ITextComponent> textList) {
        super.addErrorText(textList);
        if (!isStructureFormed()) {
            return;
        }

        if (this.batteryCount <= 0) {
            textList.add(redText("susyplusplus.multiblock.wireless_energy_tower.no_battery"));
        } else if (this.batteryCount > MAX_BATTERIES) {
            textList.add(redText("susyplusplus.multiblock.wireless_energy_tower.too_many_batteries", MAX_BATTERIES));
        }

        if (this.droneCount <= 0) {
            textList.add(redText("susyplusplus.multiblock.wireless_energy_tower.no_drone"));
        } else if (this.droneCount > MAX_TARGETS) {
            textList.add(redText("susyplusplus.multiblock.wireless_energy_tower.too_many_drones", MAX_TARGETS));
        }

        if (this.invalidNameCount > 0) {
            textList.add(
                    redText("susyplusplus.multiblock.wireless_energy_tower.invalid_target", this.invalidNameCount));
        }

        if (this.offlineTargetCount > 0) {
            textList.add(redText("susyplusplus.multiblock.wireless_energy_tower.target_offline",
                    this.offlineTargetCount));
        }
    }

    @Override
    protected void addWarningText(List<ITextComponent> textList) {
        super.addWarningText(textList);
        if (!isStructureFormed()) {
            return;
        }
        if (this.batteryCount > 0 && this.energyStored <= 0L) {
            ITextComponent warning = new TextComponentTranslation(
                    "susyplusplus.multiblock.wireless_energy_tower.no_energy");
            warning.getStyle().setColor(TextFormatting.YELLOW);
            textList.add(warning);
        }
    }

    /** 一行红字。 */
    private static ITextComponent redText(String langKey, Object... args) {
        ITextComponent text = new TextComponentTranslation(langKey, args);
        text.getStyle().setColor(TextFormatting.RED);
        return text;
    }

    /** 千分位数字（不依赖 GT 工具类，避免版本差异）。 */
    private static String formatNumbers(long value) {
        return String.format("%,d", value);
    }

    @Override
    protected boolean shouldShowVoidingModeButton() {
        return false;
    }

    // ==========================================================================
    // 物品提示
    // ==========================================================================

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(TextFormatting.AQUA + I18n.format("susyplusplus.machine.wireless_energy_tower.tooltip.drone"));
        tooltip.add(TextFormatting.AQUA + I18n.format("susyplusplus.machine.wireless_energy_tower.tooltip.battery"));
        tooltip.add(TextFormatting.GREEN + I18n.format("susyplusplus.machine.wireless_energy_tower.tooltip.energy"));
        tooltip.add(TextFormatting.GRAY + I18n.format("susyplusplus.machine.wireless_energy_tower.tooltip.target"));
        tooltip.add(
                TextFormatting.GRAY + I18n.format("susyplusplus.machine.wireless_energy_tower.tooltip.maintenance"));
    }

    // ==========================================================================
    // 渲染
    // ==========================================================================

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @SideOnly(Side.CLIENT)
    @NotNull
    @Override
    protected ICubeRenderer getFrontOverlay() {
        // GT 的储能变电站 overlay 自带 overlay_front / overlay_front_active，
        // 因此传输时正面会自动切换成 active 贴图（见 renderMetaTileEntity）。
        return Textures.POWER_SUBSTATION_OVERLAY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getFrontOverlay().renderOrientedState(renderState, translation, pipeline, getFrontFacing(), isActive(), true);
    }

    // ==========================================================================
    // 持久化
    // ==========================================================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setLong(NBT_ENERGY, this.energyStored);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.energyStored = data.getLong(NBT_ENERGY);
    }

    // ==========================================================================
    // 内部类型
    // ==========================================================================

    /** 一个「可注入能量的机器」：能量容器 + 找到它的那一面。 */
    private static final class EnergyHandle {

        final IEnergyContainer container;
        final EnumFacing side;

        EnergyHandle(IEnergyContainer container, EnumFacing side) {
            this.container = container;
            this.side = side;
        }
    }
}
