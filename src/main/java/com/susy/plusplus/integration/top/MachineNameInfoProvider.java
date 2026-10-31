package com.susy.plusplus.integration.top;

import com.susy.plusplus.Tags;
import com.susy.plusplus.machine.MachineNameHelper;
import com.susy.plusplus.waterproof.WaterproofHelper;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * The One Probe：显示机器的**自定义名称**（{@code CustomName}）。
 *
 * <p>
 * 只有确实设置了名字的 GT 机器才显示一行：
 * {@code 名称：<自定义名>}（金色）。名字由 {@link MachineNameHelper#getName}
 * 读取（内部反射，不依赖 AE2）。
 * </p>
 *
 * <p>
 * TOP 的 probe 信息在<b>服务端</b>组装（与阀门/无线塔的 provider 一致），
 * 所以读到的一定是权威名字。
 * </p>
 */
public class MachineNameInfoProvider implements IProbeInfoProvider {

    private static final String KEY = "susyplusplus.top.machine_name";

    @Override
    public String getID() {
        return Tags.MOD_ID + ":machine_name";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
            IBlockState blockState, IProbeHitData data) {
        if (world == null || data == null) {
            return;
        }
        if (WaterproofHelper.getMachine(world, data.getPos()) == null) {
            return;
        }
        String name = MachineNameHelper.getName(world, data.getPos());
        if (name == null || name.isEmpty()) {
            return;
        }
        probeInfo.text(TextFormatting.GOLD + IProbeInfo.STARTLOC + KEY + IProbeInfo.ENDLOC + " " + name);
    }
}
