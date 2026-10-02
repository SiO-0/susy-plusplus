# 电动碾磨机（Electric Grinder）

> **LV 电动多方块研磨机**：直接复用 GT 的研磨机配方表，固定 **32 并行**。
> 填补「蒸汽研磨机 → 偏心破碎机」之间的空档，且**不需要锰钢**。

| 项 | 值 |
| --- | --- |
| 注册名 | `susyplusplus:electric_grinder`（MTE 数字 ID **32103**） |
| 名字键 | `susyplusplus.machine.electric_grinder.name`（`MetaTileEntity#getMetaName()` = `<命名空间>.machine.<路径>`） |
| 类型 | **LV 电动多方块**（`extends RecipeMapMultiblockController`） |
| 配方表 | **没有自己的表**：直接复用 `RecipeMaps.MACERATOR_RECIPES` |
| 并行 | **32**（常量 `MetaTileEntityElectricGrinder.PARALLEL_LIMIT`，**不做成配置项**） |
| 维护 | **不需要**（结构不接受维护仓） |
| 结构 | 3(X)×3(Y)×3(Z)：钢制机械外壳 + 四根钢框架角柱 |
| 获取 | 组装机（`ASSEMBLER_RECIPES`，30 EU/t / 600 ticks） |
| 配置开关 | `enableElectricGrinder`（默认 `true`） |
| 材质 | **无需新增**：机身 `Textures.SOLID_STEEL_CASING`，正面 `Textures.MACERATOR_OVERLAY` |

---

## 1. 定位与设计

### 1.1 它填的空档

SUSY 的早期矿处链是：手工（研钵 / 磨石）→ **蒸汽研磨机** → **（空档）** → **偏心破碎机**。
偏心破碎机的组装机配方需要 `plateDoubleManganeseSteel`，而锰要 ERF + 碳源熔 `pyrolusite`
（75% 概率）——属于 MV 中后期。于是玩家的实际体验是：**从蒸汽时代拿到 ×2 之后，
直到 MV 中后期矿石处理效率毫无提升。** 本机器就是填这一段的：**拿到钢就能造**。

### 1.2 关键设计：直接复用研磨机的配方表

```java
// MetaTileEntityElectricGrinder 构造器
super(metaTileEntityId, RecipeMaps.MACERATOR_RECIPES);
```

好处是决定性的：

- **产率 / 副产物 / 耗时与研磨机逐字段相同**——本来就是同一张表里的同一条配方对象，
  不存在"克隆时抄错一个字段"的可能；
- 整合包用 GroovyScript 对研磨机做的**任何**增删改（铝土矿那种 ×4 覆盖、矿床方块的
  手工配方……）本机器**自动跟随**；
- **不存在时序问题**（见下）；
- JEI 里本机器直接显示**研磨机的配方分类**，不另开分类。

### 1.3 为什么「克隆一份到自己的配方表」行不通（历史记录）

最初的设计是「新建一张 `electric_grinder` 表，把研磨机的矿石配方克隆进去、时长改成 1/4」。
它踩到两个独立的坑，都已实测确认，**这套代码已全部删除**：

**坑一：时序。** 实测日志时间戳：

| 时刻 | 事件 |
| --- | --- |
| 00:41:28 | 本模组 postInit ← GroovyScript 还什么都没加 |
| 00:41:50 | 所有 mod 的 postInit 结束 |
| 00:41:50 | GroovyScript 开始跑 postInit 脚本 |
| **00:43:08** | GroovyScript 跑完 |
| 00:43:08 | `FMLLoadCompleteEvent` 开始派发：`groovyscript → jei → … → gregtech → susyplusplus` |
| 00:43:47 | 本模组收到 `FMLLoadCompleteEvent` |

- **postInit 太早**：整合包对研磨机的增删改还没发生（日志实测
  `macerator overrides 0/0, eccentric-roll-crusher manual 0/0`，即铝土矿 ×4 与矿床方块配方全丢）；
- **`FMLLoadCompleteEvent` 太晚**：JEI 的插件注册发生在这个事件派发的**前半段**
  （本模组排在 `gregtech` 之后），那时 JEI 已经把配方抄进索引，之后再加的不会显示。

**坑二：** 想用「继承 `RecipeMap`、覆写 `getRecipesByCategory()` 做惰性生成」绕开时序，
**在本工程根本无法编译**：GT 的 `RecipeMap` 类上带着 `@ZenClass` / `@ZenRegister`（`javap -v` 可证），
而编译期类路径里没有 CraftTweaker / ZenScript，`extends RecipeMap` 会让 javac 在解析父类注解时
直接报 `无法访问 craftteaker.annotations.ZenRegister` 之类的错误。
（只是把 `RecipeMap` 当类型用只会产生警告，所以这个坑很隐蔽。）

**结论：不克隆，直接用同一张表。**

---

## 2. 吞吐来自并行

时长沿用研磨机自身（矿石配方 400 ticks），吞吐全靠并行：

```java
this.recipeMapWorkable.setParallelLimit(PARALLEL_LIMIT);   // = 32，常量，不读配置
```

> ⚠ **为什么不给这个并行数做配置项**：Forge **不会更新已经存在的配置项**。
> 早期的版本写下过 `config/susyplusplus.cfg` 里的 `electricGrinderParallel=16`，
> 之后即使把代码默认值改成 32，那个旧值依然生效——表现为「改了半天还是 16 并行」。
> 所以现在直接写成常量 `PARALLEL_LIMIT`；要改就改这个常量。
> （旧的 `electricGrinderParallel` 键已经不在代码里，下次启动时 Forge 重写配置就会把它去掉。）

GT 的并行逻辑会再按**输入量、输出空间、电力**限制实际并行数，所以并行开大
**不会丢物品**，只会自动降并行（电力不足或输出堵塞时）。

---

## 3. 结构

**3(X) × 3(Y) × 3(Z)** 的钢外壳，中间一格是空腔，四根竖直角柱用钢框架加强。

```
俯视（y=1 层）        正视（沿 Z 看）
 F C F                  y=2:  F C F
 C # C                  y=1:  C # C     ← 正面中央就是控制器
 F C F                  y=0:  F C F
```

| 符号 | 方块 | 说明 |
| --- | --- | --- |
| `F` | Steel Frame Box | 4 根竖直角柱 |
| `C` | Solid Steel Machine Casing | 外壳，**可被仓室替换** |
| `S` | 控制器 | 位于某一面的正中 |
| `#` | 空气 | 内部空腔 |

- 允许的仓室（显式 `abilities(...)`，与 `MetaTileEntityReinforcedPBF` 同一写法）：
  **能源仓** 1~3 个（至少 1 个）、**物品输入总线**最多 2 个、**物品输出总线**最多 2 个；
- **不允许**维护仓与流体仓：不把它们写进谓词即可，且 `hasMaintenanceMechanics()` 返回 `false`。

### ⚠ 为什么结构必须「四面看起来一样」

GT 的 JEI 结构预览是按**固定朝向**把这个 pattern 画出来的，而玩家实际摆放控制器时，
controller 的 `frontFacing` 取决于放置方向，GT 会把整个 pattern 按该朝向**旋转**后再校验。
如果 pattern 本身前后不对称（例如"前段进料斗 + 后段齿轮箱"那种），
那么「照着 JEI 图摆出来的结构」和「机器认定的正面」就会差 90°/180° ——
表现就是**主面朝向与 JEI 里不一致**。

外壳均匀、控制器居中之后，任何旋转都自洽，JEI 里看到的与摆出来的完全一致。
本模组的 `MetaTileEntityReinforcedPBF` 用的也是这种"外壳均匀"的写法。

### ⚠ 机身谓词**绝对不能**写 `states(...).setMinGlobalLimited(N)`

`TraceabilityPredicate#setMinGlobalLimited(int)` 的实现是
`limited.addAll(common); common.clear(); ...`——它会把这条 `SimplePredicate`
从 `common` **移进 `limited`**。而 GT 的 JEI 结构预览（`BlockPattern#getPreview`）
取方块的顺序是：

1. 先遍历 `limited`，按 `previewCount` / `minGlobalCount` **以「额度」逐格消耗**，
   额度用完就换下一条；
2. 再遍历 `common` 里 `previewCount > 0` 的条目（同样按额度）；
3. **最后才**遍历 `common` 里 `previewCount == -1` 的条目——
   **只有这一档会「不限量」覆盖剩余的全部位置**。

所以一旦写成 `states(getCasingState()).setMinGlobalLimited(10)`，预览就会
**只画 10 格机身 + 每类仓室各 1 格**，剩下十几个 `C` 位置**什么都不画**，
右键那些格子也点不出可替换方块列表——看起来就像「仓室不显示、机身贴图丢失、不用 BaseTexture」。
GT 自己的机器（`MetaTileEntityLargeMiner` / `DistillationTower` / `FusionReactor` /
`LargeTurbine` / `FluidDrill`）的机身谓词都只写 `states(getCasingState())`，不设全局下限。

---

## 4. JEI 里能看到什么

- **配方**：显示的是**研磨机（Macerator）的配方分类**，内容与研磨机完全一致
  （本机器没有自己的配方表）；
- **结构**：GT 的多方块信息页显示上面那个 3×3×3 结构，每个位置都有方块与贴图，
  右键可点出该位置的可替换方块列表，且方向与摆出来的完全一致；
- **机器名**：`susyplusplus.machine.electric_grinder.name`（中英双语已提供）；
- **tooltip**：三行——`32 并行` / `配方与研磨机完全相同` / `无需维护`。

---

## 5. 与其它模组的关系

- 本机器**不修改** GT / SUSY 的任何既有配方（它只是"再读一张别人的表"）；
- 不影响蒸汽研磨机、LV/MV 研磨机、偏心破碎机的任何行为；
- **纯 GT 环境**下也能正常工作：只用 GT 本体的钢制方块与 LV 元件，
  不依赖 SUSY 的任何机器或材料（因此**不受 `vanillaGtCompat` 影响**）。

---

## 6. 配置

| 选项 | 默认 | 说明 |
| --- | --- | --- |
| `enableElectricGrinder` | `true` | 关闭后不注册机器与组装机配方 |

> ⚠ 这是**加载期**开关，改动后需**重启游戏**。

并行数是代码常量（见 §2），不是配置项。

---

## 7. 实现时核实过的 GT API（供维护参考）

工程编译依赖是 **`2.8.7-beta`**、运行时是 **`2.8.10-beta`**，存在版本漂移，
因此下面这些签名都对照过 GTCEu 源码并用 `javap` 反查过运行时
`run/mods/gregtech-1.12.2-2.8.10-beta.jar`，且通过 `gradlew build` 编译验证：

| 用到的 API | 签名 / 说明 |
| --- | --- |
| `RecipeMapMultiblockController` 构造器 | `(ResourceLocation, RecipeMap<?>)`；传 `RecipeMaps.MACERATOR_RECIPES` 即可让机器直接使用研磨机的配方 |
| `MultiblockRecipeLogic#setParallelLimit(int)` | 实际声明在 `AbstractRecipeLogic` 上（`public void setParallelLimit(int)`），配套 `getParallelLimit()` |
| `MultiblockControllerBase.states(IBlockState...)` / `frames(Material...)` / `air()` / `abilities(MultiblockAbility<?>...)` | 均为 **static**，可在 `private static` 辅助方法里调用 |
| `MultiblockControllerBase.selfPredicate()` | 实例方法 |
| `MultiblockWithDisplayBase.hasMaintenanceMechanics()` | `public boolean`，覆写为 `false` 即关闭维护机制 |
| `FactoryBlockPattern.start()` | 等价于 `(RIGHT, UP, BACK)`：字符串内沿 X、同一 aisle 的多个字符串沿 Y、多次 aisle 沿 Z |
| `TraceabilityPredicate#setMinGlobalLimited(int)` | 会把谓词从 `common` 移进 `limited`，因此机身谓词不要加（见 §3） |
| `Textures` | `SOLID_STEEL_CASING`、`MACERATOR_OVERLAY` 均存在 |
| `MetaTileEntities.registerMetaTileEntity(int, T)` | 自动完成仓室能力注册与 JEI 多方块预览注册，**无需自写 JEI 插件** |
| `RecipeMap` 类注解 | 类上带 `@ZenClass` / `@ZenRegister`（`javap -v`），**继承它在本工程会编译失败** |

**建议的检查点：**

1. `build/libs/susyplusplus-*.jar` 能正常产出、无编译错误；
2. JEI 里研磨机的配方分类中能看到本机器作为催化剂；
3. JEI 的多方块结构页：每个位置都有方块与贴图、右键能点出候选列表、方向与摆放一致；
4. 结构搭建：3×3×3，四角放钢框架，外壳位置放 1~3 个能源仓 + 输入/输出总线各不超过 2 个，
   其余填钢制机械外壳，控制器放在某一面中央。
