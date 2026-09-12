# 强化土高炉 / Reinforced PBF

> 一个"不耗电、无需维护、外壳可插仓室"的土高炉变体。

---

## 1. 功能说明

| 项目 | 值 |
| --- | --- |
| 控制器注册名 | `susyplusplus:reinforced_pbf` |
| 强化耐火砖注册名 | `susyplusplus:reinforced_firebrick` |
| 使用的 RecipeMap | `RecipeMaps.PRIMITIVE_BLAST_FURNACE_RECIPES`（GT 原版土高炉，**未新增任何配方**） |
| 耗电 | **不耗电**（无能量仓） |
| 维护 | **无需维护**（无维护仓、无维护问题） |
| 并行 | **4 并行**（`recipeMapWorkable.setParallelLimit(4)`） |
| TOP 能耗行 | **隐藏**（`getInfoProviderEUt()` 返回 0） |
| JEI/REI 预览 | **自动**（无需自写 JEI 插件） |

它复用原版土高炉的配方（铁/熟铁 + 煤/木炭/焦炭 → 钢），因此**不需要新增任何配方**，
JEI 里会直接沿用 GT 已有的那批土高炉配方。

---

## 2. 多方块结构图

三层，每层 3(X) × 4(Z)；`S` = 控制器，`R` = 强化耐火砖**或**仓室，`#` = 空气。

```
        第 1 层 (y=0)        第 2 层 (y=1)        第 3 层 (y=2)

  z=0     R  R  R              R  R  R              R  R  R
  z=1     R  R  R              R  #  R              R  S  R      <-- 控制器
  z=2     R  R  R              R  #  R              R  R  R
  z=3     R  R  R              R  #  R              R  R  R
```

对应代码（`MetaTileEntityReinforcedPBF#createStructurePattern`）：

```java
return FactoryBlockPattern.start()
        .aisle("RRR", "RRR", "RRR", "RRR")
        .aisle("RRR", "R#R", "R#R", "R#R")
        .aisle("RRR", "RSR", "RRR", "RRR")
        .where('R', casingOrHatchPredicate())
        .where('#', air())
        .where('S', selfPredicate())
        .build();
```

> `aisle` 的每个字符串是**一行（沿 X）**，字符串顺序**沿 Z**，多次 `aisle()` **沿 Y**。
> 控制器位于**顶层、X 中间、Z 第 2 格**。
> 与 GT 原版土高炉的结构完全同形，只是把原版的 `&`（可被雪占据的格）去掉了，
> 并因此**没有**实现原版的"破雪 + 岩浆伤害"逻辑（该逻辑绑定在 `&` 那一格上）。

---

## 3. 为什么不直接用 GT 的 primitive（土高炉）框架

GT 的 primitive 多方块（`RecipeMapPrimitiveMultiblockController`，土高炉的基类）**天然不支持仓室**：

```java
// RecipeMapPrimitiveMultiblockController
protected void initializeAbilities() {
    this.importItems = new NotifiableItemStackHandler(this, recipeMap.getMaxInputs(), this, false);  // 内置 3 格
    this.exportItems = new NotifiableItemStackHandler(this, recipeMap.getMaxOutputs(), this, true);  // 内置 3 格
}
@Override
public <T> T getCapability(Capability<T> capability, EnumFacing side) {
    if ((capability == ITEM_HANDLER || capability == FLUID_HANDLER) && side != null) return null;  // 侧面不暴露！
}
```

而原版土高炉的结构谓词也**只有** `states(PRIMITIVE_BRICKS)`，没有任何 `abilities(...)`。

但需求是「**R 可替换为仓室**」+「**不耗电 + 无需维护**」，两者在 primitive 框架下互斥。
因此本实现改为继承**带电框架** `RecipeMapMultiblockController`（才有 `abilities(...)` 仓室支持），
再用**能量伪装**把"耗电"消掉 —— 这与 GT 自己 `PrimitiveRecipeLogic` 的做法完全一致。

---

## 4. 不耗电是怎么做的

见 `ReinforcedPbfRecipeLogic`（继承 `MultiblockRecipeLogic`，并替换掉默认 logic）：

```java
getEnergyInputPerSecond() → Integer.MAX_VALUE
getEnergyStored()         → Integer.MAX_VALUE
getEnergyCapacity()       → Integer.MAX_VALUE
drawEnergy(..)            → true     // 假装电量已扣
getMaxVoltage()           → GTValues.LV
getMaximumOverclockVoltage() → GTValues.V[LV]
```

- **必须覆写 `getMaxVoltage()`**：父类读的是能量仓的输入电压；本多方块没有能量仓
  （能力列表为空，`EnergyContainerList` 为空），其值会是 `0`，
  任何配方都会因 "tier 不足" 而无法匹配。
- **不需要能量仓**，结构谓词里也**不允许**能量仓。

### 替换 logic 是否安全？

安全。这是 GT 自己的惯用写法，源码里有 17 处（例：
`MetaTileEntityElectricBlastFurnace` → `new HeatingCoilRecipeLogic(this)`、`SteamMultiWorkable` 等）。
原因是 `MetaTileEntity.addMetaTileEntityTrait` 用的是**按名字索引的 Map**：

```java
void addMetaTileEntityTrait(MTETrait trait) {
    this.mteTraits.put(trait.getName(), trait);   // 同名直接覆盖，不会残留旧 logic
}
```

---

## 5. 无需维护是怎么做的

1. 控制器覆写：
   ```java
   @Override public boolean hasMaintenanceMechanics() { return false; }
   ```
2. `ReinforcedPbfRecipeLogic` 再覆写 `getMaintenanceValues()` 返回 `(0, 1.0)` 双保险：
   ```java
   @Override protected Tuple<Integer, Double> getMaintenanceValues() { return new Tuple<>(0, 1.0D); }
   ```
   （该方法同时决定"维护问题导致的时长惩罚"，置 0 即无惩罚。）

结构谓词里**不允许** `MAINTENANCE_HATCH`。

---

## 6. 仓室替换规则

`R` 位置可以是「强化耐火砖」**或**以下仓室：

| 允许的仓室能力 | 上限 | 说明 |
| --- | --- | --- |
| `MultiblockAbility.IMPORT_ITEMS` | 2 | 对应土高炉 RecipeMap 的 3 物品输入 |
| `MultiblockAbility.EXPORT_ITEMS` | 2 | 对应 3 物品输出 |
| `MultiblockAbility.IMPORT_FLUIDS` | 1 | 原版土高炉配方无流体，预留 |
| `MultiblockAbility.EXPORT_FLUIDS` | 1 | 同上 |

**刻意不允许**：

- `INPUT_ENERGY` / `SUBSTATION_INPUT_ENERGY` / `INPUT_LASER` —— 本机不耗电
- `MAINTENANCE_HATCH` —— 本机无需维护
- `MUFFLER_HATCH` —— 未启用消声器机制

代码：

```java
private static TraceabilityPredicate casingOrHatchPredicate() {
    return states(SuBlocks.REINFORCED_FIREBRICK.getDefaultState())
            .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
            .or(abilities(MultiblockAbility.EXPORT_ITEMS).setMaxGlobalLimited(2))
            .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1))
            .or(abilities(MultiblockAbility.EXPORT_FLUIDS).setMaxGlobalLimited(1));
}
```

> 如果希望"允许更多/更少仓室"，改 `setMaxGlobalLimited(n)` 即可；
> 若想放开能量仓，直接加一行 `.or(abilities(MultiblockAbility.INPUT_ENERGY)...)`
> —— 但那样就和"不耗电"的设定不一致了。

---

## 7. JEI / REI 预览

**无需任何额外代码**。注册走的是 GT 自己的入口：

```java
MetaTileEntities.registerMetaTileEntity(int id, T mte)
```

其内部会做（源码 `MetaTileEntities.java`）：

```java
if (Mods.JustEnoughItems.isModLoaded()
        && mte instanceof MultiblockControllerBase controller
        && controller.shouldShowInJei()) {
    MultiblockInfoCategory.registerMultiblock(controller);
}
mte.getRegistry().register(id, mte.metaTileEntityId, mte);
```

所以只要通过它注册控制器，**JEI 的多方块结构预览就会自动生成**。

### MTE 数字 ID

`MetaTileEntities.registerMetaTileEntity(int, T)` 的 id 用于物品元数据，必须全局唯一。
已核实的占用：GT 本体用低位；Susy-Core 用 `14500–18527 / 19000–20002 / 32000`。
本模组取 **32100**（见 `SuMetaTileEntities#ID_REINFORCED_PBF`）。若冲突，改这一个常量即可。

---

## 8. 本地化键

本项目使用 Forge 1.12.2 的 **`.lang`**（不是 `.json`）：
`assets/susyplusplus/lang/en_us.lang` / `zh_cn.lang`。

| 键 | en_us | zh_cn |
| --- | --- | --- |
| `tile.reinforced_firebrick.name` | Reinforced Firebrick | 强化耐火砖 |
| `susyplusplus.machine.reinforced_pbf.name` | Reinforced Blast Furnace | 强化土高炉 |
| `susyplusplus.machine.reinforced_pbf.description` | （JEI 预览描述） | （JEI 预览描述） |

> 控制器的键格式由 GT 决定：
> `MetaTileEntity#getMetaName()` = `String.format("%s.machine.%s", metaTileEntityId.getNamespace(), metaTileEntityId.getPath())`
> → `susyplusplus.machine.reinforced_pbf`，完整键再加 `.name`。
> （对照 GT 自己：`gregtech.machine.primitive_blast_furnace.bronze.name`）

---

## 9. 材质路径

### 🔴 血泪教训：纹理路径必须「全小写」+ 目录名是「blocks」（复数）

1. `SimpleOverlayRenderer(String basePath)` 内部固定拼成
   `new ResourceLocation(modid, "blocks/" + basePath)`
   → 只认 **`textures/blocks/`（复数）**。
2. `ResourceLocation` 的构造函数会把 path **强制 `toLowerCase(Locale.ROOT)`**
   （反编译证据 `ResourceLocation.java` 第 22 行）：
   ```java
   this.path = resourceName[1].toLowerCase(Locale.ROOT);
   ```

所以最初的 `textures/block/ReinforcedPBF/`（**单数 + 大写 PBF**）**永远找不到**，
方块就渲染成黑紫格。现已复制到规范路径：

| 路径 | 状态 |
| --- | --- |
| `assets/susyplusplus/textures/blocks/reinforcedpbf/reinforced_bricks.png` | ✅ **实际生效** |
| `assets/susyplusplus/textures/block/ReinforcedPBF/reinforced_bricks.png` | ⚠️ 旧路径，已失效（可删） |

### 强化耐火砖方块

| 用途 | 路径 |
| --- | --- |
| 纹理 | `assets/susyplusplus/textures/blocks/reinforcedpbf/reinforced_bricks.png` |
| blockstate | `assets/susyplusplus/blockstates/reinforced_firebrick.json` |
| 方块模型 | `assets/susyplusplus/models/block/reinforced_firebrick.json` → `"all": "susyplusplus:blocks/reinforcedpbf/reinforced_bricks"` |
| 物品模型 | `assets/susyplusplus/models/item/reinforced_firebrick.json` |

### 控制器：本体用你的纹理，覆盖层用 GT 土高炉的表盘

```java
// MetaTileEntityReinforcedPBF（两个方法都是 @SideOnly(Side.CLIENT)）
getBaseTexture(...) → SuTextures.REINFORCED_BRICKS               // 你的 reinforced_bricks.png
getFrontOverlay()   → Textures.PRIMITIVE_BLAST_FURNACE_OVERLAY   // GT 原版土高炉表盘

// SuTextures（@SideOnly(Side.CLIENT)）
new SimpleOverlayRenderer("susyplusplus:reinforcedpbf/reinforced_bricks")
```

> 这样机身是强化耐火砖纹理，正面保留 GT 土高炉的"运行中/暂停"表盘，
> 且因为是两张不同的图，**不存在 z-fighting**。

### 多方块结构预览材质

JEI 预览是**按结构里每个方块的 IBlockState 实时渲染**的，**不需要单独的预览贴图**。
所以只要"强化耐火砖"的方块纹理正确，预览里 R 的显示就是正确的。

---

## 9.4 物品提示（tooltip）

覆写 `MetaTileEntity#addInformation(ItemStack, World, List<String>, boolean)` 添加（先调 `super` 保留 GT 自己的行）：

```java
@Override
public void addInformation(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
    super.addInformation(stack, world, tooltip, advanced);
    tooltip.add(TextFormatting.GOLD + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.recipe"));
    tooltip.add(TextFormatting.GREEN + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.no_energy"));
    tooltip.add(TextFormatting.GREEN + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.no_maintenance"));
    tooltip.add(TextFormatting.AQUA + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.parallel"));
    tooltip.add(TextFormatting.GRAY + I18n.format("susyplusplus.machine.reinforced_pbf.tooltip.hatch"));
}
```

| 本地化键 | en_us | zh_cn |
| --- | --- | --- |
| `...tooltip.recipe` | Uses the Primitive Blast Furnace recipes | 使用原版土高炉的配方 |
| `...tooltip.no_energy` | No energy required | 不消耗电力 |
| `...tooltip.no_maintenance` | No maintenance required | 无需维护 |
| `...tooltip.parallel` | Processes 4 recipes in parallel | 4 并行处理 |
| `...tooltip.hatch` | Casing can be replaced by item/fluid hatches | 外壳可替换为物品/流体仓室 |

---

## 9.5 工作台合成配方

用 GT 的 `ModHandler.addShapedRecipe(name, output, ...)` + `UnificationEntry` 注册（与 GT 自己
`BatteryRecipes` 的写法一致）：

```
  S P S        S = 钢螺丝   (OrePrefix.screw, Materials.Steel)
  P B P        P = 钢板     (OrePrefix.plate, Materials.Steel)
  S P S        B = 见下表
```

| 产物 | 中心 B |
| --- | --- |
| 强化土高炉 `susyplusplus:reinforced_pbf` | 原版土高炉（`MetaTileEntities.PRIMITIVE_BLAST_FURNACE.getStackForm()`） |
| 强化耐火砖 `susyplusplus:reinforced_firebrick` | 原版耐火砖（`MetaBlocks.METAL_CASING.getItemVariant(MetalCasingType.PRIMITIVE_BRICKS)`） |

配方名：`susyplusplus_reinforced_pbf` / `susyplusplus_reinforced_firebrick`（全局唯一）。

> 螺丝材料你未指定，默认取**钢**（与钢板一致）。要改的话只改
> `SuRecipes#registerReinforcedPbfCraftingRecipes` 里的 `OrePrefix.screw` 材料即可。
> 形状若要改（比如竖排），直接改那 3 个字符串。

---

## 10. 验证步骤

编译/打包：

```bat
gradlew.bat compileJava
gradlew.bat copyModToRunMods     :: 复制 reobfJar（SRG 名）到 run\mods\susyplusplus.jar
```

> 必须用 `copyModToRunMods`。若把 `jar` 任务（MCP 名）的产物丢进 `run/mods`，
> 运行时会 `NoSuchMethodError`。

游戏内清单：

1. [ ] 编译通过（当前状态：**BUILD SUCCESSFUL**）
2. [ ] 创造模式物品栏能找到「强化耐火砖」与「强化土高炉」
3. [ ] 把控制器放地上 → **JEI 里出现该多方块的结构预览**（说明预览注册成功）
4. [ ] 按 §2 的结构搭建（3 层）→ 多方块**成型**
5. [ ] 放入铁锭 + 煤 → **能正常炼制出钢**（说明用的是原版土高炉 RecipeMap）
6. [ ] **不耗电**：不放任何能量仓也能运行；`TOP`/工具提示中不应要求电力
7. [ ] **无需维护**：不插维护仓，长时间运行也不出现维护问题
8. [ ] 把某个 `R` 换成**物品输入/输出仓** → 仍然成型，且可从仓室进料/出料
9. [ ] 破坏结构 → 方块**正确掉落**（强化耐火砖用扳手或镐均可挖）
10. [ ] 存档退出重进 → 结构**仍能成型**

若第 8 步失败，请把日志（`run/logs/latest.log` 中 `susyplusplus` 相关行）发我，
我再核对 `abilities(...)` 的谓词写法。

---

## 11. 文件清单

新增：

| 文件 | 作用 |
| --- | --- |
| `src/main/java/com/susy/plusplus/block/BlockReinforcedFirebrick.java` | 强化耐火砖方块（属性对齐 GT 原版） |
| `src/main/java/com/susy/plusplus/block/SuBlocks.java` | 方块/ItemBlock 注册（RegistryEvent） |
| `src/main/java/com/susy/plusplus/multiblock/MetaTileEntityReinforcedPBF.java` | 控制器 |
| `src/main/java/com/susy/plusplus/multiblock/ReinforcedPbfRecipeLogic.java` | 不耗电 / 无维护的配方逻辑 |
| `src/main/java/com/susy/plusplus/multiblock/SuMetaTileEntities.java` | MTE 注册入口 |
| `src/main/resources/assets/susyplusplus/blockstates/reinforced_firebrick.json` | 方块状态 |
| `src/main/resources/assets/susyplusplus/models/block/reinforced_firebrick.json` | 方块模型 |
| `src/main/resources/assets/susyplusplus/models/item/reinforced_firebrick.json` | 物品模型 |

修改：

| 文件 | 改动 |
| --- | --- |
| `src/main/java/com/susy/plusplus/SusyPlusPlus.java` | init 阶段调用 `SuMetaTileEntities.init()` |
| `src/main/java/com/susy/plusplus/client/SuClientEvents.java` | 注册 ItemBlock 的物品模型 |
| `assets/susyplusplus/lang/en_us.lang` / `zh_cn.lang` | 新增本地化键 |

**未修改**：`somemods` 下的任何源码（GregTech / Susy-Core / Pyrotech 等）。
