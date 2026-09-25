package com.susy.plusplus.material;

import com.susy.plusplus.Tags;
import com.susy.plusplus.config.SuConfig;

import gregtech.api.GregTechAPI;
import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.event.MaterialEvent;
import gregtech.api.unification.material.event.MaterialRegistryEvent;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 本模组的材料 / 流体注册。
 *
 * <p>
 * 关键点：GT 在<strong>自身的 preInit 阶段</strong>依次触发
 * {@link MaterialRegistryEvent}（创建各附属的材料注册表）与 {@link MaterialEvent}（创建材料）。
 * 因此本类必须在“模组构造阶段”就注册到事件总线，见
 * {@code SusyPlusPlus} 的构造函数。
 * </p>
 *
 * <p>
 * 注册结果：
 * </p>
 * <ul>
 * <li>材料 {@code susyplusplus:waterproof_paint}（防水漆液），流体注册名
 * {@code waterproof_paint}</li>
 * </ul>
 *
 * <p>
 * <b>注意：</b>「橡胶流体管道速率对齐钢」<b>不在这里</b>做 —— 因为
 * {@code MaterialEvent} 触发时 {@code Materials.Rubber} 还没有 {@code FLUID_PIPE} 属性
 * （那是本整合包的 GroovyScript 之后补上的），详见
 * {@link com.susy.plusplus.pipe.SuPipeTweaks}。
 * </p>
 */
public final class SuMaterials {

    private SuMaterials() {
    }

    /** 防水漆液（流体材料）。 */
    public static Material WaterproofPaint;

    /** GT 要求每个附属先创建自己的材料注册表（modid 必须与材料 ResourceLocation 的命名空间一致）。 */
    @SubscribeEvent
    public static void onMaterialRegistry(MaterialRegistryEvent event) {
        GregTechApiCompat.createRegistryIfAbsent(Tags.MOD_ID);
    }

    /** 创建“防水漆液”材料（仅流体）。 */
    @SubscribeEvent
    public static void onMaterial(MaterialEvent event) {
        // 适配原版 GT 模式（SuConfig#vanillaGtCompat）：不注册「防水漆液」材料 ——
        // 防水喷漆改为「空喷漆罐 + 液态硅橡胶 576 mB」在灌装机合成（见 SuRecipes）。
        if (SuConfig.vanillaGtCompat) {
            return;
        }
        if (WaterproofPaint != null) {
            return;
        }
        // 使用 GT 自带的流体注册管线：
        // Material.Builder#liquid(FluidBuilder) -> FluidProperty ->
        // GTFluidRegistration，
        // 贴图由 GT 依据材质图标集自动生成（无需自备流体 PNG），并用 color 着色。
        WaterproofPaint = new Material.Builder(0, new ResourceLocation(Tags.MOD_ID, "waterproof_paint"))
                .liquid(new FluidBuilder().color(0x2E6FA3))
                .color(0x2E6FA3)
                .build();
    }

    /** 小工具：避免重复创建注册表（GT 的 createRegistry 在重复时会抛异常）。 */
    private static final class GregTechApiCompat {

        private GregTechApiCompat() {
        }

        static void createRegistryIfAbsent(String modid) {
            try {
                if (GregTechAPI.materialManager.getRegistry(modid) == null) {
                    GregTechAPI.materialManager.createRegistry(modid);
                }
            } catch (RuntimeException e) {
                // getRegistry 在不存在时也可能抛异常，此时直接创建
                GregTechAPI.materialManager.createRegistry(modid);
            }
        }
    }
}
