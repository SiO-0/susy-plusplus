# 无线能量传输塔（Wireless Energy Transmission Tower）

> 代码：[`MetaTileEntityWirelessEnergyTower.java`](../src/main/java/com/susy/plusplus/multiblock/wireless/MetaTileEntityWirelessEnergyTower.java)、
> [`WirelessTowerTarget.java`](../src/main/java/com/susy/plusplus/multiblock/wireless/WirelessTowerTarget.java)
> 注册：[`SuMetaTileEntities.java`](../src/main/java/com/susy/plusplus/multiblock/SuMetaTileEntities.java)

---

## 1. 基本信息

| 项 | 值 |
| --- | --- |
| 注册名 | `susyplusplus:wireless_energy_tower` |
| MTE 数字 ID | `32101` |
| 本地化键 | `susyplusplus.machine.wireless_energy_tower.name` |
| 控制器方块 | 顶层第 4 行第 3 列（`S`） |
| 配置开关 | `enableWirelessEnergyTower`（默认 `true`） |
| 配方 | **无**（不是配方机器，因此 `RecipeMapMultiblockController` 不适用） |
| 耗电 | **机器自身不消耗 EU** |
| 维护 | **需要维护**（结构里必须有 1 个维护仓） |
| JEI 预览 | 由 GT 的 `registerMetaTileEntity` 自动注册，无需自写插件 |

### 为什么继承 `MultiblockWithDisplayBase`

它既不是配方机器（没有 RecipeMap），也不是 primitive 机器（要能插仓室）；
所以直接继承带 GUI / 维护 / 显示文本的基类，自管逻辑。

---

## 2. 结构

**5 层 × 每层 5 行 × 每行 9 字符**。`aisle()` 的顺序：**第 1 个 = 最底层（y=0）**，
**第 5 个 = 最顶层（y=4）**；每个 `aisle(...)` 里的 5 个字符串依次是沿 Z 的 5 行。

```
y=0（最底层）              y=1/2/3（中间三层，完全相同）   y=4（最顶层）
  R # # # R # # # R           # # # # # # # # #              R # # # R # # # R
  R # # # R # # # R           # # # # # # # # #              R # # # R # # # R
  R R R R R R R R R           R X X X R X X X R              R R R R R R R R R
  R R R R R # # # R           R # # # R # # # #              R R S R R # # # R
  # R R R # # # # #           R R R R R # # # #              # R R R # # # # #
```

| 字符 | 方块 | 说明 |
| --- | --- | --- |
| `R` | 脱氧钢机械方块 **或** 仓室 | `MetaBlocks.METAL_CASING` + `MetalCasingType.STEEL_SOLID`（中文名「脱氧钢机械方块」，`tile.metal_casing.steel_solid.name`） |
| `X` | 淡色混凝土 | `MetaBlocks.STONE_BLOCKS.get(StoneVariant.SMOOTH)` + `StoneType.CONCRETE_LIGHT` |
| `S` | 控制器本体 | `selfPredicate()` |
| `#` | 空气 | `air()`，**必须**是空气 |

### `R` 允许的仓室

| 仓室能力 | 上限 | 用途 |
| --- | --- | --- |
| `MAINTENANCE_HATCH` | 1 | 本机需要维护 |
| `IMPORT_ITEMS` | 4 | 放**标记无人机**与**电池**（每个输入总线至少 4 格 → 最多 16 格） |
| `INPUT_ENERGY` | 2 | 从电网给主方块充电（可选，也可以只用电池） |

> 没有 `EXPORT_ENERGY` / 没有物品流体仓：塔只做「充能」这一件事。

对应的 `aisle` 代码（可直接对照）：

```java
FactoryBlockPattern.start()
    .aisle("R###R###R", "R###R###R", "RRRRRRRRR", "RRRRR###R", "#RRR#####")
    .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
    .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
    .aisle("#########", "#########", "RXXXRXXXR", "R###R####", "RRRRR####")
    .aisle("R###R###R", "R###R###R", "RRRRRRRRR", "RRSRR###R", "#RRR#####")
    .where('R', casingOrHatchPredicate())
    .where('X', states(concreteState()))
    .where('#', air())
    .where('S', selfPredicate())
    .build();
```

---

## 3. 玩法与流程

### 3.1 标记目标：**铁砧改名的无人机**

Susy-Core 的货运无人机（`SuSyMetaItems` 的 `BASIC/ADVANCED/ELITE_CARGO_DRONE`）用**铁砧改名成坐标**：

```
120 64 -350
```

控制器直接读取物品名（`ItemStack#getDisplayName()`，即 NBT 的 `display.Name`），
**服务端也能读到**（铁砧改名不是客户端专属）。

解析规则（`parseCoordinates`）：

- 名字里用正则抽出**整数**，取**前 3 个**作为 `x y z`；
- 因此 `120 64 -350`、`120,64,-350`、`x120 y64 z-350` 都能用；
- 少于 3 个整数（例如没改名的「Basic Cargo Drone」）→ 记为**无效目标**，红字提示并跳过。

无人机**不会被消耗**，一直放在输入总线里即可。

### 3.2 电池：决定容量与电压

输入总线里的 GT 电池（`IElectricItem` 且 `canProvideChargeExternally()`）：

| 读到的量 | 用途 |
| --- | --- |
| 电池**数量** | UI 显示 `x / 16`；超过 16 → 红字「电池过多」；0 → 红字「没有电池」 |
| 电池**总容量**（`getMaxCharge()` 求和，只计前 16 个） | **主方块电量的上限** |
| **最低**电池电压（`getTier()` → `GTValues.V[tier]`） | **传输电压** |

### 3.3 主方块电量怎么来（两条路都支持）

1. **能源仓**：每个 tick 把 `EnergyContainerList` 里的电搬进主方块，直到装满；
2. **电池**：每 20 tick（扫描时）用 `IElectricItem#discharge(..., externally = true, ...)`
   把总线里电池的电放出来充入主方块。

### 3.4 传输流程

```
每 20 tick：重扫总线（电池数 / 容量 / 电压 / 目标）+ 电池放电充入主方块
每 tick   ：能源仓充入主方块 → 推进各目标（传输中持续输出能量）
```

对每个目标：

1. 空闲时检查目标机器（`GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER`）；
   - 找不到 / 容量为 0 → 记「无效目标」，红字提示并跳过；
   - 电量 **≥ 50%** 最大电量 → 不工作；
2. 否则开始一次传输，时长 `ceil((5 + 欧氏距离 / 5) × 20)` tick；
3. **只在最后 3 秒**（`INJECT_WINDOW = 60` tick）持续输出
   `传输电压 × 电池个数 × 64`（EU/t）：
   - 实际送出量 = `min(每 tick 输出, 目标还能接收的量, 主方块当前电量)`；
   - 时长的其余部分只是「飞行 / 准备」时间，**不送电**；
   - 时长走完后再重新判断（若仍低于 50% 就再来一轮）。

### 3.5 关键参数

| 常量 | 值 | 含义 |
| --- | --- | --- |
| `MAX_BATTERIES` | 16 | 计入容量的电池上限 |
| `MAX_TARGETS` | 9 | 最多标记无人机 / 目标数 |
| `SCAN_INTERVAL` | 20 tick | 重扫总线间隔 |
| `INJECT_WINDOW` | 60 tick | 只在传输的**最后 3 秒**输出能量 |
| `OUTPUT_MULTIPLIER` | 64 | 输出倍率 |
| 输出功率 | `传输电压 × 电池个数 × 64` | 最后 3 秒内**每 tick**的输出（EU/t） |
| `BASE_SECONDS` | 5.0 | 传输基础时长（秒） |
| `BLOCKS_PER_SECOND` | 5.0 | 每 5 格距离 +1 秒 |
| 距离 | 欧氏距离（**含 Y**） | `sqrt(dx² + dy² + dz²)` |

> 传输用 `IEnergyContainer#acceptEnergyFromNetwork(side, 传输电压, 电流)`，
> 而不是 `addEnergy`：电压/电流都交给目标机器自己的判定，
> 「传输电压 = 最低电池电压」才有意义（目标输入电压低于该电压时会拒绝，而不是被硬塞）。

---

### 3.6 合成配方（组装机）

`RecipeMaps.ASSEMBLER_RECIPES`，**512 EU/t**，**1200 tick（60 s）**：

| 输入 | 数量 |
| --- | --- |
| HV 机器外壳（`MetaTileEntities.HULL[HV]`） | 1 |
| HV 发射器（`MetaItems.EMITTER_HV`） | 4 |
| HV 接收器（`MetaItems.SENSOR_HV`） | 4 |
| HV 电路（矿词 `circuit` + `MarkerMaterials.Tier.HV`） | 16 |
| 聚氯乙烯板（`platePolyvinylChloride`） | 32 |
| 铝线缆（1x，`cableGtSingleAluminium`） | 32 |
| 润滑剂（流体） | 16000 L |
| 电路 13（`circuitMeta(13)`，**不消耗**） | 1 |

共 **7 个物品输入**（含不消耗的编程电路）与 **1 种流体**，
都在 `ASSEMBLER_RECIPES` 的 `itemInputs(9)` / `fluidInputs(1)` 限制之内。

> 该配方同样受 `enableWirelessEnergyTower` 控制：
> 机器未注册时不会写入配方。

---

## 4. GUI 文本

`addDisplayText`（常规，白色）：

| 键 | 内容 |
| --- | --- |
| `...tower.energy` | 电量：`当前 / 上限` EU |
| `...tower.batteries` | 电池：`数量 / 16` |
| `...tower.voltage` | 传输电压：`xx EU/t`（有电池时才显示） |
| `...tower.targets` | 工作中目标：`n 个 / 无人机：m 个` |
| `...tower.progress` | 传输进度：`已进行 / 总时长` tick（传输中） |
| `...tower.output` | 输出：`xx EU/t`（仅在最后 3 秒、真的在送电时出现） |
| `...tower.idling` | 待机（空闲时） |

`addErrorText`（**红字**）：

| 键 | 触发条件 |
| --- | --- |
| `...tower.no_battery` | 电池数 = 0 |
| `...tower.too_many_batteries` | 电池数 > 16 |
| `...tower.no_drone` | 无人机数 = 0 |
| `...tower.too_many_drones` | 无人机数 > 9 |
| `...tower.invalid_target` | 名称里凑不出坐标的无人机数量（>0） |
| `...tower.target_offline` | 有坐标但该处没有可充能机器的目标数量（>0） |

`addWarningText`（黄字）：`...tower.no_energy` —— 有电池但主方块电量为 0。

状态行使用「正在传输 / 待机」（`...tower.running`）。

---

## 4.1 The One Probe（进度条）

GT 的 TOP 集成里**没有**多方块进度 provider，所以本模组自写了一个：
[`WirelessTowerInfoProvider.java`](../src/main/java/com/susy/plusplus/integration/top/WirelessTowerInfoProvider.java)

- 继承 GT 自己的 `gregtech.integration.theoneprobe.provider.CapabilityInfoProvider<IMultiblockController>`，
  能力取 `GregtechCapabilities.CAPABILITY_MULTIBLOCK_CONTROLLER`
  → 指向**控制器或任意仓室**都能看到（与 GT 的 `MultiblockInfoProvider` 同一套机制）。
- 在 [`SuTopIntegration`](../src/main/java/com/susy/plusplus/integration/top/SuTopIntegration.java) 里注册。
- 传输中：`传输进度` 标签 + **进度条**（`IProbeInfo#progress(已进行, 总时长, style)`，
  150px 宽、蓝色填充）；**最后 3 秒**真的在送电时追加 `输出 xx EU/t`；空闲时显示 `待机`。
- 始终显示：`电量 x / y EU`、`目标无人机 n`。
- TOP 的信息是**在服务端组装后**再发给客户端的，所以这里直接读塔的服务端字段即可，
  **不需要**额外做客户端同步。

---

## 5. 实现要点与坑

### 5.1 ⚠ UI API：**master 源码 ≠ jar**

本模组引用的 GTCEu 版本（编译 2.8.7-beta / 运行 2.8.10-beta）里：

- **有**：`addDisplayText / addWarningText / addErrorText(List<ITextComponent>)`、
  `maintenancePredicate()`、`MultiblockDisplayText`、`createUITemplate(EntityPlayer)`；
- **没有**：`gregtech.api.metatileentity.multiblock.ui.MultiblockUIBuilder`、
  `gregtech.api.util.KeyUtil`。

因此这里用的是**旧版 `ITextComponent` 列表**写法（与 Susy-Core 一致），
而不是 somemods 里那份更新 master 源码的 `MultiblockUIBuilder` 写法。
若照抄 master 源码会直接编译失败。

### 5.2 不依赖 `getOffsetTimer()`

扫描计时用**自己的 `scanTimer` 计数器**，避免不同版本 `getOffsetTimer()` 的差异。

### 5.3 目标状态复用

`WirelessTowerTarget` 对象**不持久化**；每次重扫按坐标
（`resolveTarget`）复用旧对象，这样**进行中的传输进度不会被扫描打断**。
只有主方块电量 `TowerEnergyStored` 写入 NBT。

### 5.4 拆分仓室数量而不是塞一个总线

电池要占格子，所以 `IMPORT_ITEMS` 上限给到 **4 个输入总线**（≥16 格），
正好对应 16 个电池的上限。

### 5.5 正面 overlay 的「工作中」动画怎么同步

`working` 是**服务端权威**的；客户端上这个字段永远是 `false`。如果 `isActive()`
直接读 `working`，客户端的正面 overlay 就永远停在 inactive 贴图。

所以：`setWorking(...)` 在服务端额外调用基类的 `setLastActive(working)`
（它把状态写进 `IS_WORKING` 自定义包），客户端在 `receiveCustomData` 里存进
`lastActive`；`isActive()` 两端统一返回 `super.isActive() && lastActive`。

overlay 用 GT 的 `Textures.POWER_SUBSTATION_OVERLAY`，它自带
`overlay_front.png` / `overlay_front_active.png`，因此传输时会自动切成 active 动画。

### 5.6 进度为什么放在 TOP / GUI 文本，而不用 `IProgressBarMultiblock`

- **TOP**：信息在服务端组装（`WirelessTowerInfoProvider`），读服务端字段天然正确。
- **GUI 文本**：`addDisplayText` 由服务端生成文本后同步（Susy-Core 也是这么读服务端字段的）。
- **GUI 的 `IProgressBarMultiblock` 进度条没有采用**：jar 里 GT 用
  `new ProgressWidget(DoubleSupplier, ...)` 建条，`DoubleSupplier` 指向
  `getFillPercentage(i)` 并在**运行时**回调；客户端那份 `progress / workTime`
  没有同步，条会一直是 0。宁可不显示，也不显示错的。

---

## 6. 验证清单（进游戏）

1. 按图搭好结构（含 1 个维护仓、至少 1 个输入总线）→ GUI 应显示「电量 0 / 0 EU」、红字「没有电池」「没有无人机」。
2. 输入总线放 1 个电池 → 红字消失，上限变成该电池容量；放 17+ 个电池 → 红字「电池过多（最多 16 个）」。
3. 铁砧把货运无人机改名为 `x y z`（目标机器坐标）后放入总线 → 「无人机：1 个」；
   放 10 个 → 红字「无人机过多（最多 9 个）」；放未改名的 → 红字「名称里没有坐标」。
4. 放能源仓并接电 → 「电量」上升（上限 = 电池总容量）；不接电仓时电池也会缓慢放电充入。
5. 目标机器电量 < 50% → 状态变「正在传输」、正面 overlay 切换为 active 动画；
   在**最后 3 秒**开始真正送电：每 tick `传输电压 × 电池个数 × 64` EU
   （主方块电量耗尽、或目标已被充满则不再送）。
6. 用 TOP 看向控制器或任意仓室 → 显示 `传输进度` **进度条**、`输出 xx EU/t`、
   `电量 x / y EU`、`目标无人机 n`。
7. 目标机器被拆 → 红字「目标位置没有可充能的机器」。
