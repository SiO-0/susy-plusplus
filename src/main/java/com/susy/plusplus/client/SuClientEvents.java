package com.susy.plusplus.client;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.block.SuBlocks;
import com.susy.plusplus.config.SuConfig;
import com.susy.plusplus.item.ItemWaterproofSprayCan;
import com.susy.plusplus.item.battery.ItemBatteryCase;
import com.susy.plusplus.item.configurator.ItemConfigurator;
import com.susy.plusplus.item.trolley.ItemTrolley;
import com.susy.plusplus.item.SuMetaItems;

import gregtech.api.items.metaitem.MetaItem;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端专用注册：确保本模组 MetaItem 的模型一定被注册。
 *
 * <p>
 * 不依赖 FML 的 {@code @Mod.EventBusSubscriber} 自动扫描，而是在模组 preInit（客户端）
 * 中显式调用 {@link #init()} 把本类注册到事件总线，确保一定生效。
 * </p>
 *
 * <p>
 * {@code MetaItem#registerModels()} / {@code registerTextureMesh()}
 * 遍历的是物品**自身**的
 * meta 表，与 GT 的 {@code MetaItems.ITEMS} 列表无关，因此可避免因加载顺序/列表快照导致的漏注册。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class SuClientEvents {

    private SuClientEvents() {
    }

    /** 由 {@code SusyPlusPlus#preInit} 在客户端显式调用。 */
    public static void init() {
        MinecraftForge.EVENT_BUS.register(SuClientEvents.class);

        // 关键：必须在这里（preInit）就把 SuTextures 初始化出来。
        // GT 的 Textures.iconRegisters 只在贴图 stitch 时遍历一次，
        // 迟于该时机创建的 ICubeRenderer 永远不会注册图标，
        // 渲染对应 MTE 物品时会 NPE（CCL: "caught an exception whilst rendering an item"）。
        SuTextures.init();

        // 同理：FluidSamplesStorageMachine 用的 OrientedOverlayRenderer 也必须在 stitch 之前构造，
        // 否则它不会被登记进 Textures.iconRegisters，渲染该 MTE 的物品时会 NPE（"材质崩溃"）。
        com.susy.plusplus.multiblock.storage.SuStorageTextures.init();

        // 配置器快捷键（Shift+V）+ 它的按键事件监听。
        ConfiguratorKeys.init();
        MinecraftForge.EVENT_BUS.register(ConfiguratorKeys.class);

        SusyPlusPlus.LOGGER.info("[SusyPlusPlus] SuClientEvents registered on client event bus.");
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        ItemWaterproofSprayCan sprayCan = SuMetaItems.WATERPROOF_SPRAY_ITEM;
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] ModelRegistryEvent: item={}, in MetaItem registry list={}, modelRL={}",
                sprayCan,
                sprayCan == null ? null : MetaItem.getMetaItems().contains(sprayCan),
                sprayCan == null ? null : sprayCan.createItemModelPath(sprayCan.sprayCan, ""));
        if (sprayCan == null) {
            // 若是配置里主动关闭的，就不必刷 WARN 了
            if (SuConfig.enableWaterproofSprayCan) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] WATERPROOF_SPRAY_ITEM is null; skip model registration.");
            }
        } else {
            sprayCan.registerModels();
            sprayCan.registerTextureMesh();
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered models + texture mesh for waterproof spray can.");
        }

        // 普通方块：注册其 ItemBlock 的物品模型（blockstates + models/block + models/item）
        if (SuBlocks.REINFORCED_FIREBRICK == null) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] REINFORCED_FIREBRICK is null; skip block model registration.");
        } else {
            Item itemBlock = Item.getItemFromBlock(SuBlocks.REINFORCED_FIREBRICK);
            ModelLoader.setCustomModelResourceLocation(itemBlock, 0,
                    new ModelResourceLocation(SuBlocks.REINFORCED_FIREBRICK.getRegistryName(), "inventory"));
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered block model for {}",
                    SuBlocks.REINFORCED_FIREBRICK.getRegistryName());
        }

        ItemBatteryCase batteryCase = SuMetaItems.BATTERY_CASE_ITEM;
        if (batteryCase == null) {
            if (SuConfig.enableBatteryCase) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] BATTERY_CASE_ITEM is null; skip model registration.");
            }
        } else {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] ModelRegistryEvent: battery case modelRL={}",
                    batteryCase.createItemModelPath(batteryCase.batteryCase, ""));
            batteryCase.registerModels();
            batteryCase.registerTextureMesh();
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered models + texture mesh for battery case.");
        }

        // 配置器
        ItemConfigurator configurator = SuMetaItems.CONFIGURATOR_ITEM;
        if (configurator == null) {
            if (SuConfig.enableConfigurator) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] CONFIGURATOR_ITEM is null; skip model registration.");
            }
        } else {
            configurator.registerModels();
            configurator.registerTextureMesh();
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered models + texture mesh for configurator.");
        }

        // 手推车
        ItemTrolley trolley = SuMetaItems.TROLLEY_ITEM;
        if (trolley == null) {
            if (SuConfig.enableTrolley) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] TROLLEY_ITEM is null; skip model registration.");
            }
        } else {
            trolley.registerModels();
            trolley.registerTextureMesh();
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Registered models + texture mesh for trolley.");
        }
    }
}
