package com.susy.plusplus.machine;

import com.susy.plusplus.config.SuConfig;

import gregtech.api.GTValues;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.ModularUI.Builder;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.SimpleTextWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.TieredMetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 「存储检测器」Storage Scanner（MV 单方块机器）。
 *
 * <h2>干什么</h2>
 *
 * <ul>
 * <li>以自身为中心，在「边长³」（3~20，默认 5）的正方体区域内<b>周期扫描</b>所有
 * <b>带物品库存能力</b>的方块实体（箱子、熔炉、GT 机器、其它 mod 容器…）；</li>
 * <li>把结果（容器坐标列表）持久化到机器 NBT；</li>
 * <li>把这些容器的库存<b>聚合成一个可读写库存</b>对外暴露（漏斗/管道/机械臂可直接取放），
 * 界面里就是<b>一格一格的真实物品</b>，点击即可取走、也能放回。</li>
 * </ul>
 *
 * <h2>实现路线：直接套用 GT 工作台的"存储空间"</h2>
 *
 * <p>
 * 对外库存用的是 GT 自己的 {@link ItemHandlerList} —— GT 的工作台
 * （{@code MetaTileEntityWorkbench}）正是用它把「自己的库存 + 周围容器的库存」合成
 * 一个可读写库存（{@code connectedInventory}）。好处：
 * </p>
 *
 * <ul>
 * <li>槽位里是<b>真实物品</b>：{@code getStackInSlot} 直读目标容器，
 * {@code setStackInSlot/insertItem/extractItem} 直写目标容器；</li>
 * <li>对自动化（漏斗 / 管道 / 机械臂）完全可用，取放都会落到真实容器；</li>
 * <li>不再需要自造的只读视图、虚拟槽位映射表一类的胶水代码。</li>
 * </ul>
 *
 * <h2>与 RFTools 存储检测器的差异（按需求）</h2>
 *
 * <ol>
 * <li><b>能扫到非玩家放置的容器</b>：MC/GT 原生<b>不记录</b>方块放置者（GT 只有
 * {@code MetaTileEntity} 自己的 owner，与本功能无关），所以"非玩家放置"天然包含；</li>
 * <li>是<b>独立机器方块</b>，不是覆盖板；</li>
 * <li>既是"可被漏斗抽取的库存"，也能在 GUI 里<b>直接点击取物</b>。</li>
 * </ol>
 *
 * <h2>能量</h2>
 *
 * <ul>
 * <li>基类 {@link TieredMetaTileEntity} 自动建立<b>接收型</b>能量容器：
 * 输入电压 {@code GTValues.V[tier]}（MV = 128 EU/t），内部缓冲 8192 EU；</li>
 * <li><b>默认耗电 {@link #EU_PER_TICK} = 120 EU/t</b>：开机且供电充足时每 tick 扣；
 * 它同时覆盖"维持库存暴露"与"每 {@link #SCAN_PERIOD} tick 一次的扫描"，不再额外收扫描费；</li>
 * <li>供电不足 120 EU/t → 停机（不扫描、不刷新库存），状态行显示"缺电"。</li>
 * </ul>
 */
public class StorageScannerMachine extends TieredMetaTileEntity implements IControllable {

    /** 扫描范围边长：最小 3（3×3×3）。 */
    public static final int MIN_RANGE = 3;

    /** 扫描范围边长：最大 20（20×20×20）。 */
    public static final int MAX_RANGE = 20;

    /** 扫描范围边长默认值（5×5×5）。 */
    public static final int DEFAULT_RANGE = 5;

    /** 自动扫描周期（tick）。 */
    public static final int SCAN_PERIOD = 20;

    /**
     * <b>默认耗电：120 EU/t</b>（开机且供电充足时每 tick 扣这么多，
     * 它同时覆盖"维持库存暴露"与"每 20 tick 一次的扫描"，不再额外收取扫描费）。
     */
    public static final long EU_PER_TICK = 120L;

    /** 最多记录多少个容器。 */
    public static final int MAX_CONTAINERS = 128;

    /** 最多暴露多少个物品槽位（跨所有容器，防止巨型区域把库存撑爆）。 */
    public static final int MAX_EXPOSED_SLOTS = 512;

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 188;

    /** 同步包 ID：工作状态。 */
    private static final int SYNC_WORKING = 101;

    /** 同步包 ID：扫描结果（范围 + 容器数）。 */
    private static final int SYNC_SCAN = 102;

    /** 已扫描容器坐标（<b>服务端权威</b>，持久化）。 */
    private final List<BlockPos> scannedContainers = new ArrayList<>();

    /** 客户端镜像：容器总数（用于 tooltip / 界面文字）。 */
    private int clientContainerTotal;

    /** 扫描范围边长（3~20）。 */
    private int scanRange = DEFAULT_RANGE;

    /** 开机 / 关机（软锤可切换，{@link IControllable}）。 */
    private boolean workingEnabled = true;

    /** 当前是否真的在工作（已开机 + 有电）。 */
    private boolean isWorking = false;

    /** 距离下次自动扫描还有多少 tick。 */
    private int scanCooldown = SCAN_PERIOD;

    /** 对外暴露的聚合库存（内部是 GT 的 {@link ItemHandlerList}）。 */
    private AggregateItemHandler aggregateHandler;

    public StorageScannerMachine(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new StorageScannerMachine(metaTileEntityId, getTier());
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        // 关键：把对外暴露的物品库存换成"聚合视图"（本机器自身不存物品）
        this.aggregateHandler = new AggregateItemHandler();
        this.itemInventory = this.aggregateHandler;
    }

    // ==========================================================================
    // 库存暴露
    // ==========================================================================

    /**
     * 覆写以<b>始终</b>暴露聚合库存（基类在 {@code getSlots() == 0} 时会返回 null）。
     * 任意面都可取放。
     */
    @Override
    public <T> T getCapability(@NotNull Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.aggregateHandler);
        }
        return super.getCapability(capability, side);
    }

    /** 已扫描容器数量（服务端权威值）。 */
    public int getScannedContainerCount() {
        return scannedContainers.size();
    }

    /** 已暴露的物品槽位总数（= 所有已扫描容器的槽位之和，受 {@link #MAX_EXPOSED_SLOTS} 限制）。 */
    public int getVirtualSlotCount() {
        return aggregateHandler == null ? 0 : aggregateHandler.getSlots();
    }

    /**
     * 解析第 {@code index} 个容器当前的 {@link IItemHandler}。
     *
     * <p>
     * 返回 {@code null} 表示该容器<b>当前不可用</b>：已被破坏 / 被移动 / 区块未加载 /
     * 能力被移除 —— 调用方按"空"处理。
     * </p>
     */
    @Nullable
    public IItemHandler getContainerHandler(int index) {
        if (index < 0 || index >= scannedContainers.size()) {
            return null;
        }
        World world = getWorld();
        if (world == null) {
            return null;
        }
        BlockPos pos = scannedContainers.get(index);
        // 区块未加载 → 不可访问
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            return null;
        }
        return findItemHandler(tileEntity);
    }

    /** 依次尝试 6 个面与 null 面，取第一个可用的物品库存处理器。 */
    @Nullable
    private static IItemHandler findItemHandler(TileEntity tileEntity) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            IItemHandler handler = getHandler(tileEntity, facing);
            if (handler != null) {
                return handler;
            }
        }
        return getHandler(tileEntity, null);
    }

    @Nullable
    private static IItemHandler getHandler(TileEntity tileEntity, @Nullable EnumFacing facing) {
        if (!tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
            return null;
        }
        IItemHandler handler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        return handler != null && handler.getSlots() > 0 ? handler : null;
    }

    /**
     * 取"后端库存身份"，用于给多方块共享库存去重。
     *
     * <p>
     * Forge 的 {@link InvWrapper}（以及 {@code SidedInvWrapper}）往往是<b>每次取能力都新建</b>的
     * 包装器，但它们包的 {@code IInventory} 是同一个对象；只比较包装器身份会失效，
     * 所以要拆到后端对象。
     * </p>
     */
    private static Object backendIdentity(IItemHandler handler) {
        if (handler instanceof InvWrapper) {
            Object inv = ((InvWrapper) handler).getInv();
            if (inv != null) {
                return inv;
            }
        }
        return handler;
    }

    /** 身份比较（刻意用 {@code ==}，避免被方块实体自定义的 equals 误导）。 */
    private static boolean containsIdentity(List<Object> list, Object value) {
        for (Object element : list) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 单个容器在聚合库存里的"解析式"句柄。
     *
     * <p>
     * 槽位数在扫描时固定（这样 {@link ItemHandlerList} 的槽位偏移是稳定的），
     * 但每次访问都会<b>重新解析</b>真实容器：容器被拆掉/区块卸载时就表现为"空/不可放入"，
     * 不会去操作一个过期的 {@code TileEntity}。
     * </p>
     */
    private final class ContainerRef implements IItemHandlerModifiable {

        private final int index;
        private final int slotCount;

        ContainerRef(int index, int slotCount) {
            this.index = index;
            this.slotCount = slotCount;
        }

        @Nullable
        private IItemHandler live() {
            return getContainerHandler(index);
        }

        @Override
        public int getSlots() {
            return slotCount;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler handler = live();
            return handler == null || slot < 0 || slot >= handler.getSlots()
                    ? ItemStack.EMPTY
                    : handler.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandler handler = live();
            if (handler instanceof IItemHandlerModifiable && slot >= 0 && slot < handler.getSlots()) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler handler = live();
            return handler == null || slot < 0 || slot >= handler.getSlots()
                    ? stack
                    : handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler handler = live();
            return handler == null || slot < 0 || slot >= handler.getSlots()
                    ? ItemStack.EMPTY
                    : handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler handler = live();
            return handler == null || slot < 0 || slot >= handler.getSlots() ? 0 : handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler handler = live();
            return handler != null && slot >= 0 && slot < handler.getSlots() && handler.isItemValid(slot, stack);
        }
    }

    // ==========================================================================
    // 扫描
    // ==========================================================================

    @Override
    public void update() {
        super.update();

        World world = getWorld();
        if (world == null || world.isRemote) {
            return;
        }

        // 耗电 120 EU/t：开机 + 电够才工作（不够就停机，状态行显示"缺电"）
        boolean canRun = workingEnabled && energyContainer.getEnergyStored() >= EU_PER_TICK;
        if (canRun) {
            energyContainer.removeEnergy(EU_PER_TICK);
            if (--scanCooldown <= 0) {
                scanCooldown = SCAN_PERIOD;
                scanNow();
            }
        }

        if (canRun != isWorking) {
            this.isWorking = canRun;
            writeCustomData(SYNC_WORKING, buffer -> buffer.writeBoolean(canRun));
        }
    }

    /**
     * 执行一次扫描并持久化结果。
     *
     * <p>
     * 只扫描<b>已加载区块</b>里的方块实体；任何"带物品库存能力"的方块实体都算，
     * <b>不区分是否玩家放置</b>（MC/GT 原生没有放置者概念）。
     * </p>
     */
    public void scanNow() {
        World world = getWorld();
        BlockPos center = getPos();
        if (world == null || center == null || world.isRemote) {
            return;
        }

        // 边长 scanRange 的正方体，尽量以机器为中心（边长 5 → ±2）
        int lowHalf = (scanRange - 1) / 2;
        int highHalf = scanRange / 2;
        BlockPos min = center.add(-lowHalf, -lowHalf, -lowHalf);
        BlockPos max = center.add(highHalf, highHalf, highHalf);

        List<BlockPos> found = new ArrayList<>();
        List<Object> seenBackends = new ArrayList<>();
        for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
            if (found.size() >= MAX_CONTAINERS) {
                break;
            }
            if (pos.getX() == center.getX() && pos.getY() == center.getY() && pos.getZ() == center.getZ()) {
                continue; // 跳过自己（否则会自引用）
            }
            if (!world.isBlockLoaded(pos)) {
                continue; // 只扫已加载区块
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity == null || tileEntity == getHolder()) {
                continue;
            }
            IItemHandler handler = findItemHandler(tileEntity);
            if (handler == null) {
                continue; // 不是容器
            }
            // 去重：多方块存储（例如工业复兴的 storage rank）会让【多个方块实体返回同一份库存】，
            // 逐个计入就会把同一批物品重复算 N 倍。这里按"后端库存身份"判等，只算一次。
            if (SuConfig.storageScannerDedupeMultiblockStorage) {
                Object backend = backendIdentity(handler);
                if (containsIdentity(seenBackends, backend)) {
                    continue;
                }
                seenBackends.add(backend);
            }
            found.add(pos.toImmutable());
        }

        this.scannedContainers.clear();
        this.scannedContainers.addAll(found);
        rebuildInventory();
        markDirty();
        syncScanData();
    }

    /**
     * 按 {@link #scannedContainers} 重新装配聚合库存
     * （一个已扫描容器 = 一个 {@link ContainerRef} 子处理器）。
     */
    private void rebuildInventory() {
        List<IItemHandler> refs = new ArrayList<>();
        int total = 0;
        for (int index = 0; index < scannedContainers.size(); index++) {
            IItemHandler handler = getContainerHandler(index);
            int slots = handler == null ? 0 : handler.getSlots();
            if (slots <= 0) {
                continue;
            }
            if (total + slots > MAX_EXPOSED_SLOTS) {
                break; // 到上限就停止继续拼接
            }
            refs.add(new ContainerRef(index, slots));
            total += slots;
        }
        this.aggregateHandler.setList(new ItemHandlerList(refs));
    }

    // ==========================================================================
    // 范围 / 开关 / 翻页
    // ==========================================================================

    public int getScanRange() {
        return scanRange;
    }

    /** 调整扫描范围（界面 -1 / +1 按钮）。 */
    public void adjustScanRange(int amount) {
        int next = MathHelper.clamp(scanRange + amount, MIN_RANGE, MAX_RANGE);
        if (next == scanRange) {
            return;
        }
        this.scanRange = next;
        markDirty();
        syncScanData();
    }

    @Override
    public boolean isWorkingEnabled() {
        return workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
        markDirty();
    }

    /** 界面按钮：切换开机 / 关机。 */
    public void toggleWorking() {
        setWorkingEnabled(!workingEnabled);
    }

    // ==========================================================================
    // 同步
    // ==========================================================================

    private void syncScanData() {
        writeCustomData(SYNC_SCAN, this::writeScanData);
    }

    private void writeScanData(PacketBuffer buffer) {
        buffer.writeVarInt(scanRange);
        buffer.writeVarInt(scannedContainers.size());
    }

    private void readScanData(PacketBuffer buffer) {
        this.scanRange = buffer.readVarInt();
        this.clientContainerTotal = buffer.readVarInt();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buffer) {
        super.writeInitialSyncData(buffer);
        buffer.writeBoolean(isWorking);
        buffer.writeBoolean(workingEnabled);
        writeScanData(buffer);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buffer) {
        super.receiveInitialSyncData(buffer);
        this.isWorking = buffer.readBoolean();
        this.workingEnabled = buffer.readBoolean();
        readScanData(buffer);
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buffer) {
        super.receiveCustomData(dataId, buffer);
        if (dataId == SYNC_WORKING) {
            this.isWorking = buffer.readBoolean();
            scheduleRenderUpdate();
        } else if (dataId == SYNC_SCAN) {
            readScanData(buffer);
        }
    }

    // ==========================================================================
    // 持久化
    // ==========================================================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("ScanRange", scanRange);
        data.setBoolean("WorkingEnabled", workingEnabled);
        NBTTagList list = new NBTTagList();
        for (BlockPos pos : scannedContainers) {
            // 用 {x,y,z} 三个 int：1.12.2 的 NBTTagList 没有 getLongAt()，
            // 而 getCompoundTagAt() 是稳定可用的读法（也便于人工查看存档）。
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.getX());
            entry.setInteger("y", pos.getY());
            entry.setInteger("z", pos.getZ());
            list.appendTag(entry);
        }
        data.setTag("ScannedContainers", list);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.scanRange = MathHelper.clamp(
                data.hasKey("ScanRange") ? data.getInteger("ScanRange") : DEFAULT_RANGE, MIN_RANGE, MAX_RANGE);
        this.workingEnabled = !data.hasKey("WorkingEnabled") || data.getBoolean("WorkingEnabled");
        this.scannedContainers.clear();
        NBTTagList list = data.getTagList("ScannedContainers", 10); // 10 = NBTTagCompound
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            scannedContainers.add(new BlockPos(entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z")));
        }
        this.scanCooldown = SCAN_PERIOD;
        // 按保存下来的容器列表重建聚合库存（容器内容/槽位数在加载后即可读取）
        rebuildInventory();
    }

    // ==========================================================================
    // 渲染 / 界面
    // ==========================================================================

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        // 正面一个"输出口"覆盖层：表示这里可以取放物品
        Textures.PIPE_OUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    /**
     * 界面（GT 旧 GUI）。
     *
     * <p>
     * 按需求<b>不显示容器的物品槽位</b>：界面只提供范围调整 / 开关机 / 状态与统计；
     * 物品的取放请通过漏斗、管道等自动化（机器的聚合库存仍然照常对外提供）。
     * </p>
     */
    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, GUI_WIDTH, GUI_HEIGHT)
                .label(10, 5, getMetaFullName());

        // 扫描范围：-1 / +1
        builder.widget(new ClickButtonWidget(7, 18, 20, 20, "-1", data -> adjustScanRange(-1)));
        builder.widget(new ClickButtonWidget(149, 18, 20, 20, "+1", data -> adjustScanRange(1)));
        builder.widget(new ImageWidget(29, 18, 118, 20, GuiTextures.DISPLAY));
        builder.widget(new SimpleTextWidget(88, 23, "susyplusplus.gui.storage_scanner.range", 0xFFFFFF,
                () -> Integer.toString(scanRange)));

        // 开机-关机（"手动扫描"按钮已按要求移除：扫描每 SCAN_PERIOD tick 自动进行）
        builder.widget(new ClickButtonWidget(7, 42, 162, 20, I18n.format("susyplusplus.gui.storage_scanner.toggle"),
                data -> toggleWorking()));

        // 运行状态（供给器只返回"键名"，由客户端翻译）
        builder.widget(new ImageWidget(7, 64, 162, 18, GuiTextures.DISPLAY));
        builder.widget(new SimpleTextWidget(88, 69, "", 0xFFFFFF, this::stateKey));

        // 统计：容器数 · 物品槽位数
        builder.widget(new ImageWidget(7, 84, 162, 18, GuiTextures.DISPLAY));
        builder.widget(new SimpleTextWidget(88, 89, "susyplusplus.gui.storage_scanner.counts", 0xFFFFFF,
                () -> getScannedContainerCount() + " · " + getVirtualSlotCount()));

        // 界面里不再显示容器的物品槽位（按需求移除）：物品取放走漏斗/管道等自动化
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7, 106);
        return builder.build(getHolder(), entityPlayer);
    }

    /**
     * 运行状态的<b>键名</b>（服务端求值安全：不触碰 I18n）。
     *
     * <p>
     * 顺带解释"格子里为什么没有物品"：关机 / 缺电 / 范围内扫不到容器，
     * 都会在这里给出明确原因。
     * </p>
     */
    private String stateKey() {
        if (!workingEnabled) {
            return "susyplusplus.gui.storage_scanner.state.disabled";
        }
        if (!isWorking) {
            return "susyplusplus.gui.storage_scanner.state.no_power";
        }
        if (getVirtualSlotCount() == 0) {
            return "susyplusplus.gui.storage_scanner.state.no_container";
        }
        return "susyplusplus.gui.storage_scanner.state.running";
    }

    // ==========================================================================
    // tooltip
    // ==========================================================================

    /** 展示用的容器数（客户端用同步镜像）。 */
    private int getDisplayContainerCount() {
        World world = getWorld();
        if (world != null && world.isRemote) {
            return clientContainerTotal;
        }
        return scannedContainers.size();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.universal.tooltip.max_voltage_in", energyContainer.getInputVoltage(),
                GTValues.VNF[getTier()]));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity",
                energyContainer.getEnergyCapacity()));
        tooltip.add(I18n.format("gregtech.universal.tooltip.uses_per_tick", EU_PER_TICK));
        tooltip.add(I18n.format("susyplusplus.machine.storage_scanner.tooltip.range", scanRange, MIN_RANGE, MAX_RANGE));
        tooltip.add(I18n.format("susyplusplus.machine.storage_scanner.tooltip.containers", getDisplayContainerCount()));
    }

    @Override
    public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.tool_action.screwdriver.access_covers"));
        tooltip.add(I18n.format("gregtech.tool_action.wrench.set_facing"));
        super.addToolUsages(stack, world, tooltip, advanced);
    }
}
