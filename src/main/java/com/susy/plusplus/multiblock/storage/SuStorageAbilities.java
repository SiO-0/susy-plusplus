package com.susy.plusplus.multiblock.storage;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;

import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 本模组新增的多方块能力（Ability）。
 *
 * <p>
 * GT 2.8.x 的 {@code MultiblockAbility} <b>只有 {@code MultiblockAbility(String)} 一个构造器</b>
 * （没有 {@code Class<T>} 参数、没有 {@code ability(...)} 工厂、也没有 {@code checkType}），
 * 因此自定义能力就是直接 {@code new MultiblockAbility<>(name)} 并持有一个静态单例。
 * </p>
 *
 * <p>
 * 名称必须全局唯一：GT 自己用 {@code tank_valve}、{@code import_items} 等，
 * 这里统一加 {@code susyplusplus_} 前缀，避免与其它附属冲突。
 * </p>
 */
public final class SuStorageAbilities {

    /**
     * 物品阀门：把多方块控制器的物品库存<b>双向</b>对外暴露（类似储罐阀门的 {@code TANK_VALVE}）。
     *
     * <p>
     * 不使用 GT 的 {@code IMPORT_ITEMS}/{@code EXPORT_ITEMS}：那两个语义是"输入总线/输出总线"，
     * 而阀门要的是"同一份库存的取放口"，与储罐阀门一致。
     * </p>
     */
    public static final MultiblockAbility<IItemHandlerModifiable> ITEM_VALVE =
            new MultiblockAbility<>("susyplusplus_item_valve");

    private SuStorageAbilities() {
    }
}
