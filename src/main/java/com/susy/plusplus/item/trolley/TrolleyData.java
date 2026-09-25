package com.susy.plusplus.item.trolley;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * 手推车物品的 NBT 读写。
 *
 * <h2>NBT 结构</h2>
 *
 * <pre>
 * 物品根 NBT
 *  └─ StoredMachine（compound）
 *      ├─ RegistryName  : "gregtech:macerator"   ← MetaTileEntity#metaTileEntityId.toString()
 *      ├─ BlockState    : NBTUtil.writeBlockState(world.getBlockState(pos))
 *      ├─ BlockEntityNBT: MetaTileEntity#writeToNBT(...) 的完整结果
 *      │                  （朝向 / 喷漆 / 静音 / 封面 / 物品与流体缓存 / 各 MTETrait）
 *      └─ DisplayKey    : "gregtech.machine.macerator.lv.name"（客户端 I18n 翻译用）
 * </pre>
 *
 * <p>
 * <b>为什么存 {@code DisplayKey} 而不是名字本身</b>：搬起发生在<b>服务端</b>，而服务端的
 * {@code net.minecraft.util.text.translation.I18n} 没有语言表（会返回原始键），所以只存
 * <b>键</b>，等客户端画 tooltip 时再翻译。键的构造方式与 GT 的
 * {@code MachineItemBlock} / {@code MetaItem} 一致（{@code getMetaName() + ".name"}）。
 * </p>
 *
 * <p>
 * <b>为什么 {@code BlockEntityNBT} 里没有坐标</b>：GT 的 {@code MetaTileEntity#writeToNBT}
 * 不写 x/y/z 与维度（{@code MetaId} 是 {@code MetaTileEntityHolder} 另一层写的），
 * 因此放置时无需清理坐标，见 {@link TrolleyHelper}。
 * </p>
 */
public final class TrolleyData {

    /** 根键：装载的机器。 */
    public static final String STORED_MACHINE = "StoredMachine";

    /** 机器注册名（{@code MetaTileEntity#metaTileEntityId}），放置前用它查样板与校验。 */
    public static final String REGISTRY_NAME = "RegistryName";

    /** 方块状态（{@code NBTUtil.writeBlockState} 的结果）。 */
    public static final String BLOCK_STATE = "BlockState";

    /** 完整机器 BlockEntity NBT（{@code MetaTileEntity#writeToNBT} 的结果）。 */
    public static final String BLOCK_ENTITY_NBT = "BlockEntityNBT";

    /** 名字的本地化键（{@code getMetaName() + ".name"}）。 */
    public static final String DISPLAY_KEY = "DisplayKey";

    private TrolleyData() {
    }

    /** 取 {@code StoredMachine} 复合标签；没有装载机器时返回 {@code null}。 */
    public static NBTTagCompound getStored(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(STORED_MACHINE, 10)) {
            return null;
        }
        return root.getCompoundTag(STORED_MACHINE);
    }

    /** 手推车里是否已经装载了机器。 */
    public static boolean hasMachine(ItemStack stack) {
        return getStored(stack) != null;
    }

    /** 取机器注册名；缺失或非法时返回 {@code null}。 */
    public static ResourceLocation getRegistryName(ItemStack stack) {
        NBTTagCompound stored = getStored(stack);
        if (stored == null || !stored.hasKey(REGISTRY_NAME, 8)) {
            return null;
        }
        String raw = stored.getString(REGISTRY_NAME);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (RuntimeException e) {
            // ResourceLocation 对非法字符串会抛异常（存档被人为改坏时不要崩游戏）
            return null;
        }
    }

    /** 取名字的本地化键（客户端翻译用）。 */
    public static String getDisplayKey(ItemStack stack) {
        NBTTagCompound stored = getStored(stack);
        if (stored == null || !stored.hasKey(DISPLAY_KEY, 8)) {
            return null;
        }
        String key = stored.getString(DISPLAY_KEY);
        return key.isEmpty() ? null : key;
    }

    /** 取完整机器 NBT。 */
    public static NBTTagCompound getBlockEntityNbt(ItemStack stack) {
        NBTTagCompound stored = getStored(stack);
        if (stored == null || !stored.hasKey(BLOCK_ENTITY_NBT, 10)) {
            return null;
        }
        return stored.getCompoundTag(BLOCK_ENTITY_NBT);
    }

    /** 写入装载的机器（覆盖旧的）。 */
    public static void store(ItemStack stack, NBTTagCompound stored) {
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }
        root.setTag(STORED_MACHINE, stored);
    }

    /** 取出（放置成功后调用）。空的根标签会一并移除，保持物品 NBT 干净。 */
    public static void clear(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        root.removeTag(STORED_MACHINE);
        if (root.isEmpty()) {
            stack.setTagCompound(null);
        }
    }
}
