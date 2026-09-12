package com.susy.plusplus.pipe;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;

import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.properties.FluidPipeProperties;
import gregtech.api.unification.material.properties.PropertyKey;
import gregtech.common.blocks.MetaBlocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 橡胶流体管道的「速率」修改：改成与钢一致。
 *
 * <h3>为什么不在 {@code MaterialEvent} 里做？</h3>
 *
 * <p>
 * 实测日志（{@code run/logs/latest.log}）：
 * </p>
 *
 * <pre>
 * [SusyPlusPlus] Rubber fluid pipe tweak skipped: Rubber.hasFluidPipe=false, Steel.hasFluidPipe=true
 * [GroovyLog]: Registering new properties          <-- GroovyScript 紧接着才给橡胶补上 FLUID_PIPE
 * </pre>
 *
 * <p>
 * 也就是说：GT 触发 {@code MaterialEvent} 时 {@code Materials.Rubber} <b>还没有</b>
 * {@code FLUID_PIPE} 属性 —— 橡胶管道是本整合包用 GroovyScript 在<b>之后</b>补上的。
 * 因此无论用什么事件优先级，只要还在 {@code MaterialEvent} 里就永远拿不到它。
 * </p>
 *
 * <h3>为什么改完属性还要"重新写入"管道方块？</h3>
 *
 * <p>
 * 各尺寸的数值由 {@code FluidPipeType} 按 {@code capacityMultiplier} 现算：
 * {@code TINY=1, SMALL=2, NORMAL=6, LARGE=12, HUGE=24}
 * （所以微型管显示的就是材料基础速率）。
 * 而 {@code BlockMaterialPipe#addPipeMaterial(material, properties)} 本质上只是
 * {@code enabledMaterials.put(...)}，<b>幂等且可覆盖</b> ——
 * 所以把改好的属性重新写一遍，全部尺寸都会立刻生效。
 * </p>
 *
 * <h3>为什么用反射？</h3>
 *
 * <p>
 * GT 的 {@code BlockPipe} 实现了 {@code team.chisel.ctm.api.IFacade}（Chisel CTM），
 * 而 Chisel 不在本工程的编译类路径上 —— 直接引用 {@code BlockFluidPipe} 会编译失败
 * （{@code 无法访问 team.chisel.ctm.api.IFacade}）。
 * 用反射访问 {@code MetaBlocks.FLUID_PIPES} 可以完全避开这个依赖，
 * 也不必为它再引入一个 stub。
 * </p>
 */
public final class SuPipeTweaks {

    /**
     * {@code gregtech.common.blocks.MetaBlocks#FLUID_PIPES}（{@code Map<String, BlockFluidPipe[]>}）。
     */
    private static final String FLUID_PIPES_FIELD = "FLUID_PIPES";

    private SuPipeTweaks() {
    }

    /** 由 {@code SusyPlusPlus#preInit} 调用（此时 GT 已建好管道、GroovyScript 也已加好属性）。 */
    public static void applyRubberFluidPipeThroughput() {
        if (!SuConfig.enableRubberPipeTweaks) {
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Rubber pipe tweaks are DISABLED in config.");
            return;
        }

        FluidPipeProperties rubber = Materials.Rubber.getProperty(PropertyKey.FLUID_PIPE);
        FluidPipeProperties steel = Materials.Steel.getProperty(PropertyKey.FLUID_PIPE);
        if (rubber == null || steel == null) {
            SusyPlusPlus.LOGGER.warn(
                    "[SusyPlusPlus] Rubber pipe tweak skipped: Rubber.hasFluidPipe={}, Steel.hasFluidPipe={}",
                    rubber != null, steel != null);
            return;
        }

        int steelThroughput = steel.getThroughput();
        int old = rubber.getThroughput();
        if (old != steelThroughput) {
            rubber.setThroughput(steelThroughput);
            SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Rubber fluid pipe throughput: {} -> {} (Steel's).",
                    old, steelThroughput);
        }

        int refreshed = refreshPipeBlocks(rubber);
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] Rubber fluid pipe throughput = {} L/t (same as Steel); refreshed {} pipe block(s).",
                steelThroughput, refreshed);
    }

    /**
     * 把改好的属性重新写回每个流体管道方块。
     *
     * @return 实际刷新成功的方块数量
     */
    private static int refreshPipeBlocks(FluidPipeProperties rubberProperties) {
        int refreshed = 0;
        try {
            Field field = MetaBlocks.class.getField(FLUID_PIPES_FIELD);
            Object raw = field.get(null);
            if (!(raw instanceof Map)) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] {} is not a Map; cannot refresh rubber pipes.",
                        FLUID_PIPES_FIELD);
                return 0;
            }

            Method isValid = null;
            Method add = null;
            for (Object value : ((Map<?, ?>) raw).values()) {
                if (!(value instanceof Object[])) {
                    continue;
                }
                for (Object pipe : (Object[]) value) {
                    if (pipe == null) {
                        continue;
                    }
                    if (isValid == null) {
                        Class<?> pipeClass = pipe.getClass();
                        isValid = pipeClass.getMethod("isValidPipeMaterial", Material.class);
                        add = pipeClass.getMethod("addPipeMaterial", Material.class, FluidPipeProperties.class);
                    }
                    if (Boolean.TRUE.equals(isValid.invoke(pipe, Materials.Rubber))) {
                        add.invoke(pipe, Materials.Rubber, rubberProperties);
                        refreshed++;
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Failed to refresh rubber pipe blocks: {}", e.toString());
        }
        return refreshed;
    }
}
