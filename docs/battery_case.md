# 电池盒（Battery Case）

> Susy Plus Plus 的实用物品之一：把 4 个 GT 电池装进一个"盒子"里随身携带，
> 支持饰品栏（Baubles）、自动为背包供电、以及一个 4 格 UI。

---

## 1. 注册信息

| 项目 | 值 |
| --- | --- |
| 物品注册名 | `susyplusplus:battery_case` |
| 类型 | **MetaItem**（`StandardMetaItem`，非方块） |
| 子物品 | `ItemBatteryCase#batteryCase`（meta 值 `0`，unlocalizedName `battery_case`） |
| 本地化键 | `metaitem.battery_case.name` |
| 最大堆叠 | 1（NBT / Capability 承载数据） |
| 模型路径 | `assets/susyplusplus/models/item/battery_case.json` |
| 贴图路径 | `assets/susyplusplus/textures/item/battery_case.png` |

`ItemBatteryCase#createItemModelPath` 返回 **不含 `item/` 前缀** 的路径
（`susyplusplus:battery_case`），因为 Minecraft 会自动补上 `item/`
——这与 GT 的 `MetaItem` 约定一致，也是之前"紫黑格"贴图问题的根因。

---

## 2. 类结构

```
com.susy.plusplus.item.battery
├── ItemBatteryCase          // MetaItem 本体（模型路径）
├── BatteryCaseBehaviour     // 核心组件：能力提供 + 行为 + UI
├── BatteryCaseInventory     // 4 格库存（ItemStackHandler）
├── BatteryCaseEnergyStorage // IElectricItem 聚合视图（4 格求和）
└── BatteryCaseBaubles       // Baubles 隔离层（仅 Baubles 加载时才触碰其 API）
```

`ItemBatteryCase` 构造时调用：

```java
addItem(0, "battery_case")
        .setMaxStackSize(1)
        .addComponents(new BatteryCaseBehaviour());
```

`BatteryCaseBehaviour` 同时实现 4 个接口，因此一个组件就完成了全部功能：

| 接口 | 作用 |
| --- | --- |
| `IItemComponent` | 组件标记 |
| `IItemCapabilityProvider` | 为每个 ItemStack 提供 `IElectricItem` + `ITEM_HANDLER` 能力 |
| `IItemBehaviour` | 右键 / 每 tick / tooltip |
| `ItemUIFactory` | 物品 GUI |

---

## 3. 能力（Capability）

`BatteryCaseBehaviour.BatteryCaseProvider` 同时暴露两个能力：

| 能力 | 实现 | 说明 |
| --- | --- | --- |
| `GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM` | `BatteryCaseEnergyStorage` | 电池盒对外的"能量视图" |
| `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` | `BatteryCaseInventory` | 4 格电池库存 |

### 数据存在哪里（关键）

盒内 4 格电池保存在 **电池盒物品自身的 NBT tag** 里（键 `BatteryCaseInv`），
**不是** Forge 的 `ForgeCaps`。`BatteryCaseProvider` 刻意**不实现** `INBTSerializable`。

原因来自 Forge 1.12.2 的真实实现
（`build/rfg/minecraft-src/java/net/minecraft/item/Item.java`）：

```java
public NBTTagCompound getNBTShareTag(ItemStack stack) {
    return stack.getTagCompound();   // 只有普通 tag，不含 ForgeCaps！
}
public void readNBTShareTag(ItemStack stack, NBTTagCompound nbt) {
    stack.setTagCompound(nbt);       // 直接覆盖 tag
}
```

而 `gregtech.api.items.metaitem.MetaItem` 并没有覆写 `getNBTShareTag`。
所以**凡是走 share tag 的同步路径，`ForgeCaps` 都会丢**，接收端的 ItemStack 被重建、
能力数据为空 —— 表现就是"**开关电池的释能模式后，UI 里的电池消失了**"。

把库存放进普通 tag 后，它在所有路径下都随物品一起走：

| 路径 | 是否携带数据 |
| --- | --- |
| `Item.getNBTShareTag` / `readNBTShareTag` | ✓（share tag == tagCompound） |
| `ItemStack.writeToNBT` / `ItemStack(NBTTagCompound)` | ✓（`"tag"` 键） |
| `ItemStack.copy()` | ✓（会 `stackTagCompound.copy()`） |

`BatteryCaseInventory` 因此在 `onContentsChanged` 里写回 owner 的 NBT；
又因为内部电池放电是**就地改 NBT**（不会触发 `onContentsChanged`），
`BatteryCaseEnergyStorage` 在 `charge()` / `discharge()` 之后（非 simulate 且确有变化）会显式
`inventory.save()`，保证电量变化落盘。

这也是 GT 自己的做法：`ElectricItem` 的电量同样直接读写 `itemStack.getTagCompound()`
（键 `Charge` / `MaxCharge` / `Infinite`）。

> **注意：**`setInDischargeMode` 在关闭释能模式时**不会**把整个 tag 置 null ——
> 电池库存就在同一个 tag 里，置 null 会把电池一起清掉。

---

## 4. 规则

### 4.1 什么算"电池"

`BatteryCaseInventory.getBattery(ItemStack)` 的判定：

```java
IElectricItem item = stack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
if (item == null || !item.canProvideChargeExternally()) return null;
```

即 **必须是 `IElectricItem`，且可以对外供电**（`canProvideChargeExternally()`）。
普通的"可充电工具/机器"不会被误判为电池。

### 4.2 同 tier 限制

`BatteryCaseInventory.isItemValid(int, ItemStack)`：

- **电池盒自己不能放进去**（`stack.getItem() instanceof ItemBatteryCase` → 拒绝）：
  否则 UI 里会出现"电池盒套电池盒"，并造成自引用 / NBT 无限嵌套；
- 空盒：任意 tier 的电池都能放；
- 盒内已有电池：新电池的 `getTier()` 必须与之一致。

### 4.3 能量 = 4 格求和

`BatteryCaseEnergyStorage` 完全不做缓存，每次实时聚合：

| 方法 | 返回 |
| --- | --- |
| `getCharge()` | 4 格 `getCharge()` 之和 |
| `getMaxCharge()` | 4 格 `getMaxCharge()` 之和 |
| `getTier()` | 盒内电池的 tier（空盒返回 `GTValues.ULV`） |
| `getTransferLimit()` | `GTValues.V[getTier()]` |

充放电**逐格转发**给内部电池：

- `charge(...)`：槽位 1 → 4 依次充；
- `discharge(...)`：**策略 A，槽位 1 → 4 依次取**（避免频繁切换电池）。

tier 检查：`chargerTier / dischargerTier < 盒内 tier` 时直接返回 `0`。

---

## 5. 右键行为

| 操作 | 行为 |
| --- | --- |
| 普通右键 | `PlayerInventoryHolder.openHandItemUI(player, hand)` 打开 4 格 UI |
| 潜行右键 | 切换 **释能模式**（`DischargeMode` NBT 键，与 GT 电池的 `ElectricStats` 同名同格式），并在动作栏提示 |

### 释能模式

开启后，`onUpdate` 每 tick 把电池盒的电量自动充给 **玩家背包**
（装了 Baubles 则并入 **饰品栏**，通过 `BatteryCaseBaubles.wrapInventory` 拿到合并视图）中
其它可充电物品，逻辑与 GT `ElectricStats#chargeElectricItem` 的两阶段充能完全一致。

---

## 6. UI

基于 **GT 实际发布包中的旧版 GUI API**（不是 master 源码里的 `gregtech.api.mui.*`）：

```java
public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer player)
```

- 尺寸 `176 x 166`（GT 标准），玩家背包固定在 `y = 84`；
- 标题为物品显示名；
- 4 个 `SlotWidget` 排成一行，水平居中（`x = 52 + i * 18`，`y = 24`），
  背景 `GuiTextures.SLOT`；
- 放置限制由 `BatteryCaseInventory#isItemValid` 保证；
- 槽位变动时调用 `holder.markAsDirty()`。

> `PlayerInventoryHolder#createUI` 会在物品的 behaviour 列表里查找实现了
> `ItemUIFactory` 的组件并调用它 —— 所以把 `ItemUIFactory` 实现在
> `BatteryCaseBehaviour` 上即可，无需改动 `MetaItem`。

---

## 7. 饰品栏（Baubles）

1.12.2 的饰品 API 是 **Baubles**（不是 Curios）。

`BatteryCaseBaubles` 复用 GT 的 `gregtech.integration.baubles.BaubleBehavior`
（`BaubleType.TRINKET`，与 GT 电池一致），并且 **只在 `Loader.isModLoaded("baubles")` 为真时**
才被调用（见 `SuMetaItems#init`）——与 GT `BaublesModule` 相同的隔离做法，
避免未装 Baubles 时 `NoClassDefFoundError`。

---

## 8. 本地化键

`assets/susyplusplus/lang/en_us.lang` / `zh_cn.lang`：

```
metaitem.battery_case.name
susyplusplus.tooltip.battery_case.charge        # 电量：%s / %s EU
susyplusplus.tooltip.battery_case.tier          # 电压：%s
susyplusplus.tooltip.battery_case.tier_empty    # 电压：（无电池）
susyplusplus.tooltip.battery_case.count         # 电池数：%s / %s
susyplusplus.tooltip.battery_case.open_ui       # 右键打开电池盒
susyplusplus.tooltip.battery_case.toggle_mode   # 潜行右键切换释能模式
susyplusplus.tooltip.battery_case.same_tier     # 仅可放入相同电压等级的电池
```

释能模式的开关提示直接复用 GT 自带的
`metaitem.electric.discharge_mode.enabled` / `...disabled`。

---

## 9. 客户端模型注册

`SuClientEvents#onModelRegistry`（客户端 `ModelRegistryEvent`）会分别对
`WATERPROOF_SPRAY_ITEM` 与 `BATTERY_CASE_ITEM` 调用
`registerModels()` + `registerTextureMesh()`，确保模型一定被注册。

---

## 10. 构建与验证

```bat
gradlew.bat compileJava
gradlew.bat copyModToRunMods    :: 构建 reobfJar 并复制到 run\mods\susyplusplus.jar
```

> `copyModToRunMods` 复制的是 **`reobfJar`（SRG 名）**，不能用 `jar`（MCP 名），
> 否则会在生产运行时抛 `NoSuchMethodError: I18n.format(...)`。

验证清单：

- [ ] 创造模式物品栏能找到 `电池盒`；
- [ ] 盒内放 2 个同 tier 电池 → tooltip 电量/容量为两者之和，电压显示对应 tier；
- [ ] 尝试放入不同 tier 的电池 → 被拒绝；
- [ ] 尝试放入非电池（如扳手）→ 被拒绝；
- [ ] 退出重进世界 → 盒内电池仍在（`ForgeCaps` 持久化）；
- [ ] 客户端 tooltip 也能看到电量（share tag 同步）；
- [ ] 给电池盒充电/放电，能量正确分摊到 4 格；
- [ ] 潜行右键 → 释能模式切换提示；开启后背包内工具被自动充电；
- [ ] 装 Baubles 时放入饰品栏仍能工作。

---

## 11. 合成配方

组装机（Assembler），LV / 200 ticks / 30 EU/t → 输出 1x 电池盒。

| 输入 | 数量 |
| --- | --- |
| 钢板 `OrePrefix.plate, Materials.Steel` | 4 |
| 聚乙烯箔 `OrePrefix.foil, Materials.Polyethylene` | 4 |
| 1x 铜导线 `OrePrefix.wireGtSingle, Materials.Copper` | 4 |
| 小型铜弹簧 `OrePrefix.springSmall, Materials.Copper` | 4 |
| 铜箔 `OrePrefix.foil, Materials.Copper` | 4 |
| 焊锡 `Materials.SolderingAlloy` | 36 mB（`GTValues.L / 4`） |

### 关于"72 锡 或 36 焊锡"

**不需要写两条配方。** GT 的 `RecipeMapBuilder` 为 `ASSEMBLER_RECIPES` 注册了 onBuild 钩子：

> 当一条组装机配方 **只有 1 种流体输入** 且该流体是 `SolderingAlloy` 时，
> 会自动复制并追加一条把焊锡换成 `Tin`、用量为 **2 倍** 的等价配方。

即：本配方写 `SolderingAlloy(36 mB)`，GT 会自动生成 `Tin(72 mB)` 的替代配方。
用 javap 反编译 2.8.7-beta 的 `RecipeMaps.lambda$static$1(AssemblerRecipeBuilder)`
已确认该钩子在发布包中真实存在。

### API 命名注意（GTCEu 与 1.12 旧 GTCE 的差异）

| 你以为 | 实际（GTCEu 2.8.7-beta） |
| --- | --- |
| `OrePrefix.wireGt01` | **`OrePrefix.wireGtSingle`** |
| `Materials.Solder` | **不存在**，只有 `Materials.SolderingAlloy` |

---

## 11.5 Pyrotech 相关配方

用 GT 机器替代 Pyrotech 的手工流程。Pyrotech 物品通过
`ForgeRegistries.ITEMS.getValue(new ResourceLocation("pyrotech", name))` 获取
（等价于 GroovyScript 的 `item('pyrotech:xxx', meta)`）；物品缺失时只告警并跳过该配方。

| 机器 | 输入 | 输出 | 时长 / 电压 |
| --- | --- | --- | --- |
| 干燥机 `SuSyRecipeMaps.DRYER_RECIPES` | `pyrotech:material:13` | `pyrotech:material:12` | 10 t / 7 EU |
| 提取机 `RecipeMaps.EXTRACTOR_RECIPES` | `cobblestone`（矿辞） | `pyrotech:rock` x8 | 10 t / 7 EU |
| 锻造锤 `RecipeMaps.FORGE_HAMMER_RECIPES` | `minecraft:flint` | `pyrotech:material:10` x3 | 10 t / 7 EU |

### 干燥机不在 GTCEu 本体里

**GTCEu 的 `RecipeMaps` 没有 `DRYER_RECIPES`** —— 已用 javap 核实 2.8.7-beta 与 2.8.10-beta
均无此字段。干燥机是本整合包自带的 **Susy-Core**（modid `susy`，
`run/mods/Susy-Core-0.1.118.jar`）添加的，配方表在：

```
supersymmetry.api.recipes.SuSyRecipeMaps.DRYER_RECIPES   // RecipeMap<SimpleRecipeBuilder>
```

因此本工程在 `gradle/scripts/dependencies.gradle` 里加了编译期依赖：

```groovy
compileOnly fileTree(dir: 'run/mods', include: ['Susy-Core-*.jar'])
```

并用 `Loader.isModLoaded("susy")` 保护 —— 未装 Susy-Core 时不会执行到 `SuSyRecipeMaps`，
因此不会触发类加载（`NoClassDefFoundError`）。

> 注意：粘贴进 Java 文件的 GroovyScript 写法（`item('pyrotech:material', 13)`、
> `.Inputs(...)` / `.Outputs(...)`）不是合法 Java —— Java 里是 `.inputs(...)` / `.outputs(...)`，
> 且字符串必须用双引号。已按 GT 的 Java `RecipeBuilder` API 重写。

---

## 12. 待办

- 时长 / 电压目前是我按 GT 组装机惯例取的 **200 ticks / 30 EU/t**，
  若需调整请直接改 `SuRecipes#registerBatteryCaseRecipe`。
- 重启 `runClient` 后按 §10 的清单在游戏内实测。
