# 配置文件（Config）

> Susy Plus Plus 使用 Forge 1.12.2 的注解式配置。
> 对应的类：[`SuConfig.java`](../src/main/java/com/susy/plusplus/config/SuConfig.java)

---

## 1. 文件位置

```
.minecraft/config/susyplusplus.cfg
```

首次启动时由 Forge 自动生成。

> ⚠ 这些都是**加载期开关**（物品 / 方块 / 机器的注册无法运行时热插拔）。
> 改完之后**必须重启游戏**才会生效。

---

## 2. 选项一览

| 选项 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `enableWaterproofSprayCan` | boolean | `true` | 防水喷漆物品，以及灌装机 / 搅拌机的相关配方 |
| `enableBatteryCase` | boolean | `true` | 电池盒物品，以及其组装机配方 |
| `enableReinforcedPbf` | boolean | `true` | 强化土高炉多方块机器，以及其工作台配方（强化耐火砖方块始终可用） |
| `reinforcedPbfParallel` | int (1~64) | `4` | 强化土高炉的并行数 |
| `enableWirelessEnergyTower` | boolean | `true` | 无线能量传输塔多方块机器（见 [`wireless_energy_tower.md`](wireless_energy_tower.md)） |
| `enableConfigurator` | boolean | `true` | 配置器物品（Shift+V 打开界面；见 [`configurator.md`](configurator.md)） |
| `enableTrolley` | boolean | `true` | 手推车物品（Shift+右键搬起机器 / 右键放下；见 [`trolley.md`](trolley.md)） |
| `enableStorageScanner` | boolean | `true` | 存储检测器机器（MV 扫描周围容器并聚合暴露库存；见 [`storage_scanner.md`](storage_scanner.md)） |
| `enableRubberPipeTweaks` | boolean | `false` | 橡胶管道修改（见 §3） |
| `enablePyrotechRecipeTweaks` | boolean | `true` | 火种科技(Pyrotech) 相关配方（见 §4） |

### 被关闭时会发生什么

- 对应物品/机器**不注册**，其静态字段保持 `null`，相关**配方也不再注册**；
- 依赖它的代码全部做了空判断，不会崩溃；
- 日志里会出现一条明确的 `... is DISABLED in config.` 提示；
- 客户端模型注册对"因配置而关闭"的物品**不再刷 WARN**（避免误导）。

---

## 3. 橡胶管道修改（`enableRubberPipeTweaks`）

包含两件事：

### 3.1 橡胶流体管道「速率」改成与钢一致

- 代码位置：`SuPipeTweaks#applyRubberFluidPipeThroughput`（由 `SusyPlusPlus#preInit` 调用）
- 只改**速率（throughput）**，温度 / 气密 / 耐酸 / 耐低温 / 等离子等其它参数**保持原样**
- 钢的速率是**运行时从 `Materials.Steel` 读出来的**，没有写死数字

```java
FluidPipeProperties rubber = Materials.Rubber.getProperty(PropertyKey.FLUID_PIPE);
rubber.setThroughput(Materials.Steel.getProperty(PropertyKey.FLUID_PIPE).getThroughput());
```

#### ❗ 为什么**不能**写在 `MaterialEvent` 里（实战踩坑记录）

最初把这段逻辑放在 `MaterialEvent`（甚至用了 `EventPriority.LOWEST`），**完全不生效**。
实际日志（`run/logs/latest.log`）给出了原因：

```
[SusyPlusPlus] Rubber fluid pipe tweak skipped: Rubber.hasFluidPipe=false, Steel.hasFluidPipe=true
[GroovyLog]: Registering new properties          <-- GroovyScript 紧接着才给橡胶补上 FLUID_PIPE
```

**GT 触发 `MaterialEvent` 时，`Materials.Rubber` 还没有 `FLUID_PIPE` 属性** ——
橡胶管道是本整合包用 GroovyScript 在**之后**补上的。
所以无论用什么事件优先级，只要还待在 `MaterialEvent` 里就永远拿不到它。

#### 正确做法：preInit 里改 + 重新写入管道方块

改到**本模组的 preInit**（一定晚于 GT 的 preInit，此时属性与管道方块都已就绪），
并且除了改材料属性，还要把改好的属性**重新写回每个流体管道方块**：

```java
rubber.setThroughput(steel.getThroughput());

// 反射访问 MetaBlocks.FLUID_PIPES，逐个重新写入（覆盖式）
pipe.addPipeMaterial(Materials.Rubber, rubber);
```

为什么"重新写入"有效：`BlockMaterialPipe#addPipeMaterial` 本质上就是
`enabledMaterials.put(material, properties)` —— **幂等、可覆盖**；
而各尺寸的数值由 `FluidPipeType` 按 `capacityMultiplier` 现算
（`TINY=1, SMALL=2, NORMAL=6, LARGE=12, HUGE=24`，所以**微型管显示的就是材料基础速率**），
重新写入后所有尺寸立刻生效。

#### 为什么用反射

GT 的 `BlockPipe` 实现了 `team.chisel.ctm.api.IFacade`（Chisel CTM），
而 Chisel 不在本工程编译类路径上 —— 直接引用 `BlockFluidPipe` 会编译失败
（`无法访问 team.chisel.ctm.api.IFacade`）。
用反射访问 `MetaBlocks.FLUID_PIPES` 可完全避开该依赖，也不必为此再加 stub。

### 3.2 合金炉配方：橡胶锭 + 对应【钢】模头（不消耗）

用 GT 自己 `PipeRecipeHandler` 同款的 `notConsumable(SHAPE_EXTRUDER_PIPE_*)` 写法，
按需求把机器换成**合金炉**、`EUt(7)`：

| 产物 | 输入 | 模头（不消耗） |
| --- | --- | --- |
| 微型橡胶流体管道 `pipeTinyFluid` | 2x 橡胶锭 | `SHAPE_EXTRUDER_PIPE_TINY` |
| 小型橡胶流体管道 `pipeSmallFluid` | 1x 橡胶锭 | `SHAPE_EXTRUDER_PIPE_SMALL` |
| 普通橡胶流体管道 `pipeNormalFluid` | 3x 橡胶锭 | `SHAPE_EXTRUDER_PIPE_NORMAL` |
| 大型橡胶流体管道 `pipeLargeFluid` | 6x 橡胶锭 | `SHAPE_EXTRUDER_PIPE_LARGE` |
| 巨型橡胶流体管道 `pipeHugeFluid` | 12x 橡胶锭 | `SHAPE_EXTRUDER_PIPE_HUGE` |

- 机器：`RecipeMaps.ALLOY_SMELTER_RECIPES`
- `duration(100)`、`EUt(7)`
- 代码位置：`SuRecipes#registerRubberPipeRecipes`

### ⚠ 前置条件（重要）

**GTCEu 本体并没有给 `Materials.Rubber` 加 `ingot` / `FLUID_PIPE` 属性**
（本体只给了 `polymer/liquid` + `GENERATE_GEAR/RING/FOIL/BOLT_SCREW`）。
橡胶锭与橡胶流体管道是由**本整合包**（SUSY / GroovyScript）提供的。

因此上面的代码是**防御式**的：

- 若 `OrePrefix.ingot + Materials.Rubber`（橡胶锭）没有对应物品 → 只打一条 WARN 并跳过全部配方；
- 若某个尺寸的管道物品不存在 → 跳过那一条配方。

日志里的提示长这样：

```
[SusyPlusPlus] Skip rubber pipe recipes: no item for OrePrefix.stick + Materials.Rubber.
[SusyPlusPlus] Rubber fluid pipe tweak skipped: Rubber.hasFluidPipe=..., Steel.hasFluidPipe=...
```

看到这些 WARN 就说明：**要么整合包没有提供橡胶管道/橡胶条，要么它们的形态不是这里假设的**。
把实际物品名告诉我，改一行即可。

---

## 4. 火种科技(Pyrotech) 配方（`enablePyrotechRecipeTweaks`）

控制本模组添加的三条 Pyrotech 相关配方：

| 机器 | 输入 | 输出 |
| --- | --- | --- |
| 干燥机 `SuSyRecipeMaps.DRYER_RECIPES` | `pyrotech:material:13` | `pyrotech:material:12` |
| 提取机 `RecipeMaps.EXTRACTOR_RECIPES` | `cobblestone` | `pyrotech:rock` x8 |
| 锻造锤 `RecipeMaps.FORGE_HAMMER_RECIPES` | `minecraft:flint` | `pyrotech:material:10` x3 |

关闭 `enablePyrotechRecipeTweaks` 后这三条都不再注册（对整合包原有的 Pyrotech 内容无任何影响）。

---

## 5. 与其它模组的关系

- 配置只控制**本模组（susyplusplus）自己**添加/修改的内容；
- 关闭任一项**不会**影响 GT、SUSY、Pyrotech 等其它模组的原有内容；
- 不修改 `somemods` 下的任何源码。
