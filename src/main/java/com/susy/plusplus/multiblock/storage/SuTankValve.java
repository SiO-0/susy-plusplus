package com.susy.plusplus.multiblock.storage;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.common.metatileentities.multi.MetaTileEntityTankValve;

import net.minecraft.util.ResourceLocation;

/**
 * 洁净不锈钢 / 加强钛储罐阀门。
 *
 * <p>
 * <b>直接继承 GT 的 {@code MetaTileEntityTankValve}</b>：它已经实现了储罐阀门的全部行为
 * （{@code TANK_VALVE} 能力、接入结构后直接引用控制器流体库存、脱离时占位库存、
 * 朝下自动输出、无 GUI、需要潜行才能旋转……），我们只额外做两件事：
 * </p>
 *
 * <ul>
 * <li>未接入结构时显示本档外壳贴图（GT 原类只有 {@code isMetal} 布尔，表达不了洁净不锈钢/加强钛）；</li>
 * <li>{@code createMetaTileEntity} 里保留档位，让复刻出来的方块还是同一档。</li>
 * </ul>
 *
 * <p>
 * ⚠ 钢制储罐阀门<b>已存在于 GT</b>（{@code gregtech:tank_valve.steel}，注册 id 11524），
 * 因此本模组<b>不再重复注册</b>，按用户要求直接复用。
 * </p>
 */
public class SuTankValve extends MetaTileEntityTankValve {

    private final SuStorageTier tier;

    public SuTankValve(ResourceLocation metaTileEntityId, SuStorageTier tier) {
        // isMetal = true（材质差异完全由 getBaseTexture 覆写负责）
        super(metaTileEntityId, true);
        this.tier = tier;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new SuTankValve(metaTileEntityId, tier);
    }

    @Override
    public ICubeRenderer getBaseTexture() {
        if (getController() == null) {
            return SuStorageTextures.casing(tier);
        }
        // 接入结构后与 GT 原行为一致（交给父类）
        return super.getBaseTexture();
    }
}
