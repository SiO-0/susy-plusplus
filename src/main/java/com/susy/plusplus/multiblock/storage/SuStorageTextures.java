package com.susy.plusplus.multiblock.storage;

import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 多方块存储的客户端贴图映射。
 *
 * <p>
 * 之所以单独放一个 {@code @SideOnly(CLIENT)} 的类：{@code gregtech.client.renderer.texture.Textures}
 * 是纯客户端类，服务端一旦触碰它就有 {@code NoClassDefFoundError} 风险。
 * 把映射关在客户端类里、并且只在客户端的 {@code getBaseTexture(...)} 中调用，
 * 服务端就永远不会加载它（GT 自己也是这么做的）。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class SuStorageTextures {

    /**
     * 流体样品存储的正面覆盖层（GT 自带 {@code machines/fluid_samples_storage} 贴图）。
     *
     * <p>
     * <b>⚠ 必须在客户端 preInit 构造</b>（见 {@link #init()}）：
     * {@code OrientedOverlayRenderer}/{@code SimpleOverlayRenderer} 的构造器会把自己登记进
     * {@code gregtech.client.renderer.texture.Textures.iconRegisters}，而该列表<b>只在贴图拼接时
     * 遍历一次</b>。若本类在 stitch 之后才第一次加载，{@code registerIcons} 永远不会被调用，
     * renderer 内部的 sprite 保持 null —— 之后渲染该 MTE 的物品（创造模式物品栏 / JEI）就会 NPE。
     * 而且这个字段是客户端专属类，服务端也不会再碰到它（之前放在通用类里会在服务端构造客户端渲染器）。
     * </p>
     */
    public static final OrientedOverlayRenderer FLUID_SAMPLES_OVERLAY =
            new OrientedOverlayRenderer("machines/fluid_samples_storage");

    private SuStorageTextures() {
    }

    /** 由客户端 preInit 调用：触发类初始化，从而在贴图 stitch 之前完成覆盖层登记。 */
    public static void init() {
        // 调用静态字段即触发类初始化
        FLUID_SAMPLES_OVERLAY.getClass();
    }

    /** 流体样品存储的正面覆盖层。 */
    public static OrientedOverlayRenderer fluidSamplesOverlay() {
        return FLUID_SAMPLES_OVERLAY;
    }

    /** 该档外壳对应的基础材质渲染器。 */
    public static ICubeRenderer casing(SuStorageTier tier) {
        switch (tier) {
            case CLEAN_STAINLESS_STEEL:
                return Textures.CLEAN_STAINLESS_STEEL_CASING;
            case REINFORCED_TITANIUM:
                return Textures.STABLE_TITANIUM_CASING;
            case STEEL:
            default:
                return Textures.SOLID_STEEL_CASING;
        }
    }
}
