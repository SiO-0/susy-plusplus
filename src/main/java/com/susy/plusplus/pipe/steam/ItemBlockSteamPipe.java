package com.susy.plusplus.pipe.steam;

import gregtech.api.pipenet.block.ItemBlockPipe;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 蒸汽管道的物品形态（带速率 tooltip）。
 *
 * <p>
 * 必须继承 GT 的 {@link ItemBlockPipe}：管道的形状依赖连接状态，
 * 放置时要由它把 {@code PipeType} 写进方块实体，且 {@code PipeRenderer}
 * 的物品渲染只认这个类型。
 * </p>
 *
 * <p>
 * 这里额外覆写 {@code addInformation}，显示
 * 「压力等级 + 传输速率（L/t）」——速率直接取自 {@link SteamPipeType}，
 * 与方块 / 网络 / 配方同源，不会出现"tooltip 与实物不一致"。
 * </p>
 *
 * <p>
 * 用 {@link I18n}（common 侧、已废弃）而不是
 * {@code net.minecraft.client.resources.I18n}：tooltip 虽然只在客户端显示，
 * 但本方法在服务端也可能被某些 mod 调用，用 common 版本不会触发
 * {@code NoClassDefFoundError}。
 * </p>
 */
public class ItemBlockSteamPipe extends ItemBlockPipe<SteamPipeType, SteamPipeProperties> {

    public ItemBlockSteamPipe(BlockSteamPipe block) {
        super(block);
    }

    /** 本物品对应的管道规格。 */
    public SteamPipeType getSteamPipeType() {
        Block block = getBlock();
        return block instanceof BlockSteamPipe ? ((BlockSteamPipe) block).getSteamPipeType()
                : SteamPipeType.LOW_TINY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        SteamPipeType type = getSteamPipeType();
        tooltip.add(I18n.translateToLocalFormatted("susyplusplus.tooltip.steam_pipe.rate", type.getThroughput()));
        tooltip.add(I18n.translateToLocalFormatted("susyplusplus.tooltip.steam_pipe.pressure",
                I18n.translateToLocal(type.getPressure() == SteamPipePressure.HIGH_PRESSURE
                        ? "susyplusplus.pressure.high"
                        : "susyplusplus.pressure.low")));
        tooltip.add(I18n.translateToLocal("susyplusplus.tooltip.steam_pipe.steam_only"));
    }
}
