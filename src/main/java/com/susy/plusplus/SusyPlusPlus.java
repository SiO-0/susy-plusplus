package com.susy.plusplus;

import com.susy.plusplus.integration.top.SuTopIntegration;
import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.client.SuClientEvents;
import com.susy.plusplus.material.SuMaterials;
import com.susy.plusplus.multiblock.SuMetaTileEntities;
import com.susy.plusplus.pipe.SuPipeTweaks;
import com.susy.plusplus.recipe.SuRecipes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SUSY 整合包实用附属模组的主入口。
 *
 * <p>
 * 本模组新增“防水喷漆”物品与机器“防水状态”系统。
 * 防水状态由 Mixin 注入到 {@code gregtech.api.metatileentity.MetaTileEntity}，
 * 从而实现：真实阻止遇水/地形爆炸 + NBT 持久化 + The One Probe 显示。
 * </p>
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = "required-after:gregtech;after:theoneprobe;after:jei")
public class SusyPlusPlus {

    /** 模组统一日志器。 */
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public SusyPlusPlus() {
        // 材料注册事件由 GT 在自己的 preInit 阶段触发；
        // 因此必须在“模组构造阶段”就完成事件监听器的注册，否则会错过 MaterialEvent。
        MinecraftForge.EVENT_BUS.register(SuMaterials.class);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 构造我们的 MetaItem（会被 GT 的 MetaItem.META_ITEMS 静态列表收集，
        // 随后由 GT 在 RegistryEvent.Register<Item> 中统一注册）。
        SuMetaItems.init();

        // 客户端：显式注册模型注册处理器（不依赖 @Mod.EventBusSubscriber 自动扫描）。
        if (event.getSide() == Side.CLIENT) {
            SuClientEvents.init();
        }

        // 橡胶流体管道速率对齐钢。
        // 必须放在这里（而不是 MaterialEvent）：GT 触发 MaterialEvent 时
        // Materials.Rubber 还没有 FLUID_PIPE 属性，那是本整合包的 GroovyScript 之后才补上的；
        // 而我们的 preInit 一定晚于 GT 的 preInit，此时属性与管道方块都已就绪。
        SuPipeTweaks.applyRubberFluidPipeThroughput();

        LOGGER.info("{} preInit 完成。", Tags.MOD_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        SuTopIntegration.init();

        // 注册多方块控制器。放在 init 阶段（GT 的 MTE 已注册完毕、
        // 且 MTE 注册表尚未冻结）；MetaTileEntities.registerMetaTileEntity
        // 会自动完成"仓室能力注册"与"JEI 多方块预览注册"。
        SuMetaTileEntities.init();

        LOGGER.info("{} init 完成。", Tags.MOD_NAME);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // GT 在自身 postInit(load order: after gregtech) 中已加载完配方，
        // 此时向 RecipeMap 追加我们的配方。
        SuRecipes.init();
        LOGGER.info("{} postInit 完成。", Tags.MOD_NAME);
    }
}
