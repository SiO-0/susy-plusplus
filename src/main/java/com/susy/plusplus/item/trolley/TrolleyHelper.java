package com.susy.plusplus.item.trolley;

import com.susy.plusplus.SusyPlusPlus;

import gregtech.api.GregTechAPI;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.GTUtility;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * 手推车的「搬起 / 放下」逻辑（<b>只在服务端执行</b>）。
 *
 * <h2>搬起（Shift + 右键机器）</h2>
 *
 * <ol>
 * <li>先 <b>完整</b> 序列化：{@code mte.writeToNBT(...)} + {@code metaTileEntityId} +
 * {@code NBTUtil.writeBlockState} → <b>写进手推车物品</b>（先写物品，再删方块，
 * 这样即使中途出错也不会丢机器）；</li>
 * <li>调用 {@code mte.onRemoval()}（GT 的"被移除"清理：多方块解除注册、网络重建等，
 * 已逐个核对 2.8.x 的实现，<b>都不会生成物品实体</b>）；</li>
 * <li>{@code world.setBlockToAir(pos)} —— <b>故意不走</b> {@code BlockMachine#breakBlock} /
 * {@code getDrops}，因此 {@code clearMachineInventory}（物品/内容物掉落）、
 * {@code dropAllCovers}（封面掉落）、{@code getDrops} <b>全部不会执行 → 零掉落</b>。
 * 封面本身已被第 1 步的 {@code CoverSaveHandler} 写进 NBT，放置时会恢复。</li>
 * </ol>
 *
 * <h2>放下（右键）</h2>
 *
 * <p>
 * 用 {@code GregTechAPI.MTE_REGISTRY} 按 {@code RegistryName} 取样板 MTE →
 * {@code setBlockState} → {@code IGregTechTileEntity#setMetaTileEntity(sample)} →
 * {@code readFromNBT(...)} → {@code onPlacement()}，最后清空手推车 NBT。
 * 朝向来自 NBT 里的 {@code FrontFacing}（不按玩家朝向重设），因此摆回去还是原来的朝向。
 * </p>
 *
 * <p>
 * ⚠ <b>2.8.x 与 master 的差异（已用 javap 核对）</b>：2.8.x 只有
 * {@code IGregTechTileEntity#setMetaTileEntity(MetaTileEntity)} 单参重载，
 * 且没有 {@code MetaTileEntity#getRegistry()/getBlock()}、
 * 没有 {@code MTERegistry/MTEManager/mteManager}。所以这里用
 * {@code GregTechAPI.MTE_REGISTRY} + {@code getStackForm()} 反查方块，绝不用 master 专有 API。
 * </p>
 */
public final class TrolleyHelper {

    /** 多块控制器/部件不允许搬起。 */
    private static final String MSG_DENIED_STRUCTURE = "susyplusplus.message.trolley.multiblock_denied";

    private TrolleyHelper() {
    }

    /**
     * 搬起机器。
     *
     * @return {@link EnumActionResult#SUCCESS} = 本模组已处理（调用方应取消事件）；
     *         {@link EnumActionResult#PASS} = 不是 GT 机器或不是服务端，<b>完全不干预</b>
     *         （保留 GT / 原版行为）。
     */
    public static EnumActionResult pickUp(World world, BlockPos pos, ItemStack trolley, EntityPlayer player) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        MetaTileEntity mte = getMachine(world, pos);
        if (mte == null) {
            // 不是 GT 机器方块 → 放行（不提示、不拦截，避免影响其它模组的方块）
            return EnumActionResult.PASS;
        }
        if (TrolleyData.hasMachine(trolley)) {
            message(player, "susyplusplus.message.trolley.already_loaded");
            return EnumActionResult.SUCCESS;
        }
        if (mte instanceof MultiblockControllerBase || mte instanceof IMultiblockPart) {
            // 多方块控制器/部件：默认禁止（控制器搬走后整个结构必须重新成型；
            // 部件（仓室/总线）的 onRemoval() 还可能把内容物 flush 出来）
            message(player, MSG_DENIED_STRUCTURE);
            return EnumActionResult.SUCCESS;
        }

        // ---- 1) 先完整序列化（失败也不会丢机器）----
        NBTTagCompound blockEntityNbt = new NBTTagCompound();
        mte.writeToNBT(blockEntityNbt);

        NBTTagCompound stored = new NBTTagCompound();
        stored.setString(TrolleyData.REGISTRY_NAME, mte.metaTileEntityId.toString());
        NBTUtil.writeBlockState(stored, world.getBlockState(pos));
        stored.setTag(TrolleyData.BLOCK_ENTITY_NBT, blockEntityNbt);
        stored.setString(TrolleyData.DISPLAY_KEY, mte.getMetaName() + ".name");
        TrolleyData.store(trolley, stored);

        // ---- 2) GT 语义上的"机器被移除"清理（无掉落物）----
        mte.onRemoval();

        // ---- 3) 直接删方块：绕过 breakBlock/getDrops → 零掉落 ----
        world.setBlockToAir(pos);

        SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Trolley picked up {} at {}", mte.metaTileEntityId, pos);
        message(player, "susyplusplus.message.trolley.picked_up");
        return EnumActionResult.SUCCESS;
    }

    /**
     * 放下机器。
     *
     * @return {@link EnumActionResult#SUCCESS} = 已处理（或装载数据损坏，已就地清理）；
     *         {@link EnumActionResult#PASS} = 手推车是空的（<b>不拦截</b>，保留 GT 右键开 GUI）。
     */
    public static EnumActionResult place(World world, BlockPos clickedPos, EnumFacing side, ItemStack trolley,
            EntityPlayer player) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        if (!TrolleyData.hasMachine(trolley)) {
            // 默认策略：空车不拦截，保留 GT 原版"右键机器开 GUI"的习惯
            return EnumActionResult.PASS;
        }

        ResourceLocation id = TrolleyData.getRegistryName(trolley);
        NBTTagCompound blockEntityNbt = TrolleyData.getBlockEntityNbt(trolley);
        if (id == null || blockEntityNbt == null) {
            TrolleyData.clear(trolley);
            message(player, "susyplusplus.message.trolley.corrupted");
            return EnumActionResult.SUCCESS;
        }

        MetaTileEntity sample = GregTechAPI.MTE_REGISTRY.getObject(id);
        if (sample == null) {
            message(player, "susyplusplus.message.trolley.unknown_machine", id.toString());
            return EnumActionResult.SUCCESS;
        }
        Block block = Block.getBlockFromItem(sample.getStackForm().getItem());
        if (!(block instanceof BlockMachine)) {
            message(player, "susyplusplus.message.trolley.unknown_machine", id.toString());
            return EnumActionResult.SUCCESS;
        }

        // 与 GT 放置一致：优先放在被点击面的相邻格，退回到被点击格（可替换方块，如草）
        BlockPos targetPos = side == null ? clickedPos : clickedPos.offset(side);
        if (!canPlaceAt(world, targetPos)) {
            if (side == null || !canPlaceAt(world, clickedPos)) {
                message(player, "susyplusplus.message.trolley.no_space");
                return EnumActionResult.SUCCESS;
            }
            targetPos = clickedPos;
        }

        IBlockState state = resolveState(trolley, block, sample);
        if (!world.setBlockState(targetPos, state, 3)) {
            message(player, "susyplusplus.message.trolley.no_space");
            return EnumActionResult.SUCCESS;
        }

        TileEntity tileEntity = world.getTileEntity(targetPos);
        if (!(tileEntity instanceof IGregTechTileEntity)) {
            world.setBlockToAir(targetPos);
            message(player, "susyplusplus.message.trolley.failed");
            return EnumActionResult.SUCCESS;
        }

        MetaTileEntity placed = ((IGregTechTileEntity) tileEntity).setMetaTileEntity(sample);
        if (placed == null) {
            world.setBlockToAir(targetPos);
            message(player, "susyplusplus.message.trolley.failed");
            return EnumActionResult.SUCCESS;
        }

        // 恢复整机 NBT（朝向自动来自 NBT 的 FrontFacing）
        placed.readFromNBT(blockEntityNbt);
        // ⚠ 2.8.x 的 onPlacement() 是【无参】版本（javap 实测）；
        //   带 placer 的重载 onPlacement(EntityLivingBase) 是 master 新增的，这里不能用。
        //   GT 2.8.x 自己的放置流程（BlockMachine#onBlockPlacedBy）最后也是调这个无参版本。
        placed.onPlacement();

        TrolleyData.clear(trolley);
        SusyPlusPlus.LOGGER.info("[SusyPlusPlus] Trolley placed {} at {}", id, targetPos);
        message(player, "susyplusplus.message.trolley.placed");
        return EnumActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ 工具

    /** 取 GT 机器（严格限定为 GT 的 {@link BlockMachine} 方块）。 */
    private static MetaTileEntity getMachine(World world, BlockPos pos) {
        if (!(world.getBlockState(pos).getBlock() instanceof BlockMachine)) {
            return null;
        }
        return GTUtility.getMetaTileEntity(world, pos);
    }

    /** 目标位置是否可放置（空气 / 可替换且没有方块实体）。 */
    private static boolean canPlaceAt(World world, BlockPos pos) {
        if (world.isOutsideBuildHeight(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.AIR) {
            return true;
        }
        if (world.getTileEntity(pos) != null) {
            return false;
        }
        return state.getBlock().isReplaceable(world, pos);
    }

    /**
     * 决定放置用的方块状态：优先用存档里的 {@code BlockState}（若方块一致），
     * 再用 {@code sample.isOpaqueCube()} 覆盖 {@code opaque} 属性 —— 与 GT 的
     * {@code MachineItemBlock#placeBlockAt} 做法一致，避免"同步到客户端之前"渲染出错。
     */
    private static IBlockState resolveState(ItemStack trolley, Block block, MetaTileEntity sample) {
        IBlockState state = block.getDefaultState();
        NBTTagCompound stored = TrolleyData.getStored(trolley);
        if (stored != null && stored.hasKey(TrolleyData.BLOCK_STATE, 10)) {
            try {
                IBlockState saved = NBTUtil.readBlockState(stored.getCompoundTag(TrolleyData.BLOCK_STATE));
                if (saved != null && saved.getBlock() == block) {
                    state = saved;
                }
            } catch (RuntimeException e) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] Trolley: invalid stored BlockState, using default state.", e);
            }
        }
        return state.withProperty(BlockMachine.OPAQUE, sample.isOpaqueCube());
    }

    private static void message(EntityPlayer player, String langKey, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(langKey, args), true);
    }
}
