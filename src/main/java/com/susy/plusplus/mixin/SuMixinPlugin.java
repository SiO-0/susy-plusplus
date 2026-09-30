package com.susy.plusplus.mixin;

import net.minecraftforge.fml.common.Loader;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin 配置插件：按"目标 mod 是否存在"决定要不要应用对应的补丁。
 *
 * <p>
 * 为什么必须要有它：我们的 {@code mixins.susyplusplus.json} 是 {@code "required": true}，
 * 一旦某个 mixin 的目标类在这套整合包里根本不存在（比如没装 XNet / HoloInventory），
 * Mixin 会把"required mixin 未能应用"当成错误 —— 直接崩溃。
 * 用本插件在 {@code shouldApplyMixin} 里提前返回 {@code false}，就能做到
 * <b>"装了就补、没装就完全跳过"</b>，不会因为整合包差异而炸游戏。
 * </p>
 *
 * <p>
 * 覆盖范围只限本模组自己的补丁包名：
 * </p>
 *
 * <ul>
 * <li>{@code com.susy.plusplus.mixin.xnet.*} → 需要 modid {@code xnet}</li>
 * <li>{@code com.susy.plusplus.mixin.mcjtylib.*} → 需要 modid {@code xnet}
 * （mcjtylib 是 XNet 的前置库；补丁内容是 {@code xnet.*} 语言键的显示翻译，只有 XNet 用得到）</li>
 * <li>{@code com.susy.plusplus.mixin.holoinventory.*} → 需要 modid {@code holoinventory}</li>
 * <li>其它（本模组自身的 GT mixin）→ 永远应用</li>
 * </ul>
 */
public class SuMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // 无需初始化
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.susy.plusplus.mixin.xnet")) {
            return Loader.isModLoaded("xnet");
        }
        if (mixinClassName.startsWith("com.susy.plusplus.mixin.mcjtylib")) {
            // mcjtylib 是 XNet 的前置库，补丁只服务于 XNet 的 xnet.* 语言键
            return Loader.isModLoaded("xnet");
        }
        if (mixinClassName.startsWith("com.susy.plusplus.mixin.holoinventory")) {
            return Loader.isModLoaded("holoinventory");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // 无需处理
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 无前置处理
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // 无后置处理
    }
}
