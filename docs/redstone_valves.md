# 红石控制阀门（Redstone Valves）

> 新增三个可红石控制的 GT 单方块机器：**流体阀**、**物流阀**、**IR 流体阀**。
> 代码位置：`com.susy.plusplus.machine.valve`
> 开关：`enableRedstoneValves`（默认 **开**）

---

## 1. 三者对比

| 机器 | 注册名 | MTE id | 传送对象 | 主动抽取 | 外壳贴图 | 面标记 |
| --- | --- | --- | --- | --- | --- | --- |
| 流体阀 | `susyplusplus:fluid_valve` | 32123 | 流体 | **否** | `SOLID_STEEL_CASING` | `PIPE_IN_OVERLAY` / `PIPE_OUT_OVERLAY` |
| 物流阀 | `susyplusplus:logistics_valve` | 32124 | 物品 | **否** | `SOLID_STEEL_CASING` | `CONVEYOR_OVERLAY`（两面） |
| IR 流体阀 | `susyplusplus:ir_fluid_valve` | 32125 | 流体 | **是**（`Integer.MAX_VALUE`） | `CLEAN_STAINLESS_STEEL_CASING` | `PUMP_OVERLAY` / `PIPE_OUT_OVERLAY` |

共同点：

- **无缓存**（不存流体、不存物品）；
- **无 GUI**、无维护需求；
- 默认开、有红石信号时关；
- 扳手改面（见 §3）；
- 关闭时**仍然与管道相连**，只是通路中断（见 §4）。

---

## 2. 红石逻辑

- 读取方式：`MetaTileEntity#isBlockRedstonePowered()`（任一面的红石信号 > 0 即视为有信号），
  其缓存由 `BlockMachine#neighborChanged` → `updateInputRedstoneSignals()` 刷新。
- `open = !isBlockRedstonePowered()`；状态变化时写 NBT + 发包同步 + `notifyBlockUpdate()`。
- 每 tick（`update()`）与邻居变化（`onNeighborChanged()`）都会重新评估，因此红石信号一变化立刻生效。

---

## 3. 输入面 / 输出面

| 操作 | 效果 |
| --- | --- |
| **扳手右键** | 把**被点击的面**设为**输入面** |
| **Shift + 扳手右键** | 把**被点击的面**设为**输出面** |

- 两个面互斥：如果选中的面正好是另一侧，两者会**自动交换**，绝不会重合。
- 默认：输入面 = 北，输出面 = 南。
- 状态写入 NBT（`ValveInputFace` / `ValveOutputFace` / `ValveOpen`），重启后保留。
- ⚠ 与 GT 原生机器不同：这里扳手**不旋转正面**（源码中机器扳手默认是旋转 `frontFacing`，见
  `MetaTileEntity#onWrenchClick`；本模组按需求覆写为「设输入面」）。

---

## 4. 管道连接：关闭时为什么不「断开」

### 4.1 源码事实

- GT 流体管道连接判定：`BlockFluidPipe#canPipeConnectToBlock` = 邻居
  `getCapability(FLUID_HANDLER, 反向面) != null`；
- GT 物品管道：`BlockItemPipe#canPipeConnectToBlock` = 邻居
  `getCapability(ITEM_HANDLER, 反向面) != null`；
- Susy-Core 的「路由流体管道」（= 无罐流体管道 `BlockTanklessFluidPipe`）判定完全相同：
  `tile.hasCapability(FLUID_HANDLER, 反向面)`。

### 4.2 本整合包的关键配置

`run/config/gregtech/gregtech.cfg` 里 **`B:gt6StylePipesCables=true`**（GT 默认值也是 `true`）。
在该模式下 `BlockPipe#neighborChanged` 与 `ItemBlockPipe` 的「按 capability 自动连/断」被整段跳过，
管道只在**玩家手动扳手**或**放置时**建立连接。

### 4.3 因此本模组的做法

`getCapability(...)` 在输入面 / 输出面**始终返回非 null** 的处理器：

| 状态 | 输入面 | 输出面 |
| --- | --- | --- |
| 开 | 转发型 handler（`fill` / `insertItem` 当场转发） | 惰性 handler（不能写入、不能抽取） |
| 关 | 惰性 handler（拒绝一切） | 惰性 handler（拒绝一切） |

效果：

- 管道看到的 capability 永远存在 ⇒ **连接保持**（也符合你「关闭时仍与管道相连」的要求）；
- 关闭时惰性实现返回 0 / 原堆 ⇒ **流体与物品无法通过**；
- 不需要（也不会）去改相邻管道的 `setConnection`，不会破坏玩家手动连好的管网。

> 兼容性：GT 流体管道（含 tickable 变体）、GT 物品管道、Susy-Core 无罐流体管道都只依赖
> capability 判定，因此三者都能正常工作。

### 4.4 支持非管道方块

输入/输出面同样可以直接贴着**机器、储罐、箱子**等普通方块：

- 输出面：阀门自己调用邻居的 `IFluidHandler#fill` / `IItemHandler#insertItem`，
  与邻居是不是管道无关；
- 输入面：由**上游**（GT 输出仓、管道、机器）把流体/物品推给阀门；
  阀门本身不主动抽取（IR 流体阀除外）。

---

## 5. 无缓存与「不主动抽取」

- 流体阀：输入面 `fill(stack, doFill)` 立刻调用输出面邻居的 `fill(...)`，返回对方接受量；
  自己没有任何 `FluidTank`。输出面暴露的 handler `canFill=false`、`drain` 恒为 `null`。
- 物流阀：输入面 `insertItem(slot, stack, simulate)` 立刻把物品插入输出面邻居的
  `IItemHandler`，返回放不下的剩余量；`getStackInSlot` 恒为 `EMPTY`、`extractItem` 恒为 `EMPTY`。
  输出面是 **0 槽** handler（只用于保持连接）。
- 两者都**不主动抽取**：需要管道或上游机器推送（与「阀门」的语义一致）。

---

## 6. IR 流体阀的主动抽取

参考工业复兴 `TileEntityValvePipeLarge#update()`：

```
available = input.drain(Integer.MAX_VALUE, false)   // 模拟
accepted  = output.fill(available, true)            // 真正写入输出端
if (accepted > 0) input.drain(accepted, true)       // 只扣掉送出去的量
```

- 每 tick 执行一次（关闭时不执行）；
- **不额外限速**：`Integer.MAX_VALUE` 表示「尽可能多地抽」，
  实际速率由**输出端** `fill()` 的返回值决定（这与工业复兴的实现完全一致）；
- 会先把 `available` 复制一份再交给 `fill`，避免某些实现写坏传入的 stack。

---

## 7. 配方（组装机）

| 产物 | 电路 | 材料 | 耗时 / EUt |
| --- | --- | --- | --- |
| 流体阀 | 14 | 钢制机械方块 ×2 + MV 电动泵 + 红石 ×2 | 200 / 30 (LV) |
| 物流阀 | 15 | 钢制机械方块 ×2 + MV 传送带 + 红石 ×2 | 200 / 30 (LV) |
| IR 流体阀 | 16 | 洁净不锈钢机械方块 ×2 + HV 电动泵 ×2 + 红石 ×2 + 流体阀 ×1 | 300 / 120 (MV) |

> 电路编号 12 / 13 已被手推车 / 电动碾磨机占用，这里从 14 起。
> 配方注册名由 GT 自动生成（`assembler` + 输出），不与其它模组冲突。

---

## 8. 本地化键

| 键 | en_us | zh_cn |
| --- | --- | --- |
| `susyplusplus.machine.fluid_valve.name` | Fluid Valve | 流体阀 |
| `susyplusplus.machine.logistics_valve.name` | Logistics Valve | 物流阀 |
| `susyplusplus.machine.ir_fluid_valve.name` | IR Fluid Valve | IR 流体阀 |
| `block.susyplusplus.fluid_valve` | Fluid Valve | 流体阀 |
| `block.susyplusplus.logistics_valve` | Logistics Valve | 物流阀 |
| `block.susyplusplus.ir_fluid_valve` | IR Fluid Valve | IR 流体阀 |
| `susyplusplus.machine.redstone_valve.tooltip.default_open` | Open by default; a redstone signal closes it | 默认导通；有红石信号时关闭 |
| `susyplusplus.machine.redstone_valve.tooltip.no_buffer` | No internal buffer (pure pass-through) | 无内部缓存（仅作为通路） |
| `susyplusplus.machine.redstone_valve.tooltip.faces` | Input: %s / Output: %s | 输入面：%s / 输出面：%s |
| `susyplusplus.machine.redstone_valve.tool_usage.set_input` | Wrench: set the input face | 扳手：设置输入面 |
| `susyplusplus.machine.redstone_valve.tool_usage.set_output` | Shift + Wrench: set the output face | Shift + 扳手：设置输出面 |
| `susyplusplus.direction.<north/south/east/west/up/down>` | — | 北 / 南 / 东 / 西 / 上 / 下 |

---

## 8.1 The One Probe 显示

装了 TOP 时，指向任意阀门会显示三行（[`ValveInfoProvider`](src/main/java/com/susy/plusplus/integration/top/ValveInfoProvider.java:1)）：

| 行 | 内容 | 颜色 |
| --- | --- | --- |
| 1 | `阀门：开启` / `阀门：关闭` | 绿 / 红 |
| 2 | `输入面：<方向>` | 灰 |
| 3 | `输出面：<方向>` | 灰 |

- provider 以 `IProbeInfoProvider` 注册（`SuTopIntegration`，仅在 `theoneprobe` 存在时）；
- 文本用 TOP 的 `{*key*}` 标记交给客户端本地化，方向名复用 `susyplusplus.direction.*`；
- probe 信息由服务端组装，读的是阀门的服务端权威开关状态，因此**红石一变化，TOP 立刻同步显示**。

本地化键：`susyplusplus.top.valve.open` / `closed` / `input` / `output`。

---

## 9. 材质路径

**不新增任何 PNG**，全部复用 GT 现成资源：

| 用途 | 渲染器 |
| --- | --- |
| 流体阀 / 物流阀外壳 | `gregtech.client.renderer.texture.Textures.SOLID_STEEL_CASING` |
| IR 流体阀外壳 | `Textures.CLEAN_STAINLESS_STEEL_CASING` |
| 流体阀面标记 | `Textures.PIPE_IN_OVERLAY` / `Textures.PIPE_OUT_OVERLAY` |
| 物流阀面标记 | `Textures.CONVEYOR_OVERLAY` |
| IR 流体阀输入标记 | `Textures.PUMP_OVERLAY` |

关闭状态下不绘制面标记，因此「开/关」有直观视觉差异。

---

## 10. 验证步骤

1. **编译**：`gradlew.bat build` → `BUILD SUCCESSFUL`。
2. **创造模式**：可在 GT 创造标签页取到 3 个阀门。
3. **扳手改面**：扳手右键任一面 → 该面出现「输入」标记；Shift + 扳手右键另一面 → 出现「输出」标记。
   尝试把同一面设为输入再设为输出 → 两个面自动交换、不会重合。
4. **默认导通**：不接红石时，流体/物品可从输入面流向输出面（例如 GT 输出仓 → 阀门 → 储罐/管道）。
5. **红石关闭**：给阀门任意一面加红石信号 → 面标记消失、传输停止；**管道仍然连接**（不会从管网上掉下来）。
   移除信号 → 立刻恢复传输。装了 TOP 时，瞄准阀门应看到 `阀门：开启 / 关闭` 与输入 / 输出面，
   且随红石信号即时变化。
6. **管道兼容**：
   - GT 流体管道（普通 / tickable）、GT 物品管道、Susy-Core 无罐流体管道分别接到输入/输出面，
     关闭/开启时连接状态都不变，只是流量归零/恢复。
7. **非管道方块**：输出面直接贴 GT 输出仓 / 储罐 / 箱子也能收到流体/物品。
8. **IR 流体阀主动抽取**：输入面贴一个装满流体且**不会自己推送**的容器（例如 GT 量子缸），
   打开阀门后会被抽空并送入输出面容器；速率只受输出端容量限制。
9. **持久化**：设置好输入/输出面后退出重进 / 重启服务器，面配置与开/关状态保留。
10. **配置**：把 `config/susyplusplus.cfg` 的 `enableRedstoneValves` 设为 `false` 并重启 →
    三者不再注册、配方也不再注册，日志打印
    `[SusyPlusPlus] Redstone valves are DISABLED in config.`。
