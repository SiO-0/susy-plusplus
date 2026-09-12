package com.enderio.core.common.interfaces;

/**
 * 编译期最小 stub，与 GregTech 源码 {@code src/api/java} 中的同名接口对应。
 *
 * <p>
 * 背景：GT 的 {@code gregtech.api.items.metaitem.MetaItem} 声明
 * {@code implements ... IOverlayRenderAware}，并用 Forge 的
 * {@code @Optional.Interface(modid = "endercore", iface = "com.enderio.core.common.interfaces.IOverlayRenderAware")}
 * 标注；运行时若未安装 EnderCore，Forge 会移除该接口。但 GT 的发布 jar 不含这个 api stub
 * （其 {@code apiPackage} 为空，apiJar 未发布），因此**编译期**必须有该类型才能解析 MetaItem 的类层次。
 * </p>
 *
 * <p>
 * 本 stub 仅通过 {@code compileOnly} 参与编译，不会打进本模组 jar，也不会出现在运行时。
 * </p>
 */
public interface IOverlayRenderAware {
}
