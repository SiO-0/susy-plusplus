package com.susy.plusplus.mixin;

import com.susy.plusplus.machine.MachineExtraState;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link SimpleMachineMetaTileEntity} 自己覆写了
 * {@code getCapability(Capability, EnumFacing)}，并且对
 * {@code ITEM_HANDLER}/{@code FLUID_HANDLER} <b>直接 return，不再走 super</b>
 * （已核实运行期 GT 2.8.10 的 javap 结果里确实有该方法）。
 *
 * <p>
 * 因此 {@link MetaTileEntityExtraStateMixin} 里挂在 {@code MetaTileEntity#getCapability} 上的
 * 物品库存包装对这些"最常用的单方块机器"<b>不会生效</b> —— 必须在这里再挂一份。
 * </p>
 *
 * <p>
 * 包装只在开关打开时发生（{@link MachineExtraState#wrapItemHandler} 在关闭时原样返回），
 * 所以默认行为与 GT 完全一致。
 * </p>
 */
@Mixin(value = SimpleMachineMetaTileEntity.class, remap = false)
public abstract class SimpleMachineItemCapabilityMixin {

    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true)
    private void susyplusplus$wrapItemHandler(Capability<?> capability, EnumFacing side,
            CallbackInfoReturnable<Object> cir) {
        Object result = cir.getReturnValue();
        if (capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || !(result instanceof IItemHandler)) {
            return;
        }
        // 运行时 this 就是 SimpleMachineMetaTileEntity（MetaTileEntity 的子类）；
        // 编译期本 mixin 与 GT 类没有继承关系，因此必须先转 Object 再转目标类型。
        MetaTileEntity self = (MetaTileEntity) (Object) this;
        IItemHandler wrapped = MachineExtraState.wrapItemHandler(self, (IItemHandler) result);
        if (wrapped != result) {
            cir.setReturnValue(wrapped);
        }
    }
}
