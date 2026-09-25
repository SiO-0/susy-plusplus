# 防水喷漆（Waterproof Spray Can）与机器防水状态系统

> 模组：**Susy Plus Plus**（`susyplusplus`，基础包 `com.susy.plusplus`）
> 平台：Minecraft 1.12.2 / Forge 14.23.5.2847 / GregTech（参考源码 `../somemods/GregTech-master`，mod 版本 `2.8.10-beta`）

---

## 1. 功能说明

新增“防水喷漆”物品，以及 GT 机器的“防水 / 不防水”状态系统：

| 能力 | 说明 |
| --- | --- |
| 物品 | 防水喷漆，容量 **300 次**，单次消耗 **1** |
| 右键机器 | 将已放置的机器设为防水 |
| 副手放置 | 副手持有防水喷漆、主手放置机器后，机器自动防水 |
| 流体 | 新增“防水漆液”（`susyplusplus:waterproof_paint`） |
| 灌装机配方 | 空喷漆罐 x1 + 防水漆液 576 mB → 防水喷漆 x1（32 ticks / 8 EU/t） |
| 搅拌机配方 | 见 §3（160 ticks / 30 EU/t） |
| The One Probe | 显示“防水”（绿）/“不防水”（红），风格参考 GT 维护状态 |
| 真实效果 | 防水机器**不再**因水 / 降雨 / 岩浆等地形因素爆炸 |
| 持久化 | 状态写入机器 NBT，跨存档 / 重启保留 |

> **容量单位说明**：容量单位是“**使用次数**”，与 mB 无换算关系。灌装机只是把罐子“灌满”（产出满容量 300 次），
> 因此 576 mB 与 300 次之间不要求整除，与 GT 原版喷漆罐的设计一致。

---

## 2. 新增 / 修改文件列表

### Java 源码
| 文件 | 作用 |
| --- | --- |
| `src/main/java/com/susy/plusplus/SusyPlusPlus.java` | 主类；注册材料监听、初始化物品、TOP、配方 |
| `src/main/java/com/susy/plusplus/item/ItemWaterproofSprayCan.java` | 防水喷漆 MetaItem（继承 GT `StandardMetaItem`，重写模型路径） |
| `src/main/java/com/susy/plusplus/item/SuMetaItems.java` | MetaItem 注册入口 |
| `src/main/java/com/susy/plusplus/item/WaterproofSprayBehaviour.java` | 行为；继承 GT `AbstractUsableBehaviour`（容量 300，NBT `GT.UsesLeft`） |
| `src/main/java/com/susy/plusplus/waterproof/IWaterproofMachine.java` | 防水状态接口（Mixin 注入到 `MetaTileEntity`） |
| `src/main/java/com/susy/plusplus/waterproof/WaterproofHelper.java` | 机器方块实体查询工具 |
| `src/main/java/com/susy/plusplus/material/SuMaterials.java` | 材料 / 流体注册（`MaterialRegistryEvent` + `MaterialEvent`） |
| `src/main/java/com/susy/plusplus/recipe/SuRecipes.java` | 灌装机 + 搅拌机配方 |
| `src/main/java/com/susy/plusplus/event/WaterproofEventHandler.java` | `RightClickBlock` + `EntityPlaceEvent` 逻辑 |
| `src/main/java/com/susy/plusplus/integration/top/SuTopIntegration.java` | TOP 集成入口 |
| `src/main/java/com/susy/plusplus/integration/top/WaterproofInfoProvider.java` | TOP 显示防水状态 |
| `src/main/java/com/susy/plusplus/mixin/MetaTileEntityWaterproofMixin.java` | Mixin：注入防水字段 / 爆炸判定 / NBT 持久化 |

### 资源
| 文件 | 作用 |
| --- | --- |
| `src/main/resources/mixins.susyplusplus.json` | Mixin 配置（package / refmap / mixin 列表） |
| `src/main/resources/assets/susyplusplus/models/item/waterproof_spray_can.json` | 物品模型 |
| `src/main/resources/assets/susyplusplus/lang/en_us.lang` | 英文 |
| `src/main/resources/assets/susyplusplus/lang/zh_cn.lang` | 中文 |

### 配置
| 文件 | 修改 |
| --- | --- |
| `gradle.properties` | `use_mixins = true`；新增 `mixin_package = com.susy.plusplus.mixin` |
| `gradle/scripts/dependencies.gradle` | 新增 GT / TOP / CCL / ModularUI / GroovyScript 依赖与 enderio 编译期 stub |
| `libs/enderio-stub-src/.../IOverlayRenderAware.java` | 编译期最小 stub（见下） |

### 已实际写入并**编译通过**的依赖
```groovy
// GregTech CE: Unofficial (GTCEu) 1.12.2
implementation rfg.deobf('maven.modrinth:gregtech-ce-unofficial:2.8.7-beta')
// 备选：implementation rfg.deobf('curse.maven:gregtech-ce-unofficial-557242:<fileId>')

// GT 以 api 发布的传递依赖（编译其 API 必需）
implementation 'codechicken:codechickenlib:3.2.3.358'
implementation('com.cleanroommc:modularui:3.0.4') { transitive = false }
implementation('com.cleanroommc:groovyscript:1.2.0-hotfix1') { transitive = false }

// The One Probe 1.4.28
implementation rfg.deobf('curse.maven:top-245211:2667280')

// 编译期最小 stub（GT 的 MetaItem 实现 com.enderio.core.common.interfaces.IOverlayRenderAware）
compileOnly files('libs/enderio-stub-classes')
```
> `libs/enderio-stub-classes` 为目录形式的编译期 classpath 条目，由
> `javac --release 8 -d libs/enderio-stub-classes libs/enderio-stub-src/com/enderio/core/common/interfaces/IOverlayRenderAware.java`
> 生成。GT 的发布 jar 不含该接口（其 `apiPackage` 为空、apiJar 未发布），故需自行提供编译期 stub；
> 运行时由 Forge 的 `@Optional.Interface` 按 EnderCore 是否安装决定是否保留该接口。

---

## 3. 配方

### 3.1 灌装机（CANNER_RECIPES）
```
空喷漆罐 (gregtech:meta_item_1 / meta 61) x1
  + 防水漆液 576 mB   ( = GTValues.L(144) * 4 )
-> 防水喷漆 x1
时间 32 ticks，EU/t 8
```

> **适配原版 GT（`vanillaGtCompat = true`）时**：不再注册「防水漆液」材料 ——
> 上面的配方改为「空喷漆罐 x1 + **液态硅橡胶 576 mB**（`Materials.SiliconeRubber`，GT 原生流体）」，
> 且 §3.2 的搅拌机配方不再注册。详见 [`config.md`](config.md)。

### 3.2 防水漆液合成（MIXER_RECIPES，替代路线）
```
PolyvinylAcetate 1000 mB + SiliconeRubber 250 mB + Polydimethylsiloxane 粉 x1
-> 防水漆液 1152 mB
时间 160 ticks，EU/t 30
```

**材料替代理由**（源码中**不存在** 清漆 / 醇酸树脂 / 聚氨酯 / 硅油 / 石蜡 / 沥青 / 松节油）：

| 需求材料 | 采用材料 | 理由 |
| --- | --- | --- |
| 清漆 / 醇酸树脂 | `Materials.PolyvinylAcetate` | 源码中即为流体（`.fluid()`），聚醋酸乙烯酯是典型的清漆 / 胶粘剂树脂 |
| 硅油 | `Materials.SiliconeRubber` | 源码中即为流体（`.liquid(...)`），化学式 `Si(CH3)2O` 即聚二甲基硅氧烷（硅油主成分） |
| 石蜡粉 | `Materials.Polydimethylsiloxane` 粉 | 源码中带 `.dust()`，与硅油同族（硅氧烷）的固体防水填料 |
| — | `Materials.Epoxy` / `Materials.ReinforcedEpoxyResin` | 亦可作为备选树脂（源码中均为流体），可替换上面第一条 |

> 配方注册使用 `buildAndRegister()`，由 GT 依配方内容生成唯一 ID，不会与既有配方冲突。
> 由于使用 GT `RecipeMap`，JEI 会通过 GT 自带插件自动展示。

---

## 4. 防水状态 API

### 4.1 接口
```java
package com.susy.plusplus.waterproof;

public interface IWaterproofMachine {
    boolean isWaterproof();
    void setWaterproof(boolean waterproof);
}
```

### 4.2 注入方式（Mixin）
`MetaTileEntityWaterproofMixin`（`@Mixin(value = MetaTileEntity.class, remap = false)`）：

| 注入点（GT 实际源码） | 作用 |
| --- | --- |
| `getIsWeatherOrTerrainResistant()` HEAD，cancellable | 防水时返回 `true`，使 `checkWeatherOrTerrainExplosion(...)` 跳过遇水/降雨/岩浆爆炸判定 |
| `writeToNBT(NBTTagCompound)` RETURN | 写入 `susyplusplus:Waterproof` |
| `readFromNBT(NBTTagCompound)` RETURN | 读回状态（持久化） |

同时让 `MetaTileEntity` 实现 `IWaterproofMachine`，于是：
- TOP、喷漆右键、副手放置**统一**通过 `instanceof IWaterproofMachine` 读取；
- 查询入口：`WaterproofHelper.getMachine(world, pos)` / `WaterproofHelper.isWaterproof(world, pos)`。

### 4.3 已知局限
`getIsWeatherOrTerrainResistant()` 在少数 GT 子类中被覆写（`MetaTileEntityRockBreaker`、`MetaTileEntityPump`、
`MetaTileEntityFisher`、`MetaTileEntityBlockBreaker`、`MetaTileEntityLongDistanceEndpoint` 返回 `true`，本就防水；
`MetaTileEntityMultiblockPart` 走多方块控制器判定）。这些覆写不经过基类注入点。
**真正会因遇水爆炸的普通机器**（`TieredMetaTileEntity` 与 `MetaTileEntityEnergyHatch` 调用基类方法）均被正确覆盖。

---

## 5. The One Probe 显示

注册方式与 GT `TheOneProbeModule` 相同：`TheOneProbe.theOneProbeImp.registerProvider(...)`（init 阶段）。

- 防水：`TextFormatting.GREEN` + `{*info.susyplusplus.waterproof*}` → **绿色“防水”**
- 不防水：`TextFormatting.RED` + `{*info.susyplusplus.not_waterproof*}` → **红色“不防水”**
- 仅对 `instanceof IWaterproofMachine` 的机器显示。

---

### 5.1 显示豁免：这些机器**不再**显示防水提示

有些机器根本没有"防水"这个概念——它们不会参与 GT 的 `checkWeatherOrTerrainExplosion`，
给它们显示红色的"不防水"只会误导玩家。因此 `WaterproofInfoProvider` 的判定顺序是：

| 优先级 | 情况 | 显示 |
| --- | --- | --- |
| 1 | **天生防水**：机器自己覆写 `getIsWeatherOrTerrainResistant()` 返回 true（碎岩机、泵、钓鱼机、方块破坏机…） | 🟢 防水（**保留**） |
| 2 | **被喷漆刷过** | 🟢 防水 |
| 3 | **与防水无关**的机器（下表） | ⬜ **完全不显示** |
| 4 | 其余（确实会因水/地形爆炸，且没刷漆） | 🔴 不防水 |

**第 3 类的豁免清单**（`WaterproofInfoProvider#isWaterproofingIrrelevant`，用基类做
`instanceof`，因此 GT 与其它附属的子类一并覆盖）：

| 类别 | 判定的基类 |
| --- | --- |
| 蒸汽机器（单方块） | `gregtech.api.metatileentity.SteamMetaTileEntity` |
| 蒸汽多方块 | `gregtech.api.metatileentity.multiblock.RecipeMapSteamMultiblockController` |
| 大锅炉 | `gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler` |
| 桶 | `gregtech.common.metatileentities.storage.MetaTileEntityDrum` |
| 板条箱 | `gregtech.common.metatileentities.storage.MetaTileEntityCrate` |
| 超级箱 | `gregtech.common.metatileentities.storage.MetaTileEntityQuantumChest` |
| 超级缸 | `gregtech.common.metatileentities.storage.MetaTileEntityQuantumTank` |
| 大型储罐 | `gregtech.common.metatileentities.multi.MetaTileEntityMultiblockTank` |

> 想再豁免别的机器，在 `isWaterproofingIrrelevant` 里加一行 `instanceof` 即可。
> 注意：**天生防水的机器不会被豁免**（走优先级 1，照常显示绿色"防水"）。

---

## 6. 右键与副手放置行为

### 6.1 右键已放置机器
- 事件：`PlayerInteractEvent.RightClickBlock`（服务端）
- 条件：手持防水喷漆（通过行为实例判定 `WaterproofSprayBehaviour.getBehaviour(stack)`）
- 动作：目标方块实体是 GT 机器且尚未防水 → 设为防水、消耗 1 次、播放 `GTSoundEvents.SPRAY_CAN_TOOL`；
  并 `setCanceled(true)` + `setUseBlock/setUseItem(DENY)` 防止误开 GUI
- **冲突处理**：仅在手持“防水喷漆”时生效，因此**不会**破坏 GT 原有的扳手 / GUI / 旋转等交互，无需 Shift

### 6.2 副手放置自动防水
- 事件：`BlockEvent.EntityPlaceEvent`（服务端）
- 条件：`event.getEntity()` 是玩家，且副手为防水喷漆
- 动作：放置位置的 GT 机器 → 设为防水、从副手消耗 1 次、播放音效
- 同步：状态存于机器 NBT（服务端权威），由 Forge 常规机制同步；物品剩余次数由 `AbstractUsableBehaviour`
  写入物品 NBT（`GT.UsesLeft`），与原版喷漆罐一致

---

## 7. tooltip 与本地化键

物品 tooltip（[`WaterproofSprayBehaviour.addInformation`](src/main/java/com/susy/plusplus/item/WaterproofSprayBehaviour.java)）：

| 键 | en_us | zh_cn |
| --- | --- | --- |
| `metaitem.waterproof_spray_can.name` | Waterproof Spray Can | 防水喷漆 |
| `susyplusplus.tooltip.waterproof_spray_can.right_click` | Right-click a placed machine: set it waterproof | 右键已放置的机器：设为防水 |
| `susyplusplus.tooltip.waterproof_spray_can.offhand` | Hold in offhand while placing a machine: apply waterproofing automatically | 副手持有，主手放置机器：自动防水 |
| `susyplusplus.tooltip.waterproof_spray_can.uses` | Remaining uses: %,d | 剩余次数：%,d |
| `susyplusplus.material.waterproof_paint` | Waterproof Paint | 防水漆液 |
| `info.susyplusplus.waterproof` | Waterproof | 防水 |
| `info.susyplusplus.not_waterproof` | Not Waterproof | 不防水 |

> 1.12.2 使用 `.lang`（`key=value`）而非 `.json`，因此提供的是 `en_us.lang` / `zh_cn.lang`。

---

## 8. 材质路径

| 用途 | 路径 | 说明 |
| --- | --- | --- |
| 物品模型/贴图 | **完全复用 GT 原版**，本模组不再自带任何模型资源 | 见下 |
| 流体贴图 | **无需自备** | 使用 GT 自带的流体注册/贴图管线 |

**最终方案（复盘）**：物品模型直接复用 GT 原版蓝色喷漆罐：

```java
// ItemWaterproofSprayCan#createItemModelPath
return new ResourceLocation("gregtech", "metaitems/spray.can.dyes.blue");
```

对应 GT 资源：
- 模型 `assets/gregtech/models/item/metaitems/spray.can.dyes.blue.json`
- 贴图 `assets/gregtech/textures/items/metaitems/spray.can.dyes.blue.png`（1.12.2 贴图目录是 **`textures/items/`** 复数）

> **“紫黑格”根因**：GT 的 `createItemModelPath` 返回的 ResourceLocation **不带 `item/` 前缀**
> （原版查找物品模型时会自动补 `item/`）。此前写成 `susyplusplus:item/waterproof_spray_can`，
> 实际会去找 `assets/susyplusplus/models/item/item/waterproof_spray_can.json`（不存在），
> 于是 GT 的网格映射回退到 `builtin/missing`（= 紫黑格），且**不会产生任何日志**。
> 现已删除本模组的 `assets/susyplusplus/models/**`，直接指向 GT 原版模型。

> **流体贴图说明（已改为使用 GT 自带管线）**：
> 代码现为 `new Material.Builder(0, rl).liquid(new FluidBuilder().color(0x2E6FA3)).color(0x2E6FA3).build()`，
> 即完全交由 GT 的 `FluidProperty` → `GTFluidRegistration` 注册；
> 贴图由 GT 依据 `MaterialIconType.liquid` + 材质图标集自动取用 gregtech 内置流体贴图，
> 并应用流体颜色 `0x2E6FA3`，因此**不再需要** `assets/susyplusplus/textures/blocks/fluids/...` 或
> 任务书中提到的 `textures/fluid/waterproof_paint_still.png` / `_flow.png`。
>
> 如需自定义流体外观，再改用 `FluidBuilder.customStill().customFlow()` 并自备
> `assets/susyplusplus/textures/blocks/fluids/fluid.waterproof_paint.png`（+ `_flow.png`）即可。

---

## 9. 验证步骤

1. **依赖**：`gradle/scripts/dependencies.gradle` 已写好（GT/TOP/CCL/ModularUI/GroovyScript + enderio stub），刷新 Gradle 即可。
2. **编译**：`gradlew.bat compileJava` **已实测 BUILD SUCCESSFUL**；完整打包用 `gradlew.bat build`。
3. **JEI**：搜索“防水喷漆”，可见**灌装机**配方（空喷漆罐 + 防水漆液 576 mB）与**搅拌机**配方（防水漆液）。
4. **创造模式**：在 GT 默认创造标签页中可获取“防水喷漆”
   （不再引用 `gregtech.common.creativetab.GTCreativeTabs` —— 该包未打进 GT 发布 jar，故使用 `MetaItem` 的默认标签）。
5. **灌装**：灌装机放入空喷漆罐 + 576 mB 防水漆液，输出 1 个防水喷漆（tooltip 显示剩余 300 次）。
6. **右键**：对普通机器右键 → The One Probe 由红色“不防水”变为绿色“防水”，喷漆消耗 1 次。
7. **默认显示**：未处理的机器 TOP 显示红色“不防水”。
8. **防爆**：把防水机器放到水/岩浆旁或雨中（可在配置中开启 `doTerrainExplosion`），不再爆炸。
9. **副手放置**：副手拿防水喷漆、主手放置机器 → 放置后 TOP 立即显示绿色“防水”，副手消耗 1 次。
10. **tooltip**：物品 tooltip 含“右键已放置的机器：设为防水”“副手持有，主手放置机器：自动防水”“剩余次数”。
11. **持久化**：设为防水后退出重进存档 / 重启服务器，TOP 仍显示绿色“防水”。
12. **容量**：连续使用 300 次后，防水喷漆变为 GT 空喷漆罐（`MetaItems.SPRAY_EMPTY`）。

---

## 10. 备注 / 风险

- 本工程 `use_modern_java_syntax = false`（Java 8），所有代码均为 Java 8 语法。
- **源码(somemods/GregTech-master) 与发布 jar(2.8.7-beta) 存在少量 API 差异**，代码已按“发布 jar”适配（能编译、能运行）：
  - `Material` 无静态 `builder(int, ResourceLocation)` → 改用 `new Material.Builder(id, ResourceLocation)`（该构造器在 jar 中为 public）；
  - 发布 jar 不含 `gregtech.common.creativetab` 包 → 不显式设置创造标签，沿用 `MetaItem` 默认 GT 标签；
  - `OrePrefix` 的正确包名是 `gregtech.api.unification.ore.OrePrefix`（非 `gregtech.api.unification.OrePrefix`）。
- 已实测：`gradlew.bat compileJava` → `BUILD SUCCESSFUL`（仅余 CraftTweaker/GroovyScript 注解类缺失的 **警告**，不影响编译）。
- Mixin 通过 manifest `MixinConfigs`（模板机制）加载。若运行期发现 Mixin 未生效（GT 类未被改造），
  可将 `mixins.susyplusplus.json` 改为由 MixinBooter 的 `ILateMixinLoader` 延迟加载
  （参考 GT 的 `GregTechLateMixinLoadingPlugin`）。
- 配方 ID 由 GT 依配方内容生成，天然唯一；若后续新增同输入同输出配方需显式区分，避免 ID 冲突。
- 所有 GT / TOP 类名、方法名、注册名、NBT 键均取自 `../somemods` 下真实源码，未编造。

## 11. 开发环境（runClient）加载本模组

**问题**：本工程的 `run/` 目录是一个完整的 SUSY 整合包实例，FML 只会从 `run/mods`（及 `run/mods/1.12.2`）搜索模组；
RFG 的 dev 产物默认不会进入该目录，因此 `runClient` 启动后模组列表里看不到本模组（日志中也没有 `susyplusplus`）。

**修复**：在 [`gradle/scripts/extra.gradle`](gradle/scripts/extra.gradle:1) 中新增 `copyModToRunMods` 任务，
把本模组**重混淆后的生产 jar（`reobfJar`）**复制为 `run/mods/susyplusplus.jar`（固定文件名，重复运行覆盖），
并让它成为 `runClient` / `runServer` / `runObfClient` / `runObfServer` 的前置任务。

```groovy
def reobfJar = tasks.named('reobfJar')
def copyModToRunMods = tasks.register('copyModToRunMods', Copy) {
    dependsOn reobfJar
    from reobfJar
    into file('run/mods')
    rename { 'susyplusplus.jar' }
    doLast { file('run/mods/susyplusplus-dev.jar').delete() }
}
tasks.matching { it.name in ['runClient', 'runServer', 'runObfClient', 'runObfServer'] }
        .configureEach { dependsOn copyModToRunMods }
```

> **必须用 reobfJar，不能用 dev jar**：`jar` 任务产出的是 `*-dev.jar`（**MCP 名称**），
> 而本 `run/` 是生产环境（Cleanroom/**SRG 名称**）。混用会导致
> `NoSuchMethodError: 'java.lang.String net.minecraft.client.resources.I18n.format(java.lang.String, java.lang.Object[])'`
> 之类崩溃，表现为：物品 tooltip 报错、TOP 显示 `error:susyplusplus:waterproof`、放置方块崩溃。
> 已验证 reobfJar 产物中调用为 SRG 名称（如 `I18n.func_135052_a`、`ItemStack.func_190926_b`、`World.func_184148_a`）。

**使用**：关闭当前游戏进程后重新执行 `gradlew.bat runClient`（会自动先执行 reobfJar 并复制）。
也可单独执行 `gradlew.bat copyModToRunMods` 手动放入。

## 12. 运行时行为说明（爆炸 / TOP 判定 / 贴图注册）

- **“不防水的机器不爆炸”并非本模组改动**：GT 的爆炸判定在
  `MetaTileEntity.checkWeatherOrTerrainExplosion(...)`，条件为
  `ConfigHolder.machines.doTerrainExplosion`（本整合包 `run/config/gregtech/gregtech.cfg` 为 `true`）
  **且** `!getIsWeatherOrTerrainResistant()` **且** `energyContainer.getEnergyStored() != 0`（**机器必须带电**），
  然后每 tick 以 `GTValues.RNG.nextInt(1000) == 0`（约千分之一）判定，命中且六面紧邻
  水 / 流动水 / 岩浆（或处于降雨中）才爆炸。
  因此“不爆炸”的常见原因：机器没电、观察时间太短（平均需约 1000 tick ≈ 50 秒）、
  或该机器**天生抗性**（碎岩机 / 泵 / 钓鱼机 / 方块破坏机 / LongDistanceEndpoint 覆写了
  `getIsWeatherOrTerrainResistant()` 返回 `true`）。
  Mixin 仅在**我们设置了防水标记**时把该判定改为 `true`；不防水机器逻辑完全不变
  （`run/logs/cleanmix.log` 已确认 `APPLY mixins.susyplusplus.json:MetaTileEntityWaterproofMixin -> MetaTileEntity`）。
- **TOP 判定已兼容天生抗性**：改为 `防水 = isWaterproof() || getIsWeatherOrTerrainResistant()`，
  因此碎岩机等天生抗性的机器现在显示绿色“防水”。
- **物品模型**：由 [`ItemWaterproofSprayCan#createItemModelPath`](src/main/java/com/susy/plusplus/item/ItemWaterproofSprayCan.java:53)
  直接返回 GT 原版模型 `gregtech:metaitems/spray.can.dyes.blue`（**不带 `item/` 前缀**）；
  [`SuClientEvents`](src/main/java/com/susy/plusplus/client/SuClientEvents.java:1) 仍在客户端
  `ModelRegistryEvent` 阶段显式调用本物品的 `registerModels()`（幂等，避免漏注册）。
  本模组已删除 `assets/susyplusplus/models/**`，不再需要任何自带模型/贴图资源。
