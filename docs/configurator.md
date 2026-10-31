# 配置器（Configurator）

> 代码：[`ItemConfigurator.java`](../src/main/java/com/susy/plusplus/item/configurator/ItemConfigurator.java)、
> [`ConfiguratorBehaviour.java`](../src/main/java/com/susy/plusplus/item/configurator/ConfiguratorBehaviour.java)、
> [`ConfiguratorEventHandler.java`](../src/main/java/com/susy/plusplus/event/ConfiguratorEventHandler.java)、
> [`gui/`](../src/main/java/com/susy/plusplus/gui)

---

## 1. 功能说明

| 项 | 值 |
| --- | --- |
| 注册名 | `susyplusplus:configurator` |
| 本地化键 | `metaitem.configurator.name`（item）/ `susyplusplus.gui.configurator.*`（UI） |
| 物品材质 | `assets/susyplusplus/textures/item/configurator.png` |
| 物品模型 | `assets/susyplusplus/models/item/configurator.json`（`layer0 = susyplusplus:item/configurator`） |
| 堆叠 | **不可堆叠**（`setMaxStackSize(1)`） |
| 数据 | 全部写在物品自己的 `tagCompound` 里（不是 ForgeCaps；1.12.2 的 share tag 只同步 `getTagCompound()`） |
| 获取 | 组装机（`ASSEMBLER_RECIPES`）：**1x LV 电路 + 4x 钢板 + 2x 红石 + 2x 玻璃板 + 1x 扳手（`craftingToolWrench`）**，**200 ticks / 30 EU/t** |
| 配置开关 | `enableConfigurator`（默认 `true`） |
| 快捷键 | **Shift + V** 打开主界面（`key.susyplusplus.configurator.open`） |

### 五种模式

| 模式 | 值 | Shift + 右键机器 | 普通右键机器 |
| --- | --- | --- | --- |
| 无 | 0 | — | — |
| 修改机器输出面 | 1 | 打开「输出面」界面（仅 `SimpleMachineMetaTileEntity`） | — |
| 复制机器配置 | 2 | 复制机器配置进配置器（提示「配置已复制」） | 把配置写进机器（提示「配置已粘贴」） |
| 机器工具箱 | 3 | — | 打开「机器工具箱」界面 |
| 容量限制（仅流体） | 4 | — | 打开「流体容量」界面（无流体槽则提示不支持） |
| **给机器重命名** | 5 | — | 打开「重命名机器」界面（输入名称 / 清空名称 / 关闭） |

### 重命名（模式 5）怎么用

1. `Shift+V` → 选「给机器重命名」（模式值 5）；
2. **普通右键**目标机器 → 打开「重命名机器」界面（[`ConfiguratorRenameUI`](src/main/java/com/susy/plusplus/gui/ConfiguratorRenameUI.java:1)）：
   - **输入框**：预填当前名字；
   - **应用**：把输入框内容写入 `CustomName`（留空 = 清除）；
   - **清空名称**：无条件删除 `CustomName`（恢复机器默认名）；
   - **关闭**：不做修改。

实现要点：

- 名字存在 GT 的 `MetaTileEntityHolder#customName`（NBT 键 `CustomName`），由
  [`MachineNameHelper`](src/main/java/com/susy/plusplus/machine/MachineNameHelper.java:1) 反射调用
  `setCustomName(String)`（不直接引用 `MetaTileEntityHolder`，避免强制依赖 AE2）；
- 写入走 [`PacketRenameMachine`](src/main/java/com/susy/plusplus/network/PacketRenameMachine.java:1)
  （客户端发包 → 服务端落盘；带 8 格距离校验），服务端写完回
  [`PacketSyncMachineName`](src/main/java/com/susy/plusplus/network/PacketSyncMachineName.java:1)；
- **中文不乱码**：两个包都按 **UTF-8 字节数组**收发（长度写 `bytes.length`，
  上限为字符上限 ×4）。早期版本写的是 `String.length()`（字符数）却按字节读，
  中文（3 字节/字）因此被截断成乱码；
- **"改了 CustomName 但悬浮名牌不变"的修复**：GT 的
  `MetaTileEntityHolder#updateNameTagParticle()` 只在 `nameTagParticle == null` 时
  **新建**粒子，已有粒子不会随字段变化更新（javap 核实 2.8.7 字节码）。
  客户端收到同步包后由 `MachineNameHelper#applyNameOnClient` 先写空名（让 GT 回收旧粒子），
  再写目标名（重建粒子），因此名字**立刻**可见；
- `ConfiguratorMode#write` 切换模式时**保留 `display` 标签**（否则铁砧名字会被写模式清掉）。

### TOP 显示机器名

装上 The One Probe 后，指向任何**有自定义名字**的 GT 机器会多显示一行
`名称：<名字>`（金色）。provider 是
[`MachineNameInfoProvider`](src/main/java/com/susy/plusplus/integration/top/MachineNameInfoProvider.java:1)，
在 `SuTopIntegration` 里注册（服务端组装，读到的是权威名字）。

---

## 2. UI 结构

三层界面，全部用 **ModularUI**（`com.cleanroommc.modularui`，编译期 3.0.4）：

```
主界面 ConfiguratorMainUI      （Shift+V，5 个模式按钮）
 ├─ 按钮 1 修改机器输出面 ──► MachineFaceUI      （Shift+右键机器，仅单方块机器）
 ├─ 按钮 2 复制机器配置   ──► 无界面，直接写 NBT
 ├─ 按钮 3 机器工具箱     ──► MachineToolboxUI   （普通右键机器）
 ├─ 按钮 4 容量限制       ──► FluidCapacityUI    （普通右键容器）
 └─ 按钮 5 重命名         ──► ConfiguratorRenameUI （普通右键机器）

MachineFaceUI（3 行 × 4 列按钮 + 确认）
MachineToolboxUI（动态按钮）
FluidCapacityUI（可翻页的容量编辑）
```

### 界面为什么由服务端发起

ModularUI 的同步型界面必须由服务端 `GuiManager.open(...)` 打开；而 `Shift+V` 是纯客户端按键。
因此：客户端按键 → 发空包 [`PacketOpenConfigurator`](../src/main/java/com/susy/plusplus/network/PacketOpenConfigurator.java)
→ 服务端校验手里拿着配置器 → 打开界面。
**界面只显示在客户端，但按钮的服务端动作会真正修改 NBT / 机器**，不会出现"只改了本地"的假象。

### 背景与自适应

- 面板背景：`new Rectangle().setColor(0x80101010)` —— **半透明灰**（`SuGuiFactories.COLOR_BACKGROUND`）；
- 窗口自适应：MUI2 的 `ModularScreen#onResize(int,int)` 由框架处理，面板用绝对坐标布局，缩放窗口不会错位；
- esc：主界面按 esc = 关闭界面（主面板默认行为）；子界面按 esc 也是关闭界面；
  「输出面」界面另有一个 **「返回」按钮** 可以回主界面
  （工具箱界面按反馈去掉了底部按钮，用 Shift+V 重新打开即可）。
- 文字：按钮/标题统一走 `SuGuiFactories.buttonLabel/title/text` ——
  **白色粗体（§f§l）**，说明文字浅灰（§7）；背景是深色半透明，默认深灰字看不清。
- 悬停：面板都调用了 `disableHoverBackground()`，光标停在非按钮区域时
  **不会**被主题的悬停底色盖住半透明背景；按钮自己用 `hoverBackground` 做高亮。

### 工厂注册（重要）

MUI2 的工厂按**名字**在两瑞匹配，所以：

- 主界面：`GuiFactories.createSimple("susyplusplus:cfg_main", holder)`（**工厂名必须 ≤ 32 字符**，
  见下）；
- 两个机器界面：自建 [`SuPosGuiFactory`](../src/main/java/com/susy/plusplus/gui/SuPosGuiFactory.java)
  （`AbstractUIFactory<PosGuiData>`，坐标通过 `writeGuiData/readGuiData` 同步），
  因为 GT 的机器 TileEntity **没有**实现 MUI2 的 `IGuiHolder`，不能用 `GuiFactories.tileEntity()`；
- 三者都在 **公共 preInit** 的 `SuGuiFactories.init()` 里构造 → 客户端与服务端各注册一份。

> ⚠ **踩坑记录（会导致 preInit 崩溃）**：ModularUI 的
> `GuiManager#registerFactory` 有硬性限制 ——
> `The factory name length must not exceed 32!`。
> 最初用的 `susyplusplus:configurator_toolbox` 正好 **33** 字符，游戏在 preInit 阶段直接崩：
>
> ```
> java.lang.IllegalArgumentException: The factory name length must not exceed 32!
>     at com.cleanroommc.modularui.factory.GuiManager.registerFactory(GuiManager.java:55)
>     at com.susy.plusplus.gui.SuPosGuiFactory.<init>(SuPosGuiFactory.java:44)
>     at com.susy.plusplus.gui.SuGuiFactories.<clinit>(SuGuiFactories.java:43)
> ```
>
> 现在的名字：`susyplusplus:cfg_main`(21) / `susyplusplus:configurator_faces`(31) /
> `susyplusplus:cfg_toolbox`(24) —— 全部 ≤ 32。
> **以后新增界面工厂时注意别超 32 个字符。**

> ⚠ **踩坑记录（会导致「面配置」界面打不开）**：本工程的 ModularUI 是
> **编译期 3.0.4 / 运行期 3.1.6**（见 `gradle/scripts/dependencies.gradle` 与 `run/mods/`）。
> `PanelSyncManager#syncValue` 的 **2 参重载 `syncValue(String, SyncHandler)` 只存在于 3.0.4**，
> 3.1.6 只保留了 3 参重载 `syncValue(String, int, SyncHandler)`：
>
> ```
> java.lang.NoSuchMethodError: 'com.cleanroommc.modularui.value.sync.PanelSyncManager
>   com.cleanroommc.modularui.value.sync.PanelSyncManager.syncValue(java.lang.String,
>   com.cleanroommc.modularui.value.sync.SyncHandler)'
>     at com.susy.plusplus.gui.MachineFaceUI.buildUI(MachineFaceUI.java:130)
> ```
>
> 现在「输出面」界面**完全不用同步值**，改用
> [`InteractionSyncHandler`](../src/main/java/com/susy/plusplus/gui/MachineFaceUI.java) 的固有语义：
> 它在按下时**先在本地执行一次回调，再把点击同步到服务端执行一次** ——
> 两端的 `pendingMask` 因同一确定性逻辑而保持一致（客户端即时刷新标签，服务端拿到权威掩码用于「确认」）。
> 副作用（改机器、开上级界面）用 `data.getPlayer() instanceof EntityPlayerMP` 判断，
> **只在服务端执行**；否则客户端会改到本地假 TE，且强转 `EntityPlayerMP` 会
> `ClassCastException`（客户端玩家是 `EntityPlayerSP`）。
>
> **结论：写 MUI 代码时优先用 `Widget#syncHandler(InteractionSyncHandler)` + `instanceof` 判断侧别，
> 不要依赖 `syncValue` 这类跨版本可能变更的重载。**

---

## 3. 按钮布局（修改机器输出面）

```
第一行： X  A  X  X
第二行： B  C  D  E
第三行： X  G  X  X
                [确认]
```

| 按钮 | 含义 | 取值 |
| --- | --- | --- |
| **C** | 机器主面（`MetaTileEntity#getFrontFacing()`），**可写** | 面 |
| **A** | 上面 | `EnumFacing.UP` |
| **G** | 下面 | `EnumFacing.DOWN` |
| **E** | 后面 | 主面的对面 |
| **B** | 左侧 | 主面 `rotateYCCW()`（约定，仅影响按钮标注） |
| **D** | 右侧 | 主面 `rotateY()` |
| X | 空气，仅用于说明位置，**不添加按钮** | — |

每个面点按循环 4 态：**无配置 → 流体自动输出 → 物品自动输出 → 流体和物品自动输出 → 无**。

> `X` 不需要按钮；`C` 与其它面一样可点（需求里的"以机器主面为准"通过
> "相对面映射"体现：C/E/B/D 由主面推导，A/G 固定为上下面）。

---

## 4. NBT 结构

写在物品 `tagCompound` 里：

```
{
  "Mode": 0|1|2|3|4|5,             // 当前模式（切换模式会清空旧 tag，只留 Mode + 铁砧改名 display）
  "CopiedConfig": {                // 仅复制过配置后存在
     "HasFront": true, "Front": 2,
     "HasPainting": true, "Painting": 1234567,
     "HasMuffled": true, "Muffled": false,
     "HasTransformer": false, "Inverted": false,
     "HasOutput": true,
     "OutItems": 3, "OutFluids": 3,
     "AutoItems": true, "AutoFluids": false,
     "AllowItems": false, "AllowFluids": false
  }
}
```

- **切换模式时清空旧 NBT**：`ConfiguratorMode#write` 用新的 `NBTTagCompound` 重写，只保留 `Mode`
  与**铁砧改名产生的 `display` 标签**（保留 `display` 是为了让模式 5 能读到名字）；
- tooltip 会显示：当前模式 / 该模式的操作提示 / 是否已存有配置。

---

## 5. 机器面配置的读写（源码依据）

GT 本体的机器面模型（**编译用 jar `2.8.7-beta` 已用 javap 逐条核实**）：

| 用途 | API |
| --- | --- |
| 输出面（物品/流体各一个） | `SimpleMachineMetaTileEntity#getOutputFacingItems/Fluids`、`setOutputFacingItems/Fluids(EnumFacing)` |
| 自动输出开关 | `isAutoOutputItems/Fluids()`、`setAutoOutputItems/Fluids(boolean)` |
| 输出口是否允许输入 | `isAllowInputFromOutputSideItems/Fluids()`、`setAllowInputFromOutputSideItems/Fluids(boolean)` |
| 能力接口 | `GregtechTileCapabilities.CAPABILITY_ACTIVE_OUTPUT_SIDE` → `IActiveOutputSide`（**只有 4 个 getter**） |
| NBT 键（GT 自己的） | `OutputFacing`、`OutputFacingF`、`AutoOutputItems`、`AutoOutputFluids`、`AllowInputFromOutputSide`、`AllowInputFromOutputSideF` |

结论与实现：

- 能"改面"的 setter 只存在于 **`SimpleMachineMetaTileEntity`**（量子箱/量子缸另有 `setAllowInputFromOutputSide(boolean)`）→
  本模组**只支持普通单方块机器**，其它机器提示「该机器不支持修改输出面」；
- 界面里按 12 位掩码累积（6 面 × 2 位：物品 / 流体），**点「确认」才写机器**；
- 写机器时**全部走公开 setter**（不直接塞 NBT）→ GT 的 `notifyBlockUpdate()` 与
  `writeCustomData(UPDATE_OUTPUT_FACING / UPDATE_AUTO_OUTPUT_ITEMS / UPDATE_AUTO_OUTPUT_FLUIDS)` 会照常执行，
  客户端渲染与行为都同步；
- GT 只有"一个物品输出面 + 一个流体输出面"，因此把某面设为含"物品"的态时，
  **其它面的物品位会被自动清掉**（流体同理）—— 这就是需求里"最多只存在一个物品输出和一个流体输出"。

---

## 6. 复制 / 粘贴机器配置

`MachineConfig` 采集并回写以下内容（每类都有 `has*` 标记，机器不支持则跳过该类）：

| 项目 | API |
| --- | --- |
| 正面朝向 | `MetaTileEntity#hasFrontFacing/getFrontFacing/isValidFrontFacing/setFrontFacing` |
| 喷漆颜色 | `isPainted/getPaintingColor/setPaintingColor(int)` |
| 音效（静音） | `isMuffled/toggleMuffled()`（GT 只有开关，没有 setter：状态不同才 toggle 一次） |
| 变压器升/降压 | `MetaTileEntityTransformer#isInverted/setTransformUp(boolean)` |
| 物品/流体输出面 + 自动输出 + 输出口输入限制 | 见上一节的 `SimpleMachineMetaTileEntity` API |

**刻意不复制**：方块 id、坐标、能量缓存、物品/流体缓存、机器内部库存
（`ChargerInventory`、`CircuitInventory` 等）、封面（cover）。

提示：Shift+右键 = **「配置已复制」**；普通右键 = **「配置已粘贴」**（按你的要求用"粘贴"而不是"成功复制"）。

---

## 7. 机器工具箱

按钮**按这台机器支持的项动态生成** —— 不支持的项**根本不创建按钮**（不会留下空位，
面板高度也会随按钮数量收缩）；所有按钮**按下即生效**，所以界面里**没有确认按钮**：

| 按钮 | API |
| --- | --- |
| 音效 | `MetaTileEntity#isMuffled/toggleMuffled()`（`toggleMuffled` 为 `public final`） |
| 机器开关 | `CAPABILITY_CONTROLLABLE` → `IControllable#isWorkingEnabled/setWorkingEnabled`（⚠ 只有实现了该能力的机器，如普通单方块机器与部分配方多方块） |
| 输出口允许输入 | `SimpleMachineMetaTileEntity#setAllowInputFromOutputSideItems/Fluids` |
| 变压器模式 | `MetaTileEntityTransformer#setTransformUp(!isInverted())` |
| 修复维护问题 | `CAPABILITY_MAINTENANCE` → `IMaintenance#setMaintenanceFixed(int)`（对 0~5 全调一次 = 修好全部问题） |

> ⚠ 关于「维护仓进行维护」：`IMaintenanceHatch`（`gregtech.api.capability`）**没有**修复方法，
> 修复问题在**多方块控制器**侧的 `IMaintenance`（`gregtech.api.metatileentity.multiblock`）。
> 因此本模组只做一件事：**控制器一键修好全部维护问题**
> （`CAPABILITY_MAINTENANCE` 只有多方块控制器才有；单方块机器不满足该能力 → 按钮自动不出现）。
>
> ⚠ **已删除「维护仓贴胶带」按钮**：`IMaintenanceHatch#setTaped(boolean)` 只是一次性贴住，
> 该接口<b>没有任何读取"是否已贴"的方法</b> —— 界面既无法反映状态也无法撤销，
> 按需求不再提供。

---

## 8. 本地化键

| 键 | 用途 |
| --- | --- |
| `metaitem.configurator.name` | 物品名 |
| `key.categories.susyplusplus`、`key.susyplusplus.configurator.open` | 按键分类/名称 |
| `susyplusplus.tooltip.configurator.*` | tooltip（模式、提示、是否已复制） |
| `susyplusplus.configurator.mode.*` | 模式名（`none` / `modify_output` / `copy_config` / `machine_toolbox` / `fluid_capacity` / `rename`） |
| `susyplusplus.message.configurator.renamed` / `renamed_cleared` / `rename_failed` | 重命名模式的反馈消息 |
| `susyplusplus.configurator.face.*` | 面状态（`none` / `fluid` / `item` / `both`） |
| `susyplusplus.gui.configurator.*` | 三个界面的标题、提示、按钮、状态文本 |
| `susyplusplus.message.configurator.*` | 聊天栏动作反馈（已复制 / 已粘贴 / 不支持 …） |

> 本模组使用 **`.lang`**（`assets/susyplusplus/lang/{en_us,zh_cn}.lang`），与仓库既有资源一致。

---

## 9. 材质 / 模型路径

| 资源 | 路径 |
| --- | --- |
| 物品材质 | `src/main/resources/assets/susyplusplus/textures/item/configurator.png` |
| 物品模型 | `src/main/resources/assets/susyplusplus/models/item/configurator.json` |
| 模型 layer0 | `susyplusplus:item/configurator` |
| 界面背景 | 代码绘制（`Rectangle` 半透明灰），**不需要** GUI 贴图 |

> GT 的 `createItemModelPath` 返回值**不带** `item/` 前缀（和电池盒同样的坑），
> 所以 `models/item/configurator.json` 里的 `layer0` 必须显式写 `susyplusplus:item/configurator`。

---

## 10. 验证步骤

1. **编译**：`gradlew.bat build` → `BUILD SUCCESSFUL`；
2. **创造模式**：物品栏里能找到「配置器」（模型/材质正常，不是紫黑格）；
3. **Shift+V**：手持配置器按 Shift+V → 打开半透明灰背景的主界面（5 个模式按钮）；
4. **写入 NBT**：点「修改机器输出面」→ 界面关闭 + 聊天栏提示「配置器模式：修改机器输出面」；
   再 Shift+V 打开，应看到该按钮为高亮（已选中）；
5. **切换清空**：从模式 2 切到模式 3 后，`CopiedConfig` 应被清空（tooltip 不再显示「已复制机器配置」）；
6. **输出面界面**：模式 1 下 Shift+右键普通单方块机器 → 3 行 4 列按钮位置正确（A 在第二列首行、
   C 在第二列中行、G 在第二列末行）；点某面循环 4 态；点「确认」后机器输出面/自动输出立即变化
   （可用扳手/覆盖板或观察物品自动推出验证）；
7. **复制/粘贴**：模式 2 下 Shift+右键机器 → 「配置已复制」；普通右键另一台同类机器 → 「配置已粘贴」，
   新机器的正面朝向/输出面/自动输出等与源机器一致；
8. **工具箱**：模式 3 下普通右键机器 → 界面按机器支持项**动态**显示按钮（最多 5 个）；
   音效/开关机/输出口允许输入/变压器模式/修复维护逐个点击并观察机器状态变化；
   不支持的项**不会创建按钮**（面板高度随按钮数收缩，不留空位）；
9. **tooltip**：任意时刻悬停配置器，应显示当前模式（未见过的机器则是「当前模式：无」）；
10. **重命名**：铁砧里把配置器改成「主熔炉」→ 模式 5 → 普通右键机器：
    聊天栏提示「机器已重命名为：主熔炉」，机器**头顶立即出现**该名字；破坏机器后掉落的物品名也带这个名字；
    再用一个**未改名**的配置器（模式 5）右键 → 名字被清除、恢复默认；
11. **持久化**：退出存档再进，配置器的模式与已复制配置仍在（NBT 写在物品上）；
    机器名字同样保留（存在方块实体 NBT 的 `CustomName`）。
