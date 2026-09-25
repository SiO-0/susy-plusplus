package com.susy.plusplus.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;

import java.util.function.Function;

/**
 * 「绑定到某个方块坐标」的 ModularUI 工厂。
 *
 * <p>
 * 需求背景：机器面配置界面与机器工具箱界面都需要知道<b>点了哪台机器</b>，
 * 而 ModularUI 自带的 {@code tileEntity()} / {@code sidedTileEntity()} 工厂要求
 * 目标 TileEntity 自己实现 MUI2 的 {@code IGuiHolder} —— GT 的机器（本版本为旧版
 * {@code gregtech.api.gui}）并不满足。
 * </p>
 *
 * <p>
 * 因此这里按 MUI2 文档推荐的做法自建工厂：数据用 {@link PosGuiData}（坐标会通过
 * {@link #writeGuiData}/{@link #readGuiData} 在服务端/客户端之间同步），
 * 界面持有者由 {@code holderFactory} 每次打开时新建，便于把「本次打开期间的临时状态」
 * 放在持有者实例里。
 * </p>
 *
 * <p>
 * 构造函数会向 {@link GuiManager} 注册自己 —— 因此<b>客户端与服务端必须用同样的名字
 * 各构造一份</b>（本模组的做法是在 {@link SuGuiFactories} 里静态持有）。
 * </p>
 */
public class SuPosGuiFactory extends AbstractUIFactory<PosGuiData> {

    private final Function<PosGuiData, IGuiHolder<PosGuiData>> holderFactory;

    public SuPosGuiFactory(String name, Function<PosGuiData, IGuiHolder<PosGuiData>> holderFactory) {
        super(name);
        this.holderFactory = holderFactory;
        GuiManager.registerFactory(this);
    }

    @Override
    public IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        return this.holderFactory.apply(data);
    }

    @Override
    public void writeGuiData(PosGuiData guiData, PacketBuffer buffer) {
        buffer.writeBlockPos(guiData.getBlockPos());
    }

    @Override
    public PosGuiData readGuiData(EntityPlayer player, PacketBuffer buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new PosGuiData(player, pos.getX(), pos.getY(), pos.getZ());
    }

    /** 服务端调用：打开界面（同步到客户端）。 */
    public void open(EntityPlayerMP player, BlockPos pos) {
        GuiManager.open(this, new PosGuiData(player, pos.getX(), pos.getY(), pos.getZ()), player);
    }
}
