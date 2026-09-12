package com.susy.plusplus.block;

import gregtech.api.GregTechAPI;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 强化耐火砖方块。
 *
 * <p>
 * 属性对齐 GT 原版耐火砖（{@code gregtech.common.blocks.BlockMetalCasing} 的
 * {@code MetalCasingType.PRIMITIVE_BRICKS} 变体）：
 * </p>
 *
 * <table border="1">
 * <tr>
 * <th>属性</th>
 * <th>GT 原版</th>
 * <th>本方块</th>
 * </tr>
 * <tr>
 * <td>Block Material</td>
 * <td>{@code Material.IRON}</td>
 * <td>同</td>
 * </tr>
 * <tr>
 * <td>hardness / resistance</td>
 * <td>5.0F / 10.0F</td>
 * <td>同</td>
 * </tr>
 * <tr>
 * <td>SoundType</td>
 * <td>STONE</td>
 * <td>同</td>
 * </tr>
 * <tr>
 * <td>挖掘工具 / 等级</td>
 * <td>wrench / 1</td>
 * <td>wrench 1（另加 pickaxe 1 便于手动拆）</td>
 * </tr>
 * <tr>
 * <td>生物生成</td>
 * <td>不允许</td>
 * <td>同</td>
 * </tr>
 * </table>
 *
 * <p>
 * <b>说明：</b>GT 原版这类外壳方块只认 {@code wrench}（{@code ToolClasses.WRENCH}）。
 * 这里额外注册 {@code pickaxe} 等级 1，这样用镐也能正常挖掉并掉落，避免玩家拆结构时"挖不下来"。
 * </p>
 */
public class BlockReinforcedFirebrick extends Block {

    public BlockReinforcedFirebrick() {
        super(Material.IRON);
        setTranslationKey("reinforced_firebrick");
        setHardness(5.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("wrench", 1);
        setHarvestLevel("pickaxe", 1);
        // GT 的装饰/外壳类方块都放在这个创造模式页
        setCreativeTab(GregTechAPI.TAB_GREGTECH_DECORATIONS);
    }

    /** 与 GT 原版耐火砖一致：不允许生物在上方生成。 */
    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, SpawnPlacementType type) {
        return false;
    }
}
