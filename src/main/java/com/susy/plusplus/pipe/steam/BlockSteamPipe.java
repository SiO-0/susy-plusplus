package com.susy.plusplus.pipe.steam;

import com.susy.plusplus.Tags;
import com.susy.plusplus.client.renderer.SteamPipeRenderer;

import gregtech.api.GregTechAPI;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.ItemBlockPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * 蒸汽管道方块（低压 / 高压各一个方块实例）。
 *
 * <p>
 * 继承 GT 的 {@link BlockPipe} 而不是自己写一个普通方块，目的是直接获得：
 * 管道连接状态、扳手拆卸/遮挡面、覆盖板安装（蒸汽泵覆盖板可以装在管道上）、
 * 碰撞箱、CCL 渲染管线等一整套行为。
 * </p>
 *
 * <p>
 * 与 GT 的材质管道（{@code BlockFluidPipe}）的区别：
 * </p>
 * <ul>
 * <li><b>没有 Material 维度</b>：一个方块固定一种规格、一个速率；</li>
 * <li>节点数据是 {@link SteamPipeProperties}（而不是 {@code FluidPipeProperties}）；</li>
 * <li>网络是 {@link WorldSteamPipeNet}（而不是 {@code WorldFluidPipeNet}）。</li>
 * </ul>
 */
public class BlockSteamPipe extends BlockPipe<SteamPipeType, SteamPipeProperties, WorldSteamPipeNet> {

    private final SteamPipeType pipeType;
    private final SteamPipeProperties properties;

    public BlockSteamPipe(SteamPipeType pipeType) {
        super();
        this.pipeType = pipeType;
        this.properties = pipeType.createProperties();

        String name = pipeType.getRegistryName();
        setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        setTranslationKey(Tags.MOD_ID + "." + name);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_PIPES);
    }

    public SteamPipeType getSteamPipeType() {
        return pipeType;
    }

    public int getThroughput() {
        return pipeType.getThroughput();
    }

    // ------------------------------------------------------------------
    // BlockPipe 抽象方法
    // ------------------------------------------------------------------

    @Override
    public Class<SteamPipeType> getPipeTypeClass() {
        return SteamPipeType.class;
    }

    @Override
    public WorldSteamPipeNet getWorldPipeNet(World world) {
        return WorldSteamPipeNet.get(world);
    }

    @Override
    public TileEntityPipeBase<SteamPipeType, SteamPipeProperties> createNewTileEntity(boolean supportsTicking) {
        // 只实现了一个管道方块实体，而它的 supportsTicking() 恒为 true
        // （基类判定是 instanceof ITickable），所以这里不需要区分两种情况。
        return new TileEntitySteamPipe();
    }

    @Override
    public SteamPipeProperties createProperties(IPipeTile<SteamPipeType, SteamPipeProperties> pipeTile) {
        return pipeType.createProperties();
    }

    @Override
    public SteamPipeProperties createItemProperties(ItemStack itemStack) {
        return properties;
    }

    @Override
    protected SteamPipeProperties getFallbackType() {
        return properties;
    }

    @Override
    public SteamPipeType getItemPipeType(ItemStack itemStack) {
        return pipeType;
    }

    @Override
    public void setTileEntityData(TileEntityPipeBase<SteamPipeType, SteamPipeProperties> pipeTile,
                                 ItemStack itemStack) {
        // 关键：GT 的管道方块实体在这一步才知道自己是哪种规格。
        pipeTile.setPipeData(this, pipeType);
    }

    @Override
    public ItemStack getDropItem(IPipeTile<SteamPipeType, SteamPipeProperties> pipeTile) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == getCreativeTab()) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public boolean canPipesConnect(IPipeTile<SteamPipeType, SteamPipeProperties> selfTile, EnumFacing side,
                                   IPipeTile<SteamPipeType, SteamPipeProperties> sideTile) {
        // 低压只能接低压、高压只能接高压；
        // 同一压力下不同粗细可以互通（最细的那根自然限速），与 GT 流体管道一致。
        return sideTile instanceof TileEntitySteamPipe &&
                pipeType.samePressure(((TileEntitySteamPipe) sideTile).getPipeType());
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeTile<SteamPipeType, SteamPipeProperties> selfTile, EnumFacing side,
                                         @Nullable TileEntity tile) {
        if (tile instanceof TileEntitySteamPipe) {
            // 本模组 10 种管道是 10 个不同方块，GT 对"不同方块"的邻居走的是本方法
            // （而不是 canPipesConnect），所以压力判定必须写在这里，
            // 否则低压管会通过"邻居有流体能力"这条路连上高压管。
            // 同一压力下不同粗细要允许连接（最细者限速）。
            return pipeType.samePressure(((TileEntitySteamPipe) tile).getPipeType());
        }
        // 机器 / 储罐 / GT 流体管道：只要能取到流体能力就连
        return tile != null &&
                tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
    }

    /**
     * 玩家是否手持任意管道物品。
     *
     * <p>
     * GT 的 {@code BlockPipe#hasPipeCollisionChangingItem} 用它来决定
     * 是否把碰撞箱拆成「每面 3×3 的九宫格」（用来点击阻断某一格连接）。
     * 这里必须与 GT 一样判断<b>任意</b> {@link ItemBlockPipe}，
     * 而不是"只认本方块自己的物品"——否则手持扳手/其它管道时看不到九宫格。
     * </p>
     */
    @Override
    public boolean isHoldingPipe(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        // ⚠ 必须把静态类型写成 EntityLivingBase：
        // getHeldItemMainhand() 声明在 EntityLivingBase 上，
        // 若以 EntityPlayer 为接收者，RFG 的 reobf 不会映射该调用，
        // 运行期会抛 NoSuchMethodError（已实际踩到并修复）。
        EntityLivingBase living = player;
        ItemStack stack = living.getHeldItemMainhand();
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlockPipe;
    }

    // ------------------------------------------------------------------
    // 客户端：交给自定义 PipeRenderer
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return SteamPipeRenderer.INSTANCE.getBlockRenderType();
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected Pair<TextureAtlasSprite, Integer> getParticleTexture(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return SteamPipeRenderer.INSTANCE
                .getParticleTexture(tile instanceof IPipeTile ? (IPipeTile<?, ?>) tile : null);
    }
}
