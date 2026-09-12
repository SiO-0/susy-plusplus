package com.susy.plusplus.integration.top;

import com.susy.plusplus.Tags;
import com.susy.plusplus.multiblock.wireless.MetaTileEntityWirelessEnergyTower;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IMultiblockController;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProgressStyle;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;

import org.jetbrains.annotations.NotNull;

/**
 * 无线能量传输塔的 The One Probe 显示：<b>传输进度条</b> + 输出功率 / 电量 / 目标数。
 *
 * <p>
 * 继承 GT 自己的 {@code CapabilityInfoProvider<IMultiblockController>}：
 * 它会在玩家指向的方块实体上取 {@code CAPABILITY_MULTIBLOCK_CONTROLLER}
 * （控制器本体与多方块仓室都会暴露这个能力），因此指向控制器或任意仓室都能看到信息。
 * </p>
 *
 * <p>
 * TOP 的信息是在<b>服务端</b>组装后发给客户端的，所以这里可以直接读塔的服务端字段
 * （进度 / 电量等），不需要额外做客户端同步。
 * </p>
 */
public class WirelessTowerInfoProvider extends CapabilityInfoProvider<IMultiblockController> {

    private static final String KEY_PREFIX = "susyplusplus.top.wireless_energy_tower.";

    @Override
    public String getID() {
        return Tags.MOD_ID + ":wireless_energy_tower";
    }

    @NotNull
    @Override
    protected Capability<IMultiblockController> getCapability() {
        return GregtechCapabilities.CAPABILITY_MULTIBLOCK_CONTROLLER;
    }

    @Override
    protected void addProbeInfo(@NotNull IMultiblockController controller, @NotNull IProbeInfo probeInfo,
            @NotNull EntityPlayer player, @NotNull TileEntity tileEntity, @NotNull IProbeHitData data) {
        if (!(controller instanceof MetaTileEntityWirelessEnergyTower)) {
            return;
        }
        MetaTileEntityWirelessEnergyTower tower = (MetaTileEntityWirelessEnergyTower) controller;
        if (!tower.isStructureFormed()) {
            return;
        }

        int workTime = tower.getTransferWorkTime();
        if (workTime > 0) {
            // 进度条：当前传输已进行 / 总时长（条内数字由 TOP 自己画，不依赖本地化）
            probeInfo.text(TextFormatting.WHITE + localize(KEY_PREFIX + "progress"));
            IProgressStyle style = probeInfo.defaultProgressStyle()
                    .filledColor(0xFF00AEEF)
                    .alternateFilledColor(0xFF0072BC)
                    .borderColor(0xFF555555)
                    .backgroundColor(0xFF303030)
                    .width(150);
            probeInfo.progress(tower.getTransferProgress(), workTime, style);
            // 只有最后 3 秒真的在送电时才显示输出功率
            if (tower.isOutputting()) {
                probeInfo.text(TextFormatting.GRAY + localize(KEY_PREFIX + "output") + " "
                        + tower.getOutputPerTick() + " EU/t");
            }
        } else {
            probeInfo.text(TextFormatting.GRAY + localize(KEY_PREFIX + "idling"));
        }

        probeInfo.text(TextFormatting.GRAY + localize(KEY_PREFIX + "energy") + " "
                + tower.getStoredEnergy() + " / " + tower.getEnergyCapacity() + " EU");
        probeInfo.text(TextFormatting.GRAY + localize(KEY_PREFIX + "targets") + " " + tower.getDroneCount());
    }

    /** 用 TOP 的 {@code {*key*}} 语法交给客户端本地化。 */
    private static String localize(String key) {
        return IProbeInfo.STARTLOC + key + IProbeInfo.ENDLOC;
    }
}
