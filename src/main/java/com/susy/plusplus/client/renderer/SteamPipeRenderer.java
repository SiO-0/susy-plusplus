package com.susy.plusplus.client.renderer;

import com.susy.plusplus.Tags;
import com.susy.plusplus.pipe.steam.SteamPipePressure;
import com.susy.plusplus.pipe.steam.SteamPipeType;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.client.renderer.pipe.PipeRenderer;
import gregtech.client.renderer.texture.Textures;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.vec.uv.IconTransformation;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽管道的渲染器（CCL {@code ICCBlockRenderer} + {@code IItemRenderer}）。
 *
 * <h3>为什么不能复用 {@code FluidPipeRenderer}</h3>
 *
 * <p>
 * GT 的 {@code FluidPipeRenderer} 内部是
 * {@code EnumMap<FluidPipeType, TextureAtlasSprite>} 并用 {@code material} 取材质颜色/贴图；
 * 本模组的管道既不是 {@code FluidPipeType} 也没有 {@code Material}，
 * 直接复用会在 {@code buildRenderer} 里被类型检查拦掉（什么都不画）。
 * 因此这里写一个最小的渲染器：贴图全部复用 GT 已有的管道贴图，
 * 只用<b>颜色</b>区分低压/高压。
 * </p>
 *
 * <h3>注册流程（与 GT 一致）</h3>
 *
 * <ol>
 * <li>客户端 preInit 调用 {@link #preInit()}：创建 CCL 渲染类型并注册自身，
 * 同时登记贴图回调；</li>
 * <li>物品模型用 {@link #getModelLocation()} 作为 {@code ModelResourceLocation}
 * （见 {@code SuClientEvents}）；</li>
 * <li>方块通过 {@code BlockSteamPipe#getRenderType} 返回本项目创建的类型。</li>
 * </ol>
 */
@SideOnly(Side.CLIENT)
public final class SteamPipeRenderer extends PipeRenderer {

    public static final SteamPipeRenderer INSTANCE = new SteamPipeRenderer();

    /** 低压：橙色。 */
    private static final int LOW_PRESSURE_TINT = 0xFF8C1A;
    /**
     * 高压：黑色。
     *
     * <p>
     * 这里用极深的近黑 {@code 0x1A1A1A} 而不是纯黑 {@code 0x000000}：
     * 渲染走的是 {@code ColourMultiplier}（乘算），纯黑会把 GT 管道贴图的所有明暗细节
     * 一起乘成 0，只剩一片没有体积感的黑块；留一点亮度即可看出管道的立体高光。
     * </p>
     */
    private static final int HIGH_PRESSURE_TINT = 0x1A1A1A;

    private SteamPipeRenderer() {
        super("susyplusplus_steam_pipe", new ResourceLocation(Tags.MOD_ID, "steam_pipe"));
    }

    @Override
    public void registerIcons(TextureMap textureMap) {
        // 不使用自有贴图：open face 与 side 都直接用 GT 的管道贴图
        // （见 buildRenderer / getParticleTexture）。这里保留空实现以满足抽象方法。
    }

    @Override
    public void buildRenderer(PipeRenderContext renderContext, BlockPipe<?, ?, ?> blockPipe,
                              @Nullable IPipeTile<?, ?> pipeTile, IPipeType<?> pipeType,
                              @Nullable Material material) {
        if (!(pipeType instanceof SteamPipeType)) {
            return;
        }
        SteamPipeType type = (SteamPipeType) pipeType;
        boolean high = type.getPressure() == SteamPipePressure.HIGH_PRESSURE;

        TextureAtlasSprite openFace;
        switch (type.getSize()) {
            case TINY:
                openFace = Textures.PIPE_TINY;
                break;
            case SMALL:
                openFace = Textures.PIPE_SMALL;
                break;
            case NORMAL:
                openFace = Textures.PIPE_NORMAL;
                break;
            case LARGE:
                openFace = Textures.PIPE_LARGE;
                break;
            case HUGE:
            default:
                openFace = Textures.PIPE_HUGE;
                break;
        }

        int tint = GTUtility.convertRGBtoOpaqueRGBA_CL(high ? HIGH_PRESSURE_TINT : LOW_PRESSURE_TINT);

        if (openFace != null) {
            renderContext.addOpenFaceRender(new ColourMultiplier(tint), new IconTransformation(openFace));
        }
        if (Textures.PIPE_SIDE != null) {
            renderContext.addSideRender(new ColourMultiplier(tint), new IconTransformation(Textures.PIPE_SIDE));
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return Textures.PIPE_SIDE;
    }
}
