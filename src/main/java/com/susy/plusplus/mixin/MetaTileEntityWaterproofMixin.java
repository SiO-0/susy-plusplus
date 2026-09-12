package com.susy.plusplus.mixin;

import com.susy.plusplus.waterproof.IWaterproofMachine;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 GT 的所有机器（{@link MetaTileEntity}）注入“防水状态”。
 *
 * <p>
 * 注入点（均来自 GT 实际源码，非编造）：
 * </p>
 * <ul>
 * <li>{@code getIsWeatherOrTerrainResistant()}：HEAD 注入，防水时返回 {@code true}，
 * 从而在 {@code checkWeatherOrTerrainExplosion(...)} 中跳过遇水/降雨/岩浆爆炸判定。</li>
 * <li>{@code writeToNBT(NBTTagCompound)}：RETURN 注入，写入键
 * {@code susyplusplus:Waterproof}。</li>
 * <li>{@code readFromNBT(NBTTagCompound)}：RETURN 注入，读回防水状态（跨存档/重启持久化）。</li>
 * </ul>
 *
 * <p>
 * 同时让目标类实现 {@link IWaterproofMachine}，供 TOP、喷漆、事件逻辑统一读取。
 * </p>
 *
 * <p>
 * 局限：{@code getIsWeatherOrTerrainResistant()} 在少数子类（如 RockBreaker/Pump/Fisher/
 * BlockBreaker/LongDistanceEndpoint）被覆写为 {@code true}（本就防水），以及
 * {@code MetaTileEntityMultiblockPart} 走多方块控制器判定；这些覆写不会经过本注入。
 * 而真正会因遇水爆炸的普通机器（{@code TieredMetaTileEntity} 与
 * {@code MetaTileEntityEnergyHatch}）都使用基类方法，因此可被正确覆盖。
 * </p>
 */
@Mixin(value = MetaTileEntity.class, remap = false)
public abstract class MetaTileEntityWaterproofMixin implements IWaterproofMachine {

    @Unique
    private boolean susyplusplus$waterproof = false;

    @Override
    public boolean isWaterproof() {
        return susyplusplus$waterproof;
    }

    @Override
    public void setWaterproof(boolean waterproof) {
        this.susyplusplus$waterproof = waterproof;
    }

    @Inject(method = "getIsWeatherOrTerrainResistant", at = @At("HEAD"), cancellable = true)
    private void susyplusplus$waterproofWeatherResistant(CallbackInfoReturnable<Boolean> cir) {
        if (susyplusplus$waterproof) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void susyplusplus$writeWaterproof(NBTTagCompound data, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (susyplusplus$waterproof) {
            cir.getReturnValue().setBoolean("susyplusplus:Waterproof", true);
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void susyplusplus$readWaterproof(NBTTagCompound data, CallbackInfo ci) {
        this.susyplusplus$waterproof = data.getBoolean("susyplusplus:Waterproof");
    }
}
