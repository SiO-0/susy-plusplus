# 蒸汽泵覆盖板 / 蒸汽管道 / Susy 翻译开关

> 本文覆盖三件事：
> 1. **Susy-Core 2.0 中文补充翻译改为默认关闭**（`enableSusyTranslations`）；
> 2. **蒸汽泵覆盖板**（1024 L/t、只过蒸汽、不耗电、无缓存）；
> 3. **蒸汽管道**（2 个压力等级 × 5 种粗细 = 10 种方块）。
>
> 相关代码：
> [`SusyLangOverride.java`](../src/main/java/com/susy/plusplus/client/SusyLangOverride.java) ·
> [`SteamPumpCover.java`](../src/main/java/com/susy/plusplus/cover/SteamPumpCover.java) ·
> [`pipe/steam/`](../src/main/java/com/susy/plusplus/pipe/steam)

---

## 1. Susy 翻译开关（`enableSusyTranslations`，默认 `false`）

### 1.1 为什么不能"放个 lang 文件然后默认不生效"

Minecraft 会**无条件**加载资源包里所有
`assets/<modid>/lang/<locale>.lang`，Forge 没有"按文件开关"的机制。
所以只要文件放在 `lang/` 目录下，它就一定生效 —— 没有配置项能关掉它。

### 1.2 本模组的做法

| 项目 | 值 |
| --- | --- |
| 文件位置 | `assets/susyplusplus/lang_override/susy_zh_cn.lang`（**不在** `lang/` 下） |
| 内容 | 只补 Susy-Core 缺失的中文键（材料 / 机器 / 方块 / metaitem / 配方表 …） |
| 生效条件 | `enableSusyTranslations = true` **且** 游戏语言为中文 |
| 注入时机 | 每次资源包重载后（`IResourceManagerReloadListener`） |
| 注入方式 | 反射拿到 `net.minecraft.client.resources.Language` 内部的 `Map<String,String>`，`putAll` |

要点：

- MC 永远不会把这个文件当语言文件读取，所以**默认状态下它对游戏零影响**；
- 注入是"只加不改"：已有键（例如 Susy-Core 自己翻好的）**不会被覆盖**；
- 反射**不按字段名**取（开发环境叫 `languageList`，生产环境是 `field_74805_a`），
  而是遍历 `Language` 的字段、取类型可赋值给 `Map` 的那一个，因此开发环境与线上包通用；
- 反射失败只打一条 `WARN`，绝不崩溃；
- 仅客户端有效（`@SideOnly(CLIENT)` + 只在 `SuClientEvents#init()` 里注册）。

> 想改这份翻译：直接编辑 `assets/susyplusplus/lang_override/susy_zh_cn.lang`
> （UTF-8，`键=值`，`#` 开头为注释），然后 `F3+T` 重载资源包即可。

---

## 2. 蒸汽泵覆盖板（`enableSteamPump`，默认 `true`）

### 2.1 物品与安装

| 项目 | 值 |
| --- | --- |
| 物品 | `susyplusplus:steam_pump`（`metaitem.steam_pump.name`） |
| 贴图 | 直接复用 GT 的 `gregtech:items/metaitems/electric.pump.lv` |
| 安装 | 手持右键**机器或管道**的某个面（与 GT 泵 / 传送带完全相同的 `CoverItemBehavior`） |
| 拆卸 | 用扳手右键该面 |

注册方式与 GT 自己的覆盖板一致：

```java
CoverBehaviors.registerBehavior(STEAM_PUMP_COVER_ID, STEAM_PUMP,
        (definition, coverableView, side) -> new SteamPumpCover(definition, coverableView, side));
```

### 2.2 行为

| 属性 | 值 | 说明 |
| --- | --- | --- |
| 速率 | **1024 L/t（= 20480 L/s）** | 每 tick 上限真的被钳在 1024（见 §2.3） |
| 过滤 | **只过蒸汽** | 白名单：`gregtech:steam`、`susy:dense_steam`（注册名精确匹配） |
| EU | **不消耗** | 继承 `CoverPump`，它本身不耗电 |
| 缓存 | **无** | 与 GT 电动泵一致，只有"本秒剩余额度" |
| 模式 | 导入 / 导出 | 沿用 GT 泵 UI 的 `PumpMode` |

### 2.3 为什么还要重写 `doTransferFluids(int)`

`javap -c` 核实 GTCEu **2.8.7** 的 `CoverPump#update()`：

```text
if (isWorkingAllowed && fluidLeftToTransferLastSecond > 0)
    fluidLeftToTransferLastSecond -= doTransferFluids(fluidLeftToTransferLastSecond);
if (getOffsetTimer() % 20 == 0)
    fluidLeftToTransferLastSecond = transferRate;
```

即 GT 的"速率"是**每秒额度**，而且**单个 tick 可以一次搬完整秒额度**
（会瞬间搬 20480 L 然后停 19 tick）。因此本类把构造参数设为 `20480`
（`MB_PER_SECOND`），并额外重写：

```java
@Override
protected int doTransferFluids(int transferLimit) {
    return super.doTransferFluids(Math.min(transferLimit, MB_PER_TICK));
}
```

这样瞬时与平均都严格是 **1024 L/t**。

### 2.4 流体过滤器为什么还在

2.8.7 的 `CoverPump` **没有** `createFilterRow()` 开关（那是 2.8.10 才出现的），
过滤器行无法隐藏。因此本模组让过滤器**叠加**在蒸汽白名单上：

```java
@Override
protected boolean checkInputFluid(FluidStack fluidStack) {
    return SteamFluids.isSteam(fluidStack)
            && getFluidFilterContainer().testFluidStack(fluidStack);
}
```

- 不设过滤器（默认）= 任意蒸汽；
- 设了过滤器 = 在蒸汽范围内再限定（例如只要 `dense_steam`）；
- 过滤器**无法**让非蒸汽通过（白名单是硬性前置条件）。

### 2.5 配方

组装机，电路 **17**：

```
电路 17 + 1× 电动泵(LV) + 4× 钢板 + 1× 红石  →  1× 蒸汽泵
200 ticks, 30 EU/t (LV)
```

---

## 3. 蒸汽管道（10 种）

### 3.1 速率规则

用户口径：**基础速率取微型管道，规格倍率与 GT 流体管道一致**
（GT `FluidPipeType`：tiny ×1 / small ×2 / normal ×6 / large ×12 / huge ×24）。

```
速率(L/t) = 压力基础速率 × 规格倍率
低压基础速率 = 64 L/t（微型）
高压基础速率 = 256 L/t（微型）
```

| 规格 | 粗细 (thickness) | 倍率 | 低压 L/t | 高压 L/t |
| --- | --- | --- | --- | --- |
| 微型 tiny | 0.25 | ×1 | **64** | **256** |
| 小型 small | 0.375 | ×2 | 128 | 512 |
| 中型 normal | 0.5 | ×6 | 384 | 1536 |
| 大型 large | 0.75 | ×12 | 768 | 3072 |
| 巨型 huge | 0.875 | ×24 | 1536 | 6144 |

> 改数值只需动 [`SteamPipePressure`](../src/main/java/com/susy/plusplus/pipe/steam/SteamPipePressure.java)
> 里的两个基础速率，或 [`SteamPipeSize`](../src/main/java/com/susy/plusplus/pipe/steam/SteamPipeSize.java)
> 里的倍率/粗细 —— 方块、缓冲罐容量、配方、语言键全部由这两处推导。

### 3.2 方块 / 注册名

低压：

- `susyplusplus:low_pressure_steam_pipe_tiny`
- `susyplusplus:low_pressure_steam_pipe_small`
- `susyplusplus:low_pressure_steam_pipe_normal`
- `susyplusplus:low_pressure_steam_pipe_large`
- `susyplusplus:low_pressure_steam_pipe_huge`

高压：同名把 `low_pressure` 换成 `high_pressure`。

- 全部注册进 GT 的**管道创造模式标签**（`GregTechAPI.TAB_GREGTECH_PIPES`）；
- 物品用本模组的 `ItemBlockSteamPipe`（继承 GT 的 `ItemBlockPipe`：放置时才能带上
  `PipeType`，且 `PipeRenderer` 的物品渲染只认 `ItemBlockPipe`），并额外提供 tooltip：
  - `传输速率：<N> L/t`
  - `压力等级：低压 / 高压`
  - `仅可通过蒸汽`
- 手持**任意管道物品**对准管道时，GT 的碰撞箱会拆成每面 **3×3 九宫格**
  （用于点击阻断某一格连接）——这依赖 `BlockSteamPipe#isHoldingPipe` 返回
  "手持的是不是 `ItemBlockPipe`"，与 GT 原版行为一致。

### 3.3 连接规则

| 情况 | 是否连接 |
| --- | --- |
| 同压力、同/不同粗细 | ✅ 连接（整条路径由**最细**的那根限速） |
| 低压 ↔ 高压 | ❌ 不连接 |
| 管道 ↔ 任意带 `IFluidHandler` 的方块（机器 / 储罐） | ✅ 连接 |
| 管道 ↔ **GT 流体管道** | ✅ 连接（双方都能向对方 `fill`） |
| 管道 ↔ 空气 | ❌ |

连接判定走 GT `BlockPipe` 的标准流程
（`canPipesConnect` / `canPipeConnectToBlock`），因此扳手遮挡面、覆盖板、
碰撞箱、CTM 外墙等行为与 GT 管道一致。

### 3.4 流体搬运模型（与 GT 流体管道一致）

**管道本身不存流体**，走的是与 GT 相同的"即时整网分发"：

- 有人往管道 `fill` 时，立刻从这根管道开始 **BFS 走完整张同压力管网**，
  把流体直接填进"网络末端"的 `IFluidHandler`（机器 / 储罐 / **GT 流体管道**）；
- 有人从管道 `drain` 时，同样遍历网络，从末端处理器里抽流体
  （供蒸汽泵覆盖板的导出模式使用）；
- 速率限制 = 网络里**最细那根**管道的速率（与 GT "最细处限速"一致）；
- 会跳过"把流体推给它的那个面"，避免立刻原路推回；
- 只接受蒸汽（`SteamFluids` 白名单），其它流体会被拒绝。

由此：

- **没有逐段延迟、没有管道残留**，手感与 GT 流体管道一致；
- 多个输出分支会按顺序依次填充；
- 蒸汽泵覆盖板可以直接装在管道上（覆盖板读到的就是这根管道的 `IFluidHandler`）；
- 与 **GT 流体管道可以直接相连并互相传输**：本模组通过
  `canPipeConnectToBlock` 的"邻居有流体能力即连接"规则与之相连，
  流体进入 GT 管道后由 GT 自己的网络分发；GT 管道在它自己的 tick 里
  也会 `fill` 进本模组的管道。

管道实体仍然实现 `ITickable` —— 但**不是**为了传输，而是因为 GT 的管道
是在 `TileEntityFluidPipeTickable#update()` 里调用
`getCoverableImplementation().update()` 来 tick 覆盖板的（javap 核实），
本类照抄这一点。

> 早期版本用的是"每根管道自带缓冲罐、每 tick 前推一格"，会出现
> +1 tick/段 的延迟和管道残留，已按上面的模型重写。

### 3.5 渲染

- 方块：`getRenderType()` 返回自定义 CCL 渲染类型（[`SteamPipeRenderer`](../src/main/java/com/susy/plusplus/client/renderer/SteamPipeRenderer.java)）；
- 贴图：**完全复用 GT 的管道贴图**，不新增任何 PNG
  - 端面：`Textures.PIPE_TINY / SMALL / NORMAL / LARGE / HUGE`（按粗细）
  - 侧面：`Textures.PIPE_SIDE`
  - 颜色：低压橙色 `0xFF8C1A`、高压黑色 `0x1A1A1A`（仅染色，不换贴图；高压用近黑以保留贴图明暗）
- 为什么不能直接用 `FluidPipeRenderer`：它内部是
  `EnumMap<FluidPipeType, TextureAtlasSprite>` 且要求 `Material != null`，
  对非材质的 10 种规格会直接返回（什么都不画）；
- 物品模型：`SuClientEvents#onModelRegistry` 把 10 个物品的 mesh 都指向
  `SteamPipeRenderer.INSTANCE.getModelLocation()`（`PipeRenderer#onModelsBake`
  会把渲染器自己塞进模型注册表）；
- `blockstates/*.json` 是**占位**（`cube_all`），与 GT 管道完全相同的做法 ——
  方块实际由 CCL 渲染器绘制。

> ⚠ 编译期依赖：GT 的 `BlockPipe` 实现了 `gregtech.integration.ctm.IFacadeWrapper`，
> 而它 extends `team.chisel.ctm.api.IFacade`。GT 发布 jar 不含这个可选依赖的 class，
> 所以 `dependencies.gradle` 里加了
> `compileOnly fileTree(dir: 'run/mods', include: ['CTM-*.jar'])`（仅编译期）。
> 运行期由整合包里的 CTM 提供。

### 3.6 配方（合金炉；材料 + 模头）

- **低压 → 粗青铜合金**（GT 材料 `Potin`），**高压 → 钢**；
- 机器：**合金炉**（`ALLOY_SMELTER_RECIPES`）。GT 的 **蒸汽合金炉
  （`SteamAlloySmelter`，jar 内已核实存在）** 用的就是这张表 → 蒸汽时代即可制造；
- 粗细：**用不同模头区分**（`notConsumable`，不消耗），见下表。

成本对齐 GT 自身流体管道（`pipeTinyFluid = M/2` … `pipeHugeFluid = 12M`，M = 1 锭）：

| 规格 | 锭 | 产出 | 模头（`gregtech:meta_item_1`） |
| --- | --- | --- | --- |
| 微型 tiny | 1 | **2 根** | meta **38** |
| 小型 small | 1 | 1 根 | meta **39** |
| 中型 normal | 3 | 1 根 | meta **40** |
| 大型 large | 6 | 1 根 | meta **41** |
| 巨型 huge | 12 | 1 根 | meta **42** |

时长随规格递增（100 / 150 / 200 / 250 / 300 ticks）。

> 这 5 个 meta 就是 GT 的**管道模头**，代码里用
> `modItem("gregtech", "meta_item_1", 1, 38 + sizeIndex)` 直接取
> （见 `SuRecipes#pipeMold(int)`），取不到时配方自动跳过。
>
> **为什么不用虚拟电路？** 蒸汽合金炉没有电路槽。
> **为什么不用"不同锭数"区分？** GT 的配方匹配允许输入数量大于配方量，
> 同材料不同数量的两条配方会互相覆盖。`notConsumable` 模头参与匹配但不消耗，
> 天然区分五种粗细，也不和 GT 现有配方冲突。
>
> 早期版本经历"1 板 → 8 根（太便宜）"→"组装机无电路（与 GT 配方抢匹配）"→
> "锻压机 + 5 种通用模具"→ 最终上表的"合金炉 + GT 管道模头"。

### 3.7 语言键

```
tile.susyplusplus.low_pressure_steam_pipe_<tiny|small|normal|large|huge>.name
tile.susyplusplus.high_pressure_steam_pipe_<tiny|small|normal|large|huge>.name
```

中文名形如「低压微型蒸汽管道」「高压巨型蒸汽管道」。

---

## 4. 游戏内验证清单

1. **翻译开关默认关闭**：默认配置下启动，Susy 的机器/材料名应是英文（或 Susy-Core 自带的翻译）；
   把 `enableSusyTranslations` 改成 `true` 重启，`F3+T` 后应变成中文补充内容；
2. **蒸汽泵**：JEI 组装机里找电路 17 → 做出 `susyplusplus:steam_pump`；
   右键机器/管道安装；管道里通蒸汽时每 tick 传输量应不超过 1024 L；
   用水/其它流体测试应被拒绝；
3. **蒸汽管道**：创造模式管道标签里应有 10 个蒸汽管道；
   低压与高压相邻时**不相连**；同一压力下不同粗细相连且由最细者限速；
   管道能装覆盖板、能用扳手遮挡面；
4. **速率**：低压巨型应为 1536 L/t、高压巨型 6144 L/t（用储罐读数或 TOP/WAILA 观察）。

---

## 5. 已知限制

- 低压力管网与高压力管网**永不互连**，需要玩家自己做压力转换；
- 网络吞吐由**最细那根**管道决定（与 GT 相同）；
- 管道方块**不受 config 控制**（注册早于配置注入，见
  [`SuSteamPipes`](../src/main/java/com/susy/plusplus/pipe/steam/SuSteamPipes.java) 的说明）；
- GT 的**未激活**流体管道（只有一个连接、不 tick 的那种）没有暴露 `IFluidHandler`，
  这时本模组的管道无法与之互传 —— 给它再接一段即可激活。

---

## 6. 已修复的坑（供后续维护参考）

### 6.1 打开配置器 UI / 看向管道时崩溃：`NoSuchMethodError`

崩溃栈：

```text
java.lang.NoSuchMethodError: 'net.minecraft.item.ItemStack
    net.minecraft.entity.player.EntityPlayer.getHeldItemMainhand()'
```

原因不在代码逻辑，而在**本项目的重混淆工具链**：
`getHeldItem*` 声明在 `EntityLivingBase`、`isSneaking` 声明在 `Entity`，
而 RFG reobf **不沿继承链查映射**。接收者静态类型写成 `EntityPlayer` 时，
调用名会被原样保留为 MCP 名，运行期就找不到方法。

**修复**：所有这类调用统一走
[`PlayerHeld`](../src/main/java/com/susy/plusplus/util/PlayerHeld.java)
（内部把接收者转成 `EntityLivingBase` / `Entity` 再调用），
覆盖：配置器（快捷键 / 右键 / GUI 取物品）、手推车、防水喷漆、电池盒、红石阀门的潜行判断、蒸汽管道。

已核验：打完包后 `run/mods/susyplusplus.jar` 里不再存在
`getHeldItem*` / `isSneaking` 这些未映射名字（全部变成 `func_184614_ca` /
`func_184592_cb` / `func_184586_b` / `func_70093_af`）。

### 6.2 `TileEntitySteamPipe is missing a mapping!`（一放管道/一开界面就崩）

```text
Description: Exception ticking world
java.lang.RuntimeException: class com.susy.plusplus.pipe.steam.TileEntitySteamPipe is missing a mapping!
    at net.minecraft.tileentity.TileEntity.writeInternal(TileEntity.java:78)
    at net.minecraft.tileentity.TileEntity.getUpdateTag(TileEntity.java:205)
    at ... SPacketChunkData.<init>
```

原因：**方块实体类没有注册**。Minecraft 写 NBT / 发区块包时要用
"类 → 注册名"反查映射，GT 的 `BlockPipe` 又覆写了 `getUpdateTag`，
所以只要附近有我们的管道、并且触发一次区块同步（打开任意界面/传送/重进世界都可能），
服务端就会在 tick 里抛异常。

修复：`SusyPlusPlus#preInit` 里

```java
GameRegistry.registerTileEntity(TileEntitySteamPipe.class,
        new ResourceLocation(Tags.MOD_ID, "steam_pipe"));
```

> 这也解释了"配置器打开 UI 还是会崩"：配置器界面本身没问题，
> 是打开界面时服务端/客户端同步区块，碰到未注册的管道方块实体而后台崩掉。

### 6.3 手持扳手对准管道没有九宫格

`BlockPipe#hasPipeCollisionChangingItem` → `isHoldingPipe(player)`，
本模组最初只认"本方块自己的物品"，导致手持扳手/其它管道时没有 3×3 碰撞箱网格。
现已改成与 GT 一致：`stack.getItem() instanceof ItemBlockPipe`。

### 6.4 管道"没有连接和传输"

两个独立原因，都已修：

1. 上面 6.1 的崩溃发生在 `BlockSteamPipe#isHoldingPipe`，
   而它被 GT 的 `getCollisionBox` 调用 —— 也就是说**看着管道就会崩**，
   根本轮不到连接/传输；
2. 第一版管道要求"已连接"才暴露 `IFluidHandler`，
   但 GT 的 `canPipeConnectToBlock` 正是靠"邻居有没有这个能力"来决定是否连接
   （鸡生蛋）。现在**每个面无条件暴露**能力，连接与互传都恢复正常。
