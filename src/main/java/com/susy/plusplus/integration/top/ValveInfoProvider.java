package com.susy.plusplus.integration.top;

import com.susy.plusplus.Tags;
import com.susy.plusplus.machine.valve.AbstractRedstoneValve;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.metatileentity.MetaTileEntity;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * The One Probe：显示红石阀门的**开关状态**与输入 / 输出面。
 *
 * <p>
 * 显示内容（每行都是 {@code {*key*}} 形式，由客户端本地化）：
 * </p>
 *
 * <ul>
 * <li>第一行：<b>开启</b>（绿）/ <b>关闭</b>（红）—— 与阀门的面标记是否绘制一致；</li>
 * <li>第二行：输入面（方向名复用 {@code susyplusplus.direction.*}）；</li>
 * <li>第三行：输出面。</li>
 * </ul>
 *
 * <p>
 * TOP 的 probe 信息由<b>服务端</b>组装后发给客户端（与本模组的无线能量塔显示一致），
 * 所以这里直接读 {@link AbstractRedstoneValve#isOpen()} 的服务端权威值即可，
 * 不需要额外的客户端同步逻辑。
 * </p>
 */
public class ValveInfoProvider implements IProbeInfoProvider {

    private static final String KEY_PREFIX = "susyplusplus.top.valve.";
    private static final String DIRECTION_PREFIX = "susyplusplus.direction.";

    @Override
    public String getID() {
        return Tags.MOD_ID + ":redstone_valve";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
            IBlockState blockState, IProbeHitData data) {
        MetaTileEntity mte = WaterproofHelper.getMachine(world, data.getPos());
        if (!(mte instanceof AbstractRedstoneValve)) {
            return;
        }
        AbstractRedstoneValve valve = (AbstractRedstoneValve) mte;

        boolean open = valve.isOpen();
        probeInfo.text((open ? TextFormatting.GREEN : TextFormatting.RED)
                + loc(KEY_PREFIX + (open ? "open" : "closed")));
        probeInfo.text(TextFormatting.GRAY + loc(KEY_PREFIX + "input") + " "
                + direction(valve.getValveInputFace()));
        probeInfo.text(TextFormatting.GRAY + loc(KEY_PREFIX + "output") + " "
                + direction(valve.getValveOutputFace()));
    }

    /** TOP 的本地化标记：{@code {*key*}}。 */
    private static String loc(String key) {
        return IProbeInfo.STARTLOC + key + IProbeInfo.ENDLOC;
    }

    private static String direction(EnumFacing face) {
        return loc(DIRECTION_PREFIX + face.getName());
    }
}
