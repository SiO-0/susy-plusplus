# 存储检测器（Storage Scanner）

> MV 单方块机器：扫描周围的物品容器，把它们的库存聚合成**一个可读写的存储空间**
> （实现与 GT 工作台 `MetaTileEntityWorkbench` 的 `connectedInventory` 同款）——
> 打开 GUI 就是**一格一格的真实物品**，点击即可取走、也能放回；漏斗/管道同样能取能放。

| 项 | 值 |
| --- | --- |
| 注册名 | `susyplusplus:storage_scanner` |
| 名字键 | **`susyplusplus.machine.storage_scanner.name`**（真键：`MetaTileEntity#getMetaName()` = `<命名空间>.machine.<路径>`，机器物品由 `MachineItemBlock` 读取 `名字 + ".name"`）<br>`block.susyplusplus.storage_scanner`（同一名字，兼容按方块名写键的习惯） |
| 类型 | **MV 单方块普通机器**（`extends TieredMetaTileEntity`，GT MTE 数字 ID `32102`） |
| 电压 | MV = **128 EU/t** 输入，内部缓冲 **8192 EU** |
| 耗电 | **120 EU/t**（开机且供电充足时每 tick 扣；它同时覆盖"维持库存暴露"与"每 20 tick 一次的扫描"，**不额外收扫描费**） |
| GUI | 只显示范围/开关/状态/统计，**不显示物品槽位**（按需求移除；物品取放走漏斗等自动化） |
| 获取 | 组装机（`ASSEMBLER_RECIPES`，**120 EU/t / 300 ticks**）：**MV 电路×1 + MV 机器外壳×4 + 物品探测覆盖板×2 + 玻璃板×2 + MV 传送带×1** |
| 配置开关 | `enableStorageScanner`（默认 `true`）、`storageScannerDedupeMultiblockStorage`（默认 `true`） |
| 材质 | **无需新增**：沿用 GT 的 MV 外壳（`Textures.VOLTAGE_CASINGS[2]`）+ 正面 `Textures.PIPE_OUT_OVERLAY` 覆盖层 |

---

## 1. 功能说明

1. **周期扫描**：以机器为中心，在「边长³」（3~20，默认 **5×5×5**）的正方体内扫描，**每 20 tick 一次**；
2. **扫描目标**：任何**带物品库存能力**的方块实体 —— 箱子、熔炉、GT 机器、其它 mod 的容器；
3. **结果持久化**：容器坐标列表写进机器 NBT，重登/重启不丢；
4. **聚合暴露**：扫描到的容器里**每一个槽位**都成为聚合库存的一个槽位（供**自动化**取放）；
5. **GUI 不显示物品槽位**（按需求移除）：界面只提供范围调整 / 开关机 / 状态与统计；
6. **本机器不存物品**：它只是这些容器的"窗口"（自身没有物品槽）。

## 2. 与 RFTools 存储检测器的差异（需求指定）

| 点 | 本机器 |
| --- | --- |
| **非玩家放置的容器** | **能扫到**。MC/GT 原生**不记录**方块放置者（GT 只有 `MetaTileEntity` 自己的 owner，与容器访问无关），所以"非玩家放置"天然包含：自然生成、结构生成、其它 mod 放的箱子/熔炉/GT 机器都能扫到。 |
| 形态 | **独立机器方块**，不是覆盖板。 |
| 对外接口 | 既能被**自动化取放**（`IItemHandler`），也能在 GUI 里**点击取放**。 |

## 3. 扫描范围与策略

- 范围 = **正方体边长** `3 ~ 20`，默认 `5`；区域为 `(center - (L-1)/2) ~ (center + L/2)`，即 `L³` 个方块、尽量以机器为中心（`L=5` → `±2`，对称）。
- **只在已加载区块内扫描**（`World#isBlockLoaded`），未加载区块跳过，不做 chunk-loading。
- **跳过机器自身**（避免自引用）。
- **最多记录 128 个容器**；聚合库存最多暴露 **512 个槽位**（`MAX_EXPOSED_SLOTS`，到顶就不再继续拼接）。
- **共享库存去重**（`storageScannerDedupeMultiblockStorage`，默认开）：多方块存储（例如工业复兴的
  *storage rank*）会让**每个方块实体都返回同一份库存**，逐个计入就会把同一批物品算成 **N 倍（N = 方块数）**。
  因此按「后端库存身份」判等 —— `InvWrapper#getInv()` 拆到后端 `IInventory`，拆不动就退化为处理器对象本身
  （`==` 比较）—— 同一份库存只保留一个容器。若某个容器被误判成重复，可在配置里关掉该项。
- **没有"手动扫描"按钮**（按需求移除）：扫描每 20 tick 自动进行。

## 4. 库存暴露方式（实现同 GT 工作台）

- 机器在**任意面**都返回 `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` = 聚合库存
  （覆写 `getCapability`，绕开基类"槽位数为 0 就不暴露"的判断）。
- 聚合库存内部就是 GT 自己的
  [`ItemHandlerList`](https://github.com/GregTechCEu/GregTech)（`implements IItemHandlerModifiable`）——
  与 GT 工作台的 `connectedInventory` 完全同一套做法；外面只包了一层薄壳
  [`AggregateItemHandler`](../src/main/java/com/susy/plusplus/machine/AggregateItemHandler.java)
  让**对象身份稳定**（漏斗/管道可能缓存它），内容在每次扫描后重新装配。
  - 一个已扫描容器 = 一个**子处理器**（`ContainerRef`）：槽位数在扫描时固定（保证槽位偏移稳定），
    但**每次访问都重新解析**真实容器；
  - 顺序天然是「容器列表顺序 → 容器内槽位顺序」；
  - `getStackInSlot` 直读目标容器、`extractItem` 从目标容器真实扣除 —— **格子里的就是真物品**。
- **可读也可写**（工作台语义）：`insertItem`/`setStackInSlot` 会**写进对应的真实容器**，
  所以漏斗可以往里塞、GUI 里也能把物品**放回**容器。
- **失效容器**：区块未加载 / 方块实体没了 / 能力消失 → 该子处理器表现为"空且不可放入"，
  **不操作过期 TileEntity**；下一次扫描会把失效容器从列表里剔除。

## 5. GUI（GT 旧 GUI；仓库风格对齐工作台的存储页）

> ⚠ 2.8.x 的机器 GUI 是 GT 自己的 `gregtech.api.gui.ModularUI`（**不是** master 已迁移的 MUI2 `buildUI`）；
> `createUI(EntityPlayer)` 在 2.8.x 是抽象方法。

| 位置 | 内容 |
| --- | --- |
| 顶部 | 机器名 |
| 范围行 | `-1` / `+1` + 当前范围 |
| 按钮行 | **开机-关机**（手动扫描按钮已移除） |
| 状态行 | 运行中 / 缺电 / 已关机 / 运行中但范围内没有容器 |
| 统计行 | `容器 · 物品槽位：12 · 45` |
| 底部 | 玩家背包 |

- **界面里没有物品槽位**（按需求移除）：物品的取放请通过漏斗 / 管道 / 机械臂等自动化 ——
  机器对外的聚合库存仍然照常提供（见 §4）；
- 因此界面也不需要任何"槽位同步"相关的胶水代码，面板只有范围行、开关机按钮、状态行与统计行。

## 6. 能量消耗

| 行为 | 消耗 |
| --- | --- |
| 运行（开机且供电充足） | **120 EU/t**（每 tick 扣；已包含扫描） |
| 电量不足 120 EU/t | **停机**：不扫描、不刷新库存；状态行显示「缺电」 |
| 开机/关机 | GUI 按钮，或用**软锤**右键（`IControllable`） |

- 输入电压上限 MV（128 EU/t），内部缓冲 8192 EU。

## 7. tooltip 与本地化键

tooltip（已按要求**去掉重复的说明/用法行**，只留必要信息）：

```
最大输入电压: 128 (MV)
内部储能: 8192 EU
耗电: 120 EU/t
扫描范围（边长）：5（可调 3 ~ 20）
已扫描容器：12
```

| 键 | 用途 |
| --- | --- |
| `susyplusplus.machine.storage_scanner.name` | **真实**机器名键 |
| `block.susyplusplus.storage_scanner` | 同一名字（兼容写法） |
| `susyplusplus.machine.storage_scanner.tooltip.range` / `.containers` | 扫描范围 / 已扫描容器 |
| `susyplusplus.gui.storage_scanner.range` / `.toggle` / `.counts` | GUI 的范围行、开关机按钮、统计行（`%s` 格式串） |
| `susyplusplus.gui.storage_scanner.state.running` / `.no_power` / `.no_container` / `.disabled` | GUI 状态行 |

> 本模组使用 **`.lang`**（`assets/susyplusplus/lang/{en_us,zh_cn}.lang`）：**1.12.2 不支持 `.json` 语言文件**。
> ⚠ `SimpleTextWidget` 的文本**由服务端求值**、格式键在客户端翻译 —— 供给器只返回「数字 / 坐标 / 键名」，绝不调用 `I18n`。

## 8. 实现文件

| 文件 | 作用 |
| --- | --- |
| [`StorageScannerMachine.java`](../src/main/java/com/susy/plusplus/machine/StorageScannerMachine.java) | 机器本体：扫描、去重、能量、状态、NBT、GUI、tooltip |
| [`AggregateItemHandler.java`](../src/main/java/com/susy/plusplus/machine/AggregateItemHandler.java) | 聚合库存（薄包装 GT 的 `ItemHandlerList`，身份稳定、内容随扫描重建） |
| [`SuMetaTileEntities.java`](../src/main/java/com/susy/plusplus/multiblock/SuMetaTileEntities.java) | 注册（ID 32102） |
| [`SuConfig.java`](../src/main/java/com/susy/plusplus/config/SuConfig.java) | `enableStorageScanner` / `storageScannerDedupeMultiblockStorage` |
| [`SuRecipes.java`](../src/main/java/com/susy/plusplus/recipe/SuRecipes.java) | MV 组装机配方 |
| `lang/{zh_cn,en_us}.lang` | 本地化 |

---

## 9. 验证步骤

1. **编译**：`gradlew build` 通过；`gradlew copyModToRunMods` 把 jar 同步进 `run/mods`。
2. **创造模式可获取**：GT 机器页里找到「存储检测器」（MV 外壳 + 正面输出口覆盖层）。
3. **放置并供电**：贴 MV 能源线缆/发电机；tooltip 显示 `MV / 128 EU/t / 8192 EU / 120 EU/t`。
4. **扫描**：周围放几个箱子（**至少一个用指令/结构生成/其它 mod 放置，即"非玩家放置"**）并放不同物品 → 右键开 GUI（扫描每 20 tick 自动进行，稍等一拍）→ 统计行的 `容器 · 物品槽位` 数量增加；界面上**不再有物品格**。
5. **漏斗取放**（物品进出的唯一交互方式）：机器旁放漏斗 → 能从**扫描到的容器**里抽出物品，也能把物品送进那些容器（按容器顺序、容器内按槽位顺序）。
6. **界面内容**：只有「范围 -1/+1」「开机-关机」「状态行」「统计行」+ 玩家背包，没有槽位、没有翻页、没有提示行。
7. **容器被破坏**：拆掉一个箱子 → 下次扫描后容器数/槽位数减少；即使没到下次扫描，对应格子也只是"空"（不报错）。
8. **区块未加载**：把某个容器所在区块卸载 → 对应格子为空，机器不崩。
9. **开关机/范围/缺电**：范围 `-1/+1` 生效并持久化；关机后状态行变「已关机」；断电后状态行变「缺电」且不再刷新；范围内没容器时状态行提示「运行中，但范围内没有容器」。
10. **tooltip**：只有电压等级 / 储能 / 耗电 120 EU/t / 扫描范围 / 已扫描容器数（无重复说明行）。
11. **存档重启**：扫描后保存退出 → 重进，容器/槽位统计与网格内容仍在，取放与漏斗都正常。
