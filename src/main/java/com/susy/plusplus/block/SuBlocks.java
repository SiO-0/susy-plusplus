package com.susy.plusplus.block;

import com.susy.plusplus.Tags;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 本模组的普通方块注册入口。
 *
 * <p>
 * 1.12.2 中方块/物品必须在 {@code RegistryEvent.Register} 阶段注册
 * （该事件在 preInit 之前触发），因此这里用 {@link Mod.EventBusSubscriber}
 * 挂到模组事件总线上，而不是在 preInit 里手动调用。
 * </p>
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class SuBlocks {

    /** 强化耐火砖方块（注册名 {@code susyplusplus:reinforced_firebrick}）。 */
    public static BlockReinforcedFirebrick REINFORCED_FIREBRICK;

    private SuBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockReinforcedFirebrick block = new BlockReinforcedFirebrick();
        block.setRegistryName(new ResourceLocation(Tags.MOD_ID, "reinforced_firebrick"));
        event.getRegistry().register(block);
        REINFORCED_FIREBRICK = block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (REINFORCED_FIREBRICK == null) {
            return;
        }
        ItemBlock itemBlock = new ItemBlock(REINFORCED_FIREBRICK);
        // 用 ItemBlock 让方块能以物品形式存在/被放置；
        // 其本地化键自动继承方块：tile.reinforced_firebrick.name
        itemBlock.setRegistryName(REINFORCED_FIREBRICK.getRegistryName());
        event.getRegistry().register(itemBlock);
    }
}
