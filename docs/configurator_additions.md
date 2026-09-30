# 配置器新增功能（复制朝向/锁定、工具箱开关、流体容量限制）

> 本文对应新增的三项功能。所有 API 都已在**运行期** `run/mods/gregtech-1.12.2-2.8.10-beta.jar`
> 用 `javap` 核实过（注意：运行期是 **2.8.10**，与本项目编译用的 2.8.7 有差异，下文会标出）。
> 代码位置：[`item/configurator`](../src/main/java/com/susy/plusplus/item/configurator)、
> [`machine`](../src/main/java/com/susy/plusplus/machine)、[`gui`](../src/main/java/com/susy/plusplus/gui)。

---

## 1. 复制机器配置：新增内容

`ConfiguratorData` / `MachineConfig` 的复制范围在原有基础上**追加**了两类（旧键名一律不改，旧存档照读）：

### 1.1 各面朝向

| 机器类型 | 复制内容 | 来源 API |
| --- | --- | --- |
| 单方块机器 | 正面朝向 `FrontFacing`（原有） | `MetaTileEntity#hasFrontFacing/getFrontFacing/isValidFrontFacing/setFrontFacing` |
| **多方块控制器**（新增） | 上方朝向 `UpwardsFacing`、是否翻转 `IsFlipped` | `MultiblockControllerBase#getUpwardsFacing/setUpwardsFacing`、`isFlipped()`，翻转写入用**反射**调用 `protected setFlipped(boolean)`（见 [`GtMachineReflection`](../src/main/java/com/susy/plusplus/machine/GtMachineReflection.java)） |

> GT 的朝向模型**不是**"每面各存一份"：单方块只有 `frontFacing`，
> 多方块控制器只多一个 `upwardsFacing` + `isFlipped`
> （`MetaTileEntity` 里根本没有 `upwardsFacing`，已 javap 核实）。

### 1.2 锁定的流体（`LockedFluids`）

| 机器 | 内容 | 来源 |
| --- | --- | --- |
| 量子缸 `MetaTileEntityQuantumTank` | 锁定流体 `LockedFluid` | `protected boolean locked` / `private FluidStack lockedFluid` / `protected void setLocked(boolean)`，读写走**反射**（`lockedFluid` 是 `private`，`@Shadow` 用不了） |

写回顺序刻意与 GT 自己的界面回调一致：先 `setLocked(fluid != null)`，**再**给 `lockedFluid` 赋值
（因为 `setLocked(true)` 会用"当前罐内流体"覆盖该字段）。

### 1.3 ⚠ 不做的部分：`LockedItems`

需求里的"锁定物品"在本整合包的 GT 版本里**不存在**：

- `MetaTileEntityQuantumChest`（量子箱）的常量池里**没有** `LockedStack` / `Locked` / `IsLocked`；
- 它**直接继承 `MetaTileEntity`**，master 源码里的 `MetaTileEntityQuantumStorage` 在 2.8.10 **根本不存在**（javap 报"找不到"）。

因此本模组**不提供** `LockedItems`（按你的确认跳过）。

### 1.4 明确不复制

方块 id / 坐标 / 能量缓存 / 物品缓存 / 流体缓存 / 进度 / 配方 / 升级 / 封面（cover）。

---

## 2. 机器工具箱：新增开关「禁止不同槽位被动输入相同物品」

- 界面：[`MachineToolboxUI`](../src/main/java/com/susy/plusplus/gui/MachineToolboxUI.java)
  （配置器模式 3，普通右键机器打开）。**只有存在物品输入槽位的机器**才出现该按钮
  （`mte.getItemInventory().getSlots() > 0`），没有物品槽的机器连按钮都不显示。
- 状态与持久化：[`MachineExtraState`](../src/main/java/com/susy/plusplus/machine/MachineExtraState.java)
  - 写机器 NBT 键 `SuNoDuplicateImport`；
  - 另走 GT 自己的 `writeInitialSyncData / receiveInitialSyncData` 初次同步，所以**客户端**也能显示正确状态；
  - 点击回调（`InteractionSyncHandler`）两端各跑一次，因此客户端标签**立即**刷新。
- 拦截实现（关键设计）：**在能力层包装**，而不是改 `insertItem`。
  - 混入点：[`MetaTileEntityExtraStateMixin#getCapability`](../src/main/java/com/susy/plusplus/mixin/MetaTileEntityExtraStateMixin.java)
    +
    [`SimpleMachineItemCapabilityMixin#getCapability`](../src/main/java/com/susy/plusplus/mixin/SimpleMachineItemCapabilityMixin.java)
    （`SimpleMachineMetaTileEntity` **覆写了 `getCapability` 且不调用 super**，所以必须单独挂一份）。
  - 包装类：`MachineExtraState.NoDuplicateItemHandler` —— `insertItem` 时若"**别的槽位**已有同种物品（NBT 也相同）"就整堆拒收。
  - **为什么这样能区分被动/玩家输入**：外部自动化（漏斗、管道、机械臂、GT 覆盖板、`pushItemsIntoNearbyHandlers`）拿的是
    `getCapability(ITEM_HANDLER, side)` 返回的对象 → 被包装；而**玩家 GUI 用的是机器内部的 handler 引用**
    （`SimpleMachineMetaTileEntity#createUITemplate(..., importItems, ...)` 直接把内部对象交给界面），
    永远拿不到包装器 → 玩家手动放入**不受限制**。
- ⚠ 已知边界：GT 内部**直接**调用 `getImportItems()` 的自有逻辑（不经能力）不会被拦；
  GUI 槽位本身走的是内部对象（这正是我们要的行为）。这条边界已在此写明，避免误解。

---

## 3. 容量限制（仅流体）

- 新增配置器模式 4：`ConfiguratorMode.FLUID_CAPACITY`（**追加在末尾**，不影响旧存档的 `Mode` 数值）。
- 操作：选该模式后**普通右键**机器方块或**仓室**。
  - 没有流体槽（例如物品总线）→ 提示「该机器没有流体槽位」，不打开界面；
  - 有流体槽（流体输入仓/输出仓、普通流体机器、量子缸、流体样品存储…）→
    打开 [`FluidCapacityUI`](../src/main/java/com/susy/plusplus/gui/FluidCapacityUI.java)。
- 界面：每页最多 **8 行**，每行 **槽位号 + 左侧可编辑容量输入框**（限非负整数、上限=原容量）+ **右侧只读原容量**；
  底部「确认」写入机器并退出，「取消」/ESC 退出且不写入。
  - **行数按实际槽位数**：只有 1 个流体槽的机器（例如流体输入仓、量子缸）就只画 **1 行**，
    不会再出现"一槽机器却有 8 个空输入框"；只有槽位多于一页时才满 8 行/页，
    且多页机器**最后一页不足的槽位会置灰禁用**（不可编辑）。
- **分页**：槽位多于一页时，底部出现「◀ 上一页 / 下一页 ▶」与「第 x / y 页」；
  翻页会先把当前页输入框里的值存进本界面的工作副本再刷新显示，
  「确认」时把**所有页**的最终值一次性发给服务端 —— 因此跨页编辑不会丢。
  （刻意<b>不用</b> ModularUI 的 `PagedWidget`/`syncValue`：编译期 3.0.4 与运行期 3.1.6 在这些 API 上有差异，
  本项目的 `MachineFaceUI` 已经因此吃过 `NoSuchMethodError`；这里用纯客户端的本地状态 + 自定义包，行为完全可控。）
- 槽位枚举与容量读写：[`FluidCapacityHelper`](../src/main/java/com/susy/plusplus/machine/FluidCapacityHelper.java)
  - **递归拆包**（都用 javap 在运行期 GT 2.8.10 核实过）：
    1. `IMultipleTankHandler`（`FluidTankList`）→ `getFluidTanks()` →
       `MultiFluidTankEntry#getDelegate()`（**public**）；
    2. **`gregtech.api.capability.impl.FluidHandlerProxy`** —— 它<b>只实现 `IFluidHandler`</b>，
       但有两个 **public** 字段 `input` / `output`。流体**输入仓/输出仓**这类"输入与输出分开"的部件正是被它包着的，
       早期只用 `instanceof IMultipleTankHandler` 判断会<b>整台漏掉</b>（这就是"输入仓不支持"的原因）；
    3. 单槽：库存本身就是 `IFluidTank`。
  - 深度上限 4 层 + 去重，避免异常结构死循环。
  - **因此支持**：流体输入仓、流体输出仓、普通单方块流体机器（如 GT 的各类流体机器）、
    量子缸、以及本模组的流体样品存储；
    **没有流体槽的部件**（例如物品输入/输出总线）会提示「该机器没有流体槽位」，不打开界面。
  - 改容量：`net.minecraftforge.fluids.FluidTank.capacity` 是 Forge 的 `protected int` 字段，
    GT 全库没有任何 `setCapacity` 调用，Forge 源码也不在参考路径 →
    用**带缓存的反射 + 失败降级**（失败只打 WARN，不会崩）。
- 客户端 → 服务端：自定义包 [`PacketSetFluidCapacity`](../src/main/java/com/susy/plusplus/network/PacketSetFluidCapacity.java)。
  **刻意不用 ModularUI 的 `syncValue`**：编译期 3.0.4 与运行期 3.1.6 在重载上有过差异，
  本项目 `MachineFaceUI` 已经因此吃过 `NoSuchMethodError`。
- **容量调小会销毁超出部分**：把某槽容量改到低于当前存量时，服务端写入容量后会立刻把超出的流体抽掉
  （用 `IFluidTank#drain` 丢弃），避免出现"存量 > 容量"的非法状态；
  读档重新应用容量（`applyStoredCapacities`）时同样会做一次，
  所以"改小容量后立刻存档再回来"也不会残留超量流体。
- 重启后生效：`MetaTileEntityExtraStateMixin` 在 `readFromNBT` 之后调用
  `FluidCapacityHelper.applyStoredCapacities(...)` 把保存的当前容量重新应用。

---

## 4. NBT 结构

### 4.1 配置器物品（`CopiedConfig`）

键名**与旧版本完全一致**，只**追加**了后 5 个：

```
CopiedConfig: {
  HasFront, Front, HasPainting, Painting, HasMuffled, Muffled,
  HasTransformer, Inverted, HasOutput, OutItems, OutFluids,
  AutoItems, AutoFluids, AllowItems, AllowFluids,
  // ↓ 本次新增
  HasUpwards,     // boolean：是否采集到多方块朝向
  Upwards,        // int：EnumFacing.getIndex()
  IsFlipped,      // boolean
  HasLockedFluid, // boolean
  LockedFluids    // FluidStack 的 NBT
}
```

### 4.2 机器自身

| 键 | 类型 | 含义 |
| --- | --- | --- |
| `SuNoDuplicateImport` | boolean | 功能二开关 |
| `OriginalFluidCapacity` | int[] | **只在第一次写入时记录**，之后永不改变 |
| `FluidCapacity` | int[] | 当前容量（按槽位索引） |

---

## 5. 本地化键

| 键 | zh_cn |
| --- | --- |
| `susyplusplus.configurator.mode.fluid_capacity` | 容量限制（仅流体） |
| `susyplusplus.tooltip.configurator.hint.fluid_capacity` | 普通右键容器：修改各流体槽位容量 |
| `susyplusplus.gui.configurator.toolbox.no_duplicate_import` | 禁止不同槽位被动输入相同物品 |
| `susyplusplus.gui.configurator.toolbox.no_duplicate_import.hint` | 只拦自动化（漏斗/管道/机械臂）；玩家手动放入不受限 |
| `susyplusplus.gui.configurator.capacity.title` | 容量限制（仅流体） |
| `susyplusplus.gui.configurator.capacity.slot` | 槽位 |
| `susyplusplus.gui.configurator.capacity.original` | 原容量：%s |
| `susyplusplus.gui.configurator.capacity.more` | 仅显示前 8 格，还有 %s 格未显示 |
| `susyplusplus.gui.configurator.cancel` | 取消 |
| `susyplusplus.message.configurator.no_fluid_tank` | 该机器没有流体槽位 |
| `susyplusplus.message.configurator.capacity_set` | 已应用流体容量设置 |

---

## 6. 验证步骤

1. **编译**：`.\gradlew.bat build copyModToRunMods` → `BUILD SUCCESSFUL`。
2. **各面朝向**：把配置器设为模式 2，Shift+右键一个**多方块控制器**（例如 GT 的采矿场/蒸馏塔），
   再普通右键另一个同型号控制器 → 观察它的上方朝向与翻转状态与来源一致。
3. **锁定流体**：拿一个**量子缸**，用它的界面锁定某种流体 → 配置器模式 2 Shift+右键复制 →
   普通右键另一个量子缸 → 锁定流体应被复制过去。
4. **工具箱开关**：配置器模式 3 普通右键一台有输入槽的机器 → 应看到
   「禁止不同槽位被动输入相同物品 关/开」按钮；没有物品槽的机器**不显示**该按钮。
5. **被动输入被拦**：开关打开后，用漏斗/管道把"槽位 A 已有的那种物品"送进槽位 B → 应被拒收；
   **玩家自己**用界面把同种物品放进另一个槽位 → 应当允许。
6. **关闭恢复**：关掉开关 → 被动输入恢复原版行为。
7. **容量界面**：配置器模式 4 普通右键一台有流体槽的机器 → 每槽一行、左输入框右原容量；
   对没有流体槽的机器右键 → 提示「该机器没有流体槽位」。
8. **原容量只记一次**：把某槽容量改小并确认 → 再次打开界面，右侧"原容量"仍是**最初的**值。
9. **当前容量生效**：确认后机器的该流体槽上限应变为新值（可用桶/流体管道灌满验证）。
10. **重启保留**：存档退出重进 → 容量设置与工具箱开关仍然生效（NBT 持久化）。
11. **兼容性**：拿一个 1.0.5 及以前存档里的配置器（只有 `CopiedConfig` 旧键）粘贴 → 正常，不报错。

---

## 7. 已知限制

- 不提供 `LockedItems`（本 GT 版本没有该功能，见 1.3）。
- 工具箱开关只拦**经能力**的外部自动化；GT 内部直接调用 `getImportItems()` 的自有路径不会被拦。
- 容量界面每页最多 8 行、**支持翻页**（见第 3 节），所以 32 格的流体样品存储也能全部编辑；
  单页机器（1~8 槽）按实际槽位数显示行数，不会有多余空框。
- **下调容量会销毁槽内超出的流体**（见第 3 节），这是刻意行为，不是数据丢失 bug。
- 改容量用的是反射写 `FluidTank.capacity`：若某机型的槽位实现**不是** `FluidTank` 子类，
  该槽位会改不动（只打一条 WARN，不会崩）。
