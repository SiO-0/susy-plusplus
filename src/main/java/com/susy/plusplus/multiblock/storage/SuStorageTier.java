package com.susy.plusplus.multiblock.storage;

import gregtech.common.blocks.BlockMetalCasing.MetalCasingType;

/**
 * 多方块存储的三档材质。
 *
 * <p>
 * 外壳方块全部复用 GT 已有的 {@link MetalCasingType}（不新增方块）：
 * </p>
 *
 * <ul>
 * <li>{@link #STEEL} —— {@code STEEL_SOLID}（Solid Steel Casing）</li>
 * <li>{@link #CLEAN_STAINLESS_STEEL} —— {@code STAINLESS_CLEAN}（Clean Stainless Steel Casing）</li>
 * <li>{@link #REINFORCED_TITANIUM} —— {@code TITANIUM_STABLE}（Stable Titanium Casing）</li>
 * </ul>
 *
 * <p>
 * ⚠ 注意：GT 里<b>没有</b>"Reinforced Titanium Casing"这个方块，也没有"脱氧钢"，
 * 因此"加强钛"档按用户确认使用 GT 的 {@code TITANIUM_STABLE}。
 * 本枚举<b>刻意不持有任何客户端贴图</b>（{@code Textures} 只能在客户端触碰），
 * 贴图映射见 {@link SuStorageTextures}。
 * </p>
 */
public enum SuStorageTier {

    /** 钢制：外壳 STEEL_SOLID，板条箱 4,000,000 物品。 */
    STEEL("steel", MetalCasingType.STEEL_SOLID, 4_000_000L),

    /** 洁净不锈钢：外壳 STAINLESS_CLEAN，板条箱 64,000,000 物品。 */
    CLEAN_STAINLESS_STEEL("clean_stainless_steel", MetalCasingType.STAINLESS_CLEAN, 64_000_000L),

    /** 加强钛：外壳 TITANIUM_STABLE，板条箱 128,000,000 物品。 */
    REINFORCED_TITANIUM("reinforced_titanium", MetalCasingType.TITANIUM_STABLE, 128_000_000L);

    /** 注册名中间段，例如 {@code steel_multiblock_crate} 里的 {@code steel}。 */
    private final String id;

    /** 外壳方块（GT 现成机械方块）。 */
    private final MetalCasingType casing;

    /** 板条箱容量（物品数）。 */
    private final long crateCapacity;

    SuStorageTier(String id, MetalCasingType casing, long crateCapacity) {
        this.id = id;
        this.casing = casing;
        this.crateCapacity = crateCapacity;
    }

    public String getId() {
        return id;
    }

    public MetalCasingType getCasing() {
        return casing;
    }

    public long getCrateCapacity() {
        return crateCapacity;
    }
}
