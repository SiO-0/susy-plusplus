package com.susy.plusplus.integration.top;

import com.susy.plusplus.waterproof.IWaterproofMachine;
import com.susy.plusplus.waterproof.WaterproofHelper;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SteamMetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapSteamMultiblockController;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler;
import gregtech.common.metatileentities.multi.MetaTileEntityMultiblockTank;
import gregtech.common.metatileentities.storage.MetaTileEntityCrate;
import gregtech.common.metatileentities.storage.MetaTileEntityDrum;
import gregtech.common.metatileentities.storage.MetaTileEntityQuantumChest;
import gregtech.common.metatileentities.storage.MetaTileEntityQuantumTank;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 在 The One Probe 中显示机器的防水状态。
 *
 * <p>
 * 显示方式参考 GT 的 {@code MaintenanceInfoProvider}：使用
 * {@code IProbeInfo.STARTLOC/ENDLOC} 做客户端本地化，并用 {@link TextFormatting} 上色。
 * </p>
 *
 * <h3>显示规则（按优先级）</h3>
 * <ol>
 * <li><b>天生防水</b>（机器自己覆写了 {@code getIsWeatherOrTerrainResistant()} 返回 true，
 * 如碎岩机 / 泵 / 钓鱼机 / 方块破坏机）→ 显示绿色"防水"。<b>这类提示保留</b>。</li>
 * <li><b>被防水喷漆刷过</b> → 显示绿色"防水"。</li>
 * <li><b>与防水无关</b>的机器（见 {@link #isWaterproofingIrrelevant}）→
 * <b>完全不显示</b>任何防水提示。</li>
 * <li>其余（既不天生防水、也没刷漆、且确实会因水/地形爆炸）→ 显示红色"不防水"。</li>
 * </ol>
 *
 * <p>
 * 第 3 条是必须的：蒸汽机器、桶、板条箱、超级箱/超级缸这类机器本来就没有"防水"概念
 * （它们根本不会调用 GT 的 {@code checkWeatherOrTerrainExplosion}），
 * 给它们显示"不防水"只会造成误导。
 * </p>
 */
public class WaterproofInfoProvider implements IProbeInfoProvider {

    private static final String KEY_WATERPROOF = "info.susyplusplus.waterproof";
    private static final String KEY_NOT_WATERPROOF = "info.susyplusplus.not_waterproof";

    @Override
    public String getID() {
        return "susyplusplus:waterproof";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world,
            IBlockState blockState, IProbeHitData data) {
        MetaTileEntity mte = WaterproofHelper.getMachine(world, data.getPos());
        if (!(mte instanceof IWaterproofMachine)) {
            return;
        }

        // 1) 天生防水：保留提示
        if (mte.getIsWeatherOrTerrainResistant()) {
            show(probeInfo, TextFormatting.GREEN, KEY_WATERPROOF);
            return;
        }

        // 2) 被喷漆刷过：显示防水
        if (((IWaterproofMachine) mte).isWaterproof()) {
            show(probeInfo, TextFormatting.GREEN, KEY_WATERPROOF);
            return;
        }

        // 3) 与防水无关的机器：不显示任何防水提示
        if (isWaterproofingIrrelevant(mte)) {
            return;
        }

        // 4) 真正"不防水"
        show(probeInfo, TextFormatting.RED, KEY_NOT_WATERPROOF);
    }

    private static void show(IProbeInfo probeInfo, TextFormatting color, String key) {
        probeInfo.text(color.toString() + IProbeInfo.STARTLOC + key + IProbeInfo.ENDLOC);
    }

    /**
     * 判断该机器是否属于"没有防水概念"的类别。
     *
     * <p>
     * 这些机器不会参与 {@code checkWeatherOrTerrainExplosion}（即不会因为靠近水/岩浆
     * 或被雨淋而爆炸），因此给它们显示"不防水"没有意义。
     * </p>
     *
     * <p>
     * 用基类做 {@code instanceof} 判断，这样 GT 以及其它附属（如 Susy-Core 的
     * {@code SuSySimpleSteamMetaTileEntity}、塑料桶等）的子类也会被覆盖。
     * 需要再加豁免类别时，在这里追加一行即可。
     * </p>
     */
    private static boolean isWaterproofingIrrelevant(MetaTileEntity mte) {
        // --- 蒸汽机器：单方块蒸汽机 + 蒸汽多方块 + 大锅炉 ---
        if (mte instanceof SteamMetaTileEntity) {
            return true;
        }
        if (mte instanceof RecipeMapSteamMultiblockController) {
            return true;
        }
        if (mte instanceof MetaTileEntityLargeBoiler) {
            return true;
        }

        // --- 存储类：桶 / 板条箱 / 超级箱 / 超级缸 / 大型储罐 ---
        if (mte instanceof MetaTileEntityDrum) {
            return true;
        }
        if (mte instanceof MetaTileEntityCrate) {
            return true;
        }
        if (mte instanceof MetaTileEntityQuantumChest) {
            return true;
        }
        if (mte instanceof MetaTileEntityQuantumTank) {
            return true;
        }
        return mte instanceof MetaTileEntityMultiblockTank;
    }
}
