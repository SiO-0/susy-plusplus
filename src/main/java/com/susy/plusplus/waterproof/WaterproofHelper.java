package com.susy.plusplus.waterproof;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 机器方块实体查询工具。 */
public final class WaterproofHelper {

    private WaterproofHelper() {
    }

    /**
     * 获取该位置上的 GT 机器（MetaTileEntity）。
     *
     * @return MetaTileEntity；若该位置不是 GT 机器方块实体则返回 {@code null}
     */
    public static MetaTileEntity getMachine(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IGregTechTileEntity) {
            return ((IGregTechTileEntity) te).getMetaTileEntity();
        }
        return null;
    }

    /** @return 该位置机器是否为防水状态（非机器返回 {@code false}）。 */
    public static boolean isWaterproof(World world, BlockPos pos) {
        MetaTileEntity mte = getMachine(world, pos);
        return mte instanceof IWaterproofMachine && ((IWaterproofMachine) mte).isWaterproof();
    }
}
