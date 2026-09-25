package com.susy.plusplus;

import com.susy.plusplus.integration.top.SuTopIntegration;
import com.susy.plusplus.item.SuMetaItems;
import com.susy.plusplus.client.SuClientEvents;
import com.susy.plusplus.gui.SuGuiFactories;
import com.susy.plusplus.material.SuMaterials;
import com.susy.plusplus.multiblock.SuMetaTileEntities;
import com.susy.plusplus.network.SuNetwork;
import com.susy.plusplus.pipe.SuPipeTweaks;
import com.susy.plusplus.recipe.SuRecipes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
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

        // 网络层（客户端 → 服务端：请求打开配置器界面）。
        // 客户端与服务端都要注册同一个频道。
        SuNetwork.init();

        // ModularUI 的工厂必须在两端都注册：服务端打开界面时只把"工厂名"发给客户端，
        // 客户端要靠这个名字在自己的 GuiManager 里找到同名工厂。
        SuGuiFactories.init();

        LOGGER.info("{} preInit 完成。", Tags.MOD_NAME);
    }

    /** The One Probe 的 modid（可选依赖）。 */
    private static final String TOP_MODID = "theoneprobe";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // ⚠ TOP 集成必须**在调用方**先判断是否加载，绝不能把判断放在 SuTopIntegration 内部：
        // SuTopIntegration 的方法体里直接引用了 mcjty.theoneprobe 的类型
        // （局部变量 → 会进入 StackMapTable），JVM 在**加载/校验该类时**就会去链接
        // mcjty/theoneprobe/api/ITheOneProbe；未装 TOP 时直接抛 NoClassDefFoundError，
        // 那时类内部的 Loader.isModLoaded 判断根本来不及执行（纯 GT 环境实测崩溃）。
        // 放在这里判断后，未装 TOP 时根本不会触碰 SuTopIntegration，该类也就不会被加载。
        if (Loader.isModLoaded(TOP_MODID)) {
            SuTopIntegration.init();
        } else {
            LOGGER.info("未检测到 The One Probe（{}），跳过 TOP 集成。", TOP_MODID);
        }

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
