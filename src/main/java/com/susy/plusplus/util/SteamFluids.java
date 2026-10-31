package com.susy.plusplus.util;

import net.minecraftforge.fluids.FluidStack;

/**
 * 「蒸汽」判定工具。
 *
 * <p>
 * 全部蒸汽相关的代码（蒸汽泵覆盖板、蒸汽管道）都必须通过本类判定，
 * 避免两处白名单不一致。
 * </p>
 *
 * <h3>为什么用注册名而不是 Material</h3>
 *
 * <p>
 * SUSY 的 {@code susy:dense_steam} 是 GroovyScript 在运行期创建的，
 * 本模组编译期拿不到对应的 Java 字段；GT 的 {@code steam} 则来自
 * {@code Materials.Steam}。统一用<b>流体注册名</b>比较最稳。
 * </p>
 *
 * <p>
 * 也<b>不能</b>用 {@code name.contains("steam")}：SUSY 的裂解产物
 * （如 {@code susy:steamcracked_light_naphtha}）名字里同样包含 "steam"。
 * </p>
 */
public final class SteamFluids {

    /** GT 的普通蒸汽。 */
    public static final String STEAM = "steam";

    /** SUSY 的高密度蒸汽。 */
    public static final String DENSE_STEAM = "dense_steam";

    private SteamFluids() {
    }

    /** 是否为白名单中的蒸汽。 */
    public static boolean isSteam(FluidStack stack) {
        if (stack == null || stack.amount <= 0 || stack.getFluid() == null) {
            return false;
        }
        String name = stack.getFluid().getName();
        return STEAM.equals(name) || DENSE_STEAM.equals(name);
    }
}
