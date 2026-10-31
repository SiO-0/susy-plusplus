package com.susy.plusplus.client;

import com.susy.plusplus.SusyPlusPlus;
import com.susy.plusplus.config.SuConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 「Susy-Core 2.0 中文补充」的可开关加载器（默认关闭）。
 *
 * <h3>为什么翻译默认不生效</h3>
 *
 * <p>
 * Minecraft 会**无条件**加载资源包里所有 {@code assets/<modid>/lang/<locale>.lang}，
 * Forge 没有"按文件开关"的机制。因此本模组把这份补充翻译<b>不放在 {@code lang/} 目录</b>，
 * 而是作为普通资源放在
 * {@code assets/susyplusplus/lang_override/susy_zh_cn.lang} —— MC 永远不会把它当语言文件读取。
 * </p>
 *
 * <p>
 * 只有当配置 {@code enableSusyTranslations = true} 时，本类才会在<b>每次资源包重载后</b>
 * 把该文件解析成 {@code key=value}，并<b>直接写入客户端当前 {@code Language} 的语言表</b>
 * （`net.minecraft.client.resources.Language` 内部那张 {@code Map<String, String>}）。
 * </p>
 *
 * <h3>为什么用反射，而不写死字段名</h3>
 *
 * <p>
 * 该字段在开发环境叫 {@code languageList}，而生产环境（SRG）叫 {@code field_74805_a}。
 * 为了同一份代码在两种环境都可用，这里<b>不按名字取字段</b>：遍历 {@code Language} 的所有字段，
 * 找出类型可赋值给 {@link Map} 的那一个（`Language` 只有一个这样的字段）。
 * 反射失败时只打一条 WARN，绝不崩溃。
 * </p>
 *
 * <p>
 * ⚠ 仅客户端有效；专用服务器根本没有客户端语言表，因此本类在服务端不会被加载
 * （{@code @SideOnly(CLIENT)} + 只在 {@link SuClientEvents} 里注册）。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class SusyLangOverride implements IResourceManagerReloadListener {

    /** 以 classpath 根为起点的资源路径（jar 内 / dev resources 均可）。 */
    private static final String RESOURCE = "/assets/susyplusplus/lang_override/susy_zh_cn.lang";

    private static boolean registered;

    /** 解析结果缓存（资源文件在运行期不变）。 */
    private static Map<String, String> cache;

    /** 由 {@link SuClientEvents#init()} 在客户端 preInit 调用一次。 */
    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.getResourceManager() instanceof IReloadableResourceManager)) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] SusyLangOverride: no reloadable resource manager, skipping.");
            return;
        }
        ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(new SusyLangOverride());
        SusyPlusPlus.LOGGER.info(
                "[SusyPlusPlus] SusyLangOverride registered (enableSusyTranslations={}).",
                SuConfig.enableSusyTranslations);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        if (!SuConfig.enableSusyTranslations) {
            return;
        }
        try {
            Map<String, String> translations = load();
            if (translations.isEmpty()) {
                SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] SusyLangOverride: no translations found at {}.", RESOURCE);
                return;
            }
            int added = inject(translations);
            if (added >= 0) {
                SusyPlusPlus.LOGGER.info(
                        "[SusyPlusPlus] SusyLangOverride: injected {} Susy zh_cn translation(s).", added);
            } else {
                SusyPlusPlus.LOGGER.warn(
                        "[SusyPlusPlus] SusyLangOverride: could not find the client language map (nothing injected).");
            }
        } catch (Throwable t) {
            SusyPlusPlus.LOGGER.warn("[SusyPlusPlus] SusyLangOverride: injection failed.", t);
        }
    }

    /** 从 jar / classpath 读取并解析 {@code key=value}。 */
    private static Map<String, String> load() throws Exception {
        if (cache != null) {
            return cache;
        }
        Map<String, String> map = new LinkedHashMap<>();
        try (InputStream in = SusyLangOverride.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                cache = map;
                return map;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1);
                // 先出现的键优先（与 .lang 的加载语义一致）
                map.putIfAbsent(key, value);
            }
        }
        cache = map;
        return map;
    }

    /**
     * 把翻译写进客户端当前语言的内部 Map。
     *
     * @return 新增的键数量；找不到语言表时返回 {@code -1}
     */
    @SuppressWarnings("unchecked")
    private static int inject(Map<String, String> translations) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getLanguageManager() == null) {
            return -1;
        }
        Object language = mc.getLanguageManager().getCurrentLanguage();
        if (language == null) {
            return -1;
        }
        for (Field field : language.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(language);
                if (!(value instanceof Map)) {
                    continue;
                }
                Map<Object, Object> map = (Map<Object, Object>) value;
                int before = map.size();
                map.putAll(translations);
                return map.size() - before;
            } catch (Throwable ignored) {
                // 换下一个候选字段
            }
        }
        return -1;
    }
}
