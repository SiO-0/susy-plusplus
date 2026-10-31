package com.susy.plusplus.pipe.steam;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 蒸汽管道方块 / 物品的持有类。
 *
 * <p>
 * 共 <b>10 种</b>：低压 5 种 + 高压 5 种（微型 / 小型 / 中型 / 大型 / 巨型）。
 * 每种对应一个方块实例和一个 {@link ItemBlock}，注册在 {@code SuBlocks}
 * 的两个 {@code RegistryEvent.Register} 里完成。
 * </p>
 *
 * <p>
 * 注意：蒸汽管道<b>不</b>受 {@code SuConfig} 开关控制。原因是
 * {@code RegistryEvent.Register<Block>} 的触发时机早于配置注入，
 * 此时读 {@code SuConfig} 只会读到 Java 默认值；为避免出现
 * "配置了关闭但其实还注册着"的假开关，这里选择像 GT 一样始终注册，
 * 只在配方上做取舍。
 * </p>
 */
public final class SuSteamPipes {

    private static final Map<SteamPipeType, BlockSteamPipe> BLOCKS = new EnumMap<>(SteamPipeType.class);
    private static final Map<SteamPipeType, ItemBlock> ITEMS = new EnumMap<>(SteamPipeType.class);

    private SuSteamPipes() {
    }

    public static void putBlock(SteamPipeType type, BlockSteamPipe block) {
        BLOCKS.put(type, block);
    }

    public static void putItem(SteamPipeType type, ItemBlock item) {
        ITEMS.put(type, item);
    }

    public static BlockSteamPipe getBlock(SteamPipeType type) {
        return BLOCKS.get(type);
    }

    public static ItemBlock getItem(SteamPipeType type) {
        return ITEMS.get(type);
    }

    /** 已注册的方块（只读）。 */
    public static Map<SteamPipeType, BlockSteamPipe> getBlocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    /** 已注册的物品（只读）。 */
    public static Map<SteamPipeType, ItemBlock> getItems() {
        return Collections.unmodifiableMap(ITEMS);
    }

    public static boolean isReady() {
        return BLOCKS.size() == SteamPipeType.VALUES.length;
    }

    /** 取某个方块的物品形态（用于配方输出）。 */
    public static ItemStack getStack(SteamPipeType type, int amount) {
        BlockSteamPipe block = BLOCKS.get(type);
        if (block == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(block, amount);
    }
}
