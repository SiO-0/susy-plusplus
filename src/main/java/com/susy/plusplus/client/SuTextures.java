package com.susy.plusplus.client;

import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 本模组的客户端材质（ICubeRenderer）持有类。
 *
 * <p>
 * <b>必须 @SideOnly(Side.CLIENT)</b>：{@link SimpleOverlayRenderer} 的构造函数会向
 * {@code gregtech.client.renderer.texture.Textures} 的客户端注册表登记自己，
 * 在服务端构造会崩。GT 自己的 {@code Textures} 类就是这么做的。
 * </p>
 *
 * <p>
 * <b>路径规则（踩过的坑）</b>：{@code SimpleOverlayRenderer(String basePath)} 支持
 * {@code "modid:path"} 形式，最终注册的是
 * </p>
 *
 * <pre>
 * new ResourceLocation(modid, "blocks/" + path)
 * </pre>
 *
 * <p>
 * 也就是固定落在 <b>{@code textures/blocks/<path>.png}</b>（注意是<b>复数 blocks</b>）。
 * 而 {@code ResourceLocation} 会把 path <b>强制小写</b>，所以目录/文件名<b>不能有大写字母</b>。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class SuTextures {

    /** 强化耐火砖纹理，用于强化土高炉控制器本体与正面覆盖层。 */
    public static final ICubeRenderer REINFORCED_BRICKS = new SimpleOverlayRenderer(
            "susyplusplus:reinforcedpbf/reinforced_bricks");

    private SuTextures() {
    }

    /**
     * 强制本类初始化（构造出上面的 renderer）。
     *
     * <p>
     * <b>必须在客户端 preInit 调用，不能等第一次渲染时才懒初始化！</b>
     * </p>
     *
     * <p>
     * 原因：{@link SimpleOverlayRenderer} 的构造函数会把自己加进
     * {@code gregtech.client.renderer.texture.Textures.iconRegisters}，
     * 而这个列表<b>只在贴图拼接（TextureStitch）时被遍历一次</b>：
     * </p>
     *
     * <pre>
     * // Textures.registerIcons(TextureMap)
     * for (IconRegistrar registrar : iconRegisters) {
     *     registrar.registerIcons(textureMap);
     * }
     * </pre>
     *
     * <p>
     * 若本类在 stitch 之后才第一次被加载，{@code registerIcons} 就永远不会被调用，
     * renderer 内部的 {@code sprite} 保持 null —— 之后渲染该 MTE 的物品
     * （创造模式物品栏 / HadEnoughItems）就会 NPE，正是 CCL 报的那个异常。
     * </p>
     */
    public static void init() {
        // 调用静态方法即触发类初始化，从而构造 REINFORCED_BRICKS
    }
}
