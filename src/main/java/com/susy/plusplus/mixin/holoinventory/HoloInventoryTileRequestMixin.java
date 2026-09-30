package com.susy.plusplus.mixin.holoinventory;

import java.lang.reflect.Field;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 HoloInventory 对准 GT 机器时能显示<b>机器名称</b>（③）。
 *
 * <h3>为什么原本显示不出来</h3>
 *
 * <p>
 * HoloInventory（{@code net.dries007.holoInventory}，modid {@code holoinventory}）的
 * {@code TileRequest$Handler#onMessage} 流程是：
 * </p>
 *
 * <ol>
 * <li>{@code world.getTileEntity(pos)}</li>
 * <li>不是箱子 / 末影箱 / 唱片机 → 取 {@code ITEM_HANDLER} 能力得到 {@code IItemHandler}</li>
 * <li><b>{@code if (iih instanceof INamedItemHandler) name = getItemHandlerName()}</b></li>
 * <li>用 {@code new PlainInventory(pos, name, iih)} 回包</li>
 * </ol>
 *
 * <p>
 * 也就是说：名字来自"<b>物品库存对象</b>"是否实现了它的
 * {@code api.INamedItemHandler} 接口。而 GT 2.8.x 早于这个 API，
 * 它返回的是自己的一堆 {@code IItemHandler} 实现（每种机器可能还不同），
 * 都不含该接口 —— 所以全息板拿不到名字。
 * </p>
 *
 * <h3>本补丁怎么做</h3>
 *
 * <p>
 * 在这个服务端处理器<b>最前面</b>拦下来：如果目标是 GT 机器
 * （{@code MetaTileEntityHolder}）且确实有物品库存，就自己构造同样类型的回包
 * （{@code PlainInventory(pos, 机器名, handler)}）并直接返回。
 * </p>
 *
 * <p>
 * 为了<b>不把 HoloInventory 写进编译期依赖</b>（别人不一定装它），
 * 回包类型用反射创建；这样本模组对 HoloInventory 只是"运行时可选补丁"。
 * 补丁失败只会打一条 WARN 并走回原逻辑，不影响游戏。
 * </p>
 */
@Pseudo
@Mixin(targets = "net.dries007.holoInventory.network.request.TileRequest$Handler", remap = false)
public class HoloInventoryTileRequestMixin {

    /** HoloInventory 用来回包的类型（反射使用，避免编译期依赖）。 */
    private static final String PLAIN_INVENTORY =
            "net.dries007.holoInventory.network.response.PlainInventory";

    /**
     * 必须用<b>完整描述符</b>指向那个"桥接方法"。
     *
     * <p>
     * ⚠ 实测踩坑：只写 {@code method = "onMessage"} 时，Mixin 会绑到
     * {@code onMessage(TileRequest, MessageContext)} 这个具体重载上，并要求注入器第一个参数
     * <b>精确等于</b> {@code TileRequest}（不允许写父类型 {@code IMessage}），
     * 于是直接抛：
     * </p>
     *
     * <pre>
     * InvalidInjectionException: Invalid descriptor ...
     * Expected (...TileRequest;...MessageContext;...CallbackInfoReturnable;)V
     * but found (...IMessage;...MessageContext;...CallbackInfoReturnable;)V
     * </pre>
     *
     * <p>
     * 而 {@code TileRequest} 不在本模组的编译类路径上（我们不依赖 HoloInventory）。
     * 桥接方法 {@code onMessage(IMessage, MessageContext)} 的参数类型是 Forge 的
     * {@code IMessage}/{@code MessageContext}，都可编译期引用，因此显式写出描述符即可。
     * </p>
     */
    @Inject(
            method = "onMessage(Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;"
                    + "Lnet/minecraftforge/fml/common/network/simpleimpl/MessageContext;)"
                    + "Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;",
            at = @At("HEAD"), cancellable = true)
    private void susyplusplus$nameGtMachines(IMessage message, MessageContext ctx,
                                             CallbackInfoReturnable<IMessage> cir) {
        if (!SuConfig.enableThirdPartyPatches) {
            return;
        }
        try {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return;
            }
            BlockPos pos = posOf(message);
            if (pos == null) {
                return;
            }

            World world = player.getServerWorld();
            TileEntity tileEntity = world.getTileEntity(pos);
            // 用 GT 自己的接口判断（不要直接引用 MetaTileEntityHolder：它通过
            // @Optional.Interface 实现了 AE2 的 IActionHost，编译期会要求 AE2 类）
            if (!(tileEntity instanceof IGregTechTileEntity)) {
                return;
            }
            MetaTileEntity mte = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
            if (mte == null) {
                return;
            }

            IItemHandler handler = tileEntity.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null || handler.getSlots() <= 0) {
                // 没有物品库存的机器本来就不会有全息信息，保持原行为
                return;
            }

            String name = I18n.translateToLocal(mte.getMetaFullName());
            Object response = buildResponse(pos, name, handler);
            if (response instanceof IMessage) {
                cir.setReturnValue((IMessage) response);
            }
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] HoloInventory machine-name patch skipped: {}", t.toString());
        }
    }

    /**
     * 读取 {@code TileRequest#pos} 字段。
     *
     * <p>
     * ⚠ 实测踩坑（这就是"对准机器仍然是 {@code tile.unnamed}"的真正原因）：
     * {@code TileRequest} 只有 {@code private BlockPos pos} 这一个字段，
     * <b>根本没有 getter</b> —— javap 的结果里只有构造函数、{@code fromBytes}/{@code toBytes}
     * 和编译器生成的 {@code access$000}。
     * </p>
     *
     * <p>
     * 最初这里用 {@code getMethod("getPos")} 取值，运行时抛
     * {@code NoSuchMethodException} 被 catch 吞掉 → 每次都提前 return，
     * 注入器形同虚设，于是客户端拿到的还是 HoloInventory 的兜底名字
     * （{@code te.getBlockType().getUnlocalizedName()}，而 GT 的机器方块
     * 就是 {@code setTranslationKey("unnamed")}，翻译键正是 {@code tile.unnamed}）。
     * </p>
     *
     * <p>
     * 字段名 {@code pos} 属于 HoloInventory 自己的类，不参与 Forge 的混淆映射（运行时仍是 {@code pos}），
     * 因此直接读字段即可。沿父类链查找，兼容将来挪到父类的重构。
     * </p>
     */
    private static BlockPos posOf(Object message) {
        try {
            Class<?> type = message.getClass();
            while (type != null) {
                try {
                    Field field = type.getDeclaredField("pos");
                    field.setAccessible(true);
                    Object value = field.get(message);
                    return value instanceof BlockPos ? (BlockPos) value : null;
                } catch (NoSuchFieldException notHere) {
                    type = type.getSuperclass();
                }
            }
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] HoloInventory machine-name patch skipped: {}", t.toString());
        }
        return null;
    }

    /** 反射构造 {@code new PlainInventory(pos, name, handler)}。 */
    private static Object buildResponse(BlockPos pos, String name, IItemHandler handler) {
        try {
            Class<?> type = Class.forName(PLAIN_INVENTORY);
            return type.getConstructor(BlockPos.class, String.class, IItemHandler.class)
                    .newInstance(pos, name, handler);
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Cannot build HoloInventory response: {}", t.toString());
            return null;
        }
    }
}
