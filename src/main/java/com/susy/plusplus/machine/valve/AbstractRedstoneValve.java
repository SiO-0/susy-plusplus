package com.susy.plusplus.machine.valve;

import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.client.renderer.ICubeRenderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 红石控制阀门的公共基类（流体阀 / 物流阀 / IR 流体阀）。
 *
 * <h2>行为</h2>
 * <ul>
 * <li><b>默认开</b>：没有红石信号时阀门导通；<b>有红石信号时关</b>（{@link #isOpen()} 为 false）。</li>
 * <li><b>扳手右键</b> = 把被点击的面设为<b>输入面</b>；
 * <b>Shift + 扳手右键</b> = 设为<b>输出面</b>（两个面互斥，撞面时自动交换）。</li>
 * <li><b>关闭时仍然与管道相连</b>：{@code getCapability} 在输入/输出面<b>始终</b>返回一个
 * I FluidHandler / IItemHandler（关闭时是不可用实现），因此 GT 管道 / Susy-Core 无罐流体管道
 * 的连接判定（只检查 capability 是否为 null）不会把管道断开；只是「通路」被掐断。
 * 这也意味着<b>不需要</b>去改相邻管道的 {@code setConnection}。</li>
 * <li><b>无缓存</b>：阀门自己不存任何流体/物品；输入面收到的东西<b>立刻</b>转交给输出面邻居
 * （{@code fill} / {@code insertItem} 当场转发）。</li>
 * <li><b>不主动抽取</b>（IR 流体阀除外）：普通流体阀 / 物流阀依赖管道或上游机器推送。</li>
 * </ul>
 *
 * <h2>为什么关闭时返回「不可用实现」而不是 null</h2>
 * <p>
 * 本整合包 {@code config/gregtech/gregtech.cfg} 里 {@code B:gt6StylePipesCables=true}，
 * 而 GT 的 {@code BlockPipe#neighborChanged} / {@code ItemBlockPipe} 在 GT6 风格下
 * <b>完全跳过</b>「按 capability 自动连/断」的逻辑。所以返回 null 不会让管道断开，
 * 反而会让玩家手动连好的管道在重新查询时丢掉视觉连接。返回非 null 的惰性实现则两全：
 * 管道保持连接，流体/物品无法通过。
 * </p>
 */
public abstract class AbstractRedstoneValve extends MetaTileEntity {

    /** 自定义同步码（用 GT 的分配器，避免与 MTE 自带码冲突）。 */
    private static final int SYNC_VALVE_STATE = GregtechDataCodes.assignId();

    private static final String NBT_INPUT_FACE = "ValveInputFace";
    private static final String NBT_OUTPUT_FACE = "ValveOutputFace";
    private static final String NBT_OPEN = "ValveOpen";

    /** 输入面（流体/物品进入阀门的一面）。 */
    private EnumFacing inputFace = EnumFacing.NORTH;

    /** 输出面（阀门把东西送出去的一面）。 */
    private EnumFacing outputFace = EnumFacing.SOUTH;

    /** 当前是否导通（服务端权威；客户端由同步包写入）。 */
    private boolean open = true;

    protected AbstractRedstoneValve(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 阀门是否导通（无红石信号时导通）。 */
    public boolean isOpen() {
        return open;
    }

    public EnumFacing getValveInputFace() {
        return inputFace;
    }

    public EnumFacing getValveOutputFace() {
        return outputFace;
    }

    /** 只允许「输入面」接受外部写入。 */
    protected boolean isInputSide(@Nullable EnumFacing side) {
        return side != null && side == inputFace;
    }

    /** 输出面只对外暴露一个「不能写入」的占位实现（用于保持管道连接）。 */
    protected boolean isOutputSide(@Nullable EnumFacing side) {
        return side != null && side == outputFace;
    }

    private void setInputFace(EnumFacing face) {
        if (face == inputFace) {
            return;
        }
        if (face == outputFace) {
            // 撞面：与输出面交换，避免输入面和输出面重合
            this.outputFace = this.inputFace;
        }
        this.inputFace = face;
        saveAndSync();
    }

    private void setOutputFace(EnumFacing face) {
        if (face == outputFace) {
            return;
        }
        if (face == inputFace) {
            this.inputFace = this.outputFace;
        }
        this.outputFace = face;
        saveAndSync();
    }

    private void saveAndSync() {
        markDirty();
        writeValveState();
        scheduleRenderUpdate();
        notifyBlockUpdate();
    }

    /** 同步「开/关 + 输入面 + 输出面」到客户端。 */
    protected void writeValveState() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        writeCustomData(SYNC_VALVE_STATE, buf -> {
            buf.writeByte(inputFace.getIndex());
            buf.writeByte(outputFace.getIndex());
            buf.writeBoolean(open);
        });
    }

    // ------------------------------------------------------------------
    // 红石
    // ------------------------------------------------------------------

    @Override
    public void update() {
        super.update();
        if (getWorld() != null && !getWorld().isRemote) {
            refreshOpenState();
        }
    }

    @Override
    public void onNeighborChanged() {
        if (getWorld() != null && !getWorld().isRemote) {
            refreshOpenState();
        }
    }

    /** 有红石信号 → 关；无信号 → 开。 */
    private void refreshOpenState() {
        boolean newOpen = !isBlockRedstonePowered();
        if (newOpen != open) {
            open = newOpen;
            markDirty();
            writeValveState();
            scheduleRenderUpdate();
            // 让相邻管道/机器重新查询一次 capability（gt6StylePipesCables=false 的整合包里才会自动连/断）
            notifyBlockUpdate();
        }
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    /** 阀门没有界面：右键不会打开 GUI（红石状态由信号控制）。 */
    @Override
    protected boolean openGUIOnRightClick() {
        return false;
    }

    /**
     * GT 的 {@code MetaTileEntity} 把 {@code createUI} 声明为抽象方法，因此必须实现。
     *
     * <p>
     * 阀门<b>没有功能界面</b>（{@link #openGUIOnRightClick()} 恒为 {@code false}，右键不会打开它）。
     * 这里只提供一个「标题 + 玩家背包」的空壳，纯粹是为了满足抽象方法，并防止任何外部路径
     * 打开界面时拿到 {@code null} 而 NPE。
     * </p>
     */
    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        return ModularUI.builder(GuiTextures.BACKGROUND, 176, 166)
                .label(10, 5, getMetaFullName())
                .bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7, 84)
                .build(getHolder(), entityPlayer);
    }

    @Override
    public boolean onWrenchClick(EntityPlayer playerIn, EnumHand hand, EnumFacing wrenchSide,
                                 CuboidRayTraceResult hitResult) {
        if (wrenchSide == null || getWorld() == null || getWorld().isRemote) {
            return wrenchSide != null;
        }
        if (com.susy.plusplus.util.PlayerHeld.sneaking(playerIn)) {
            setOutputFace(wrenchSide);
        } else {
            setInputFace(wrenchSide);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 转发辅助
    // ------------------------------------------------------------------

    /** 输出面邻居（可能为 null）。 */
    @Nullable
    protected TileEntity outputNeighbor() {
        if (getWorld() == null || getPos() == null) {
            return null;
        }
        return getWorld().getTileEntity(getPos().offset(outputFace));
    }

    /** 输入面邻居（可能为 null）。 */
    @Nullable
    protected TileEntity inputNeighbor() {
        if (getWorld() == null || getPos() == null) {
            return null;
        }
        return getWorld().getTileEntity(getPos().offset(inputFace));
    }

    // ------------------------------------------------------------------
    // 持久化 / 同步
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger(NBT_INPUT_FACE, inputFace.getIndex());
        data.setInteger(NBT_OUTPUT_FACE, outputFace.getIndex());
        data.setBoolean(NBT_OPEN, open);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.inputFace = byIndexSafe(data.getInteger(NBT_INPUT_FACE), EnumFacing.NORTH);
        this.outputFace = byIndexSafe(data.getInteger(NBT_OUTPUT_FACE), EnumFacing.SOUTH);
        if (this.inputFace == this.outputFace) {
            // 防御：旧存档/手改 NBT 导致的非法状态
            this.outputFace = this.inputFace.getOpposite();
        }
        this.open = !data.hasKey(NBT_OPEN) || data.getBoolean(NBT_OPEN);
    }

    private static EnumFacing byIndexSafe(int index, EnumFacing fallback) {
        if (index < 0 || index >= EnumFacing.VALUES.length) {
            return fallback;
        }
        return EnumFacing.VALUES[index];
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(inputFace.getIndex());
        buf.writeByte(outputFace.getIndex());
        buf.writeBoolean(open);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.inputFace = EnumFacing.VALUES[buf.readByte()];
        this.outputFace = EnumFacing.VALUES[buf.readByte()];
        this.open = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        if (dataId == SYNC_VALVE_STATE) {
            this.inputFace = EnumFacing.VALUES[buf.readByte()];
            this.outputFace = EnumFacing.VALUES[buf.readByte()];
            this.open = buf.readBoolean();
            scheduleRenderUpdate();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    /** 该阀门的基础外壳（只能用客户端类，因此声明为客户端专属）。 */
    @SideOnly(Side.CLIENT)
    protected abstract ICubeRenderer getValveCasing();

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getValveCasing().render(renderState, translation, pipeline);
        if (open) {
            renderDirectionalOverlays(renderState, translation, pipeline);
        }
    }

    /** 导通时在输入/输出面画方向标记（关闭时不画，作为「断开」的视觉反馈）。 */
    @SideOnly(Side.CLIENT)
    protected abstract void renderDirectionalOverlays(CCRenderState renderState, Matrix4 translation,
                                                      IVertexOperation[] pipeline);

    @SideOnly(Side.CLIENT)
    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(getValveCasing().getParticleSprite(), getPaintingColorForRendering());
    }

    // ------------------------------------------------------------------
    // tooltip
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    protected static String faceName(EnumFacing face) {
        return I18n.format("susyplusplus.direction." + face.getName());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(I18n.format("susyplusplus.machine.redstone_valve.tooltip.default_open"));
        tooltip.add(I18n.format("susyplusplus.machine.redstone_valve.tooltip.no_buffer"));
        tooltip.add(I18n.format("susyplusplus.machine.redstone_valve.tooltip.faces",
                faceName(inputFace), faceName(outputFace)));
    }

    @Override
    public void addToolUsages(ItemStack stack, @Nullable World world, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("susyplusplus.machine.redstone_valve.tool_usage.set_input"));
        tooltip.add(I18n.format("susyplusplus.machine.redstone_valve.tool_usage.set_output"));
        super.addToolUsages(stack, world, tooltip, advanced);
    }
}
