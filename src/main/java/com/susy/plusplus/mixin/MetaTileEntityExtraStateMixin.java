package com.susy.plusplus.mixin;

import com.susy.plusplus.machine.FluidCapacityHelper;
import com.susy.plusplus.machine.MachineExtraState;

import gregtech.api.metatileentity.MetaTileEntity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把本模组的"机器附加状态"挂进 GT 的 {@link MetaTileEntity}（功能二 / 功能三的持久化与同步）。
 *
 * <p>
 * 注入点（<b>均已在运行期 {@code gregtech-1.12.2-2.8.10-beta.jar} 用 javap 核实存在</b>）：
 * </p>
 *
 * <ul>
 * <li>{@code writeToNBT(NBTTagCompound)} RETURN → 写入我们的键；</li>
 * <li>{@code readFromNBT(NBTTagCompound)} RETURN → 读出我们的键（并在 Pass 3 里应用流体容量）；</li>
 * <li>{@code writeInitialSyncData(PacketBuffer)} HEAD →
 * {@code receiveInitialSyncData(PacketBuffer)} HEAD：成对同步"不允许重复输入"开关，
 * 让工具箱在客户端也能显示正确状态（与 GT 自己同步 {@code FrontFacing} 的做法一致）；</li>
 * <li>{@code getCapability(Capability, EnumFacing)} RETURN → 给外部自动化拿到的
 * {@code ITEM_HANDLER} 套一层"禁止不同槽位重复物品"的包装（功能二）。
 * 注意 {@code SimpleMachineMetaTileEntity} <b>覆写了 getCapability 且不调用 super</b>，
 * 所以那边另有一份 {@link SimpleMachineItemCapabilityMixin}。</li>
 * </ul>
 */
@Mixin(value = MetaTileEntity.class, remap = false)
public abstract class MetaTileEntityExtraStateMixin {

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void susyplusplus$writeExtraState(NBTTagCompound data, CallbackInfoReturnable<NBTTagCompound> cir) {
        MachineExtraState.writeToNbt((MetaTileEntity) (Object) this, data);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void susyplusplus$readExtraState(NBTTagCompound data, CallbackInfo ci) {
        MetaTileEntity self = (MetaTileEntity) (Object) this;
        MachineExtraState.readFromNbt(self, data);
        // 功能三：把保存过的"当前容量"重新应用到流体槽上（存档/重载后依然生效）
        FluidCapacityHelper.applyStoredCapacities(self);
    }

    @Inject(method = "writeInitialSyncData", at = @At("HEAD"))
    private void susyplusplus$writeInitialSync(PacketBuffer buf, CallbackInfo ci) {
        MachineExtraState.writeInitialSync((MetaTileEntity) (Object) this, buf);
    }

    @Inject(method = "receiveInitialSyncData", at = @At("HEAD"))
    private void susyplusplus$readInitialSync(PacketBuffer buf, CallbackInfo ci) {
        MachineExtraState.readInitialSync((MetaTileEntity) (Object) this, buf);
    }

    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true)
    private void susyplusplus$wrapItemHandler(Capability<?> capability, EnumFacing side,
            CallbackInfoReturnable<Object> cir) {
        Object result = cir.getReturnValue();
        if (capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || !(result instanceof IItemHandler)) {
            return;
        }
        IItemHandler wrapped = MachineExtraState.wrapItemHandler((MetaTileEntity) (Object) this,
                (IItemHandler) result);
        if (wrapped != result) {
            cir.setReturnValue(wrapped);
        }
    }
}
