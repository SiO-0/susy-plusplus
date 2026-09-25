# 手推车（Trolley）

> 搬起 GT 机器 → 零掉落带走 → 原位/他处原样放下（朝向、封面、缓存、进度全保留）。

| 项 | 值 |
| --- | --- |
| 注册名 | `susyplusplus:trolley` |
| 名字键 | `metaitem.trolley.name`（GT `MetaItem` 实际读取的键）<br>`item.susyplusplus.trolley`（同一名字，兼容按注册名写键的习惯） |
| 获取 | 组装机（`ASSEMBLER_RECIPES`）：**12 号编程电路（`circuitMeta(12)`，不消耗）+ 4x 铁板 + 2x 红石 + 2x 玻璃板 + 1x 扳手（`craftingToolWrench`）**，**200 ticks / 30 EU/t** |
| 配置开关 | `enableTrolley`（默认 `true`） |
| 模型 | `assets/susyplusplus/models/item/trolley.json`（`layer0 = susyplusplus:item/trolley`） |
| 材质 | `assets/susyplusplus/textures/item/trolley.png`（16×16） |
| 堆叠 | **不可堆叠**（`setMaxStackSize(1)`），状态存在物品自身 NBT 里 |

---

## 1. 功能说明

| 操作 | 行为 |
| --- | --- |
| **Shift + 右键机器** | 把机器「搬起」装进手推车（**零掉落**），提示「已搬起机器」 |
| **右键** | 把车上的机器放回世界，提示「已放下机器」 |
| 手推车已装载时再 Shift + 右键机器 | 提示「手推车已装载机器」（不改变任何东西） |
| 手推车为空时右键 | **不拦截**，保留 GT 原版「右键开机器 GUI」的习惯 |
| Shift + 右键 GT 多方块控制器 / 多方块部件 | 提示「多方块控制器与多方块部件无法被搬起」 |
| Shift + 右键非 GT 机器方块 | 完全放行（不提示、不拦截，避免影响其它模组的方块） |

搬起/放下**全部在服务端执行**（`TrolleyEventHandler` 在 `world.isRemote` 时直接返回），客户端只负责 tooltip 与提示文本。

---

> **12 号编程电路（不消耗）**：用 GT 的 `RecipeBuilder#circuitMeta(12)` 写入 ——
> 它要求输入里有"12 号编程电路"，但**不消耗**它，作用是把本配方与其它组装机配方彻底区分开。
>
> ⚠ **为什么是铁板而不是钢板（配方冲突修复）**：GT 机器只要求"输入槽里**包含**配方所需物品"，
> 所以如果手推车的输入集合是[配置器](configurator.md)的**真子集**
> （配置器 = 钢板×4 + 红石×2 + 玻璃板×2 + 扳手 **+ LV 电路**），那么**摆齐配置器材料时两个配方都会命中** → 冲突。
> 手推车改用**铁板** + 12 号编程电路后，两边输入集合**互不包含**，冲突彻底消除。
> 本模组三个组装机配方（配置器 / 手推车 / 存储检测器）互相都不是子集关系。

---

## 2. 搬起逻辑（[`TrolleyHelper.pickUp`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyHelper.java)）

1. **取机器**：目标方块必须是 GT 的 `BlockMachine`，再用 `GTUtility.getMetaTileEntity(world, pos)` 取 `MetaTileEntity`；
   为空 → `PASS`（完全不干预）。
2. **拒绝多方块**：`mte instanceof MultiblockControllerBase || mte instanceof IMultiblockPart` → 提示并结束
   （控制器搬走后整个结构必须重新成型；部件（仓室/总线）的 `onRemoval()` 还可能把内容物 flush 出来）。
3. **先序列化，再删方块**（顺序很重要 —— 中途出错也不会丢机器）：

   ```java
   NBTTagCompound beNbt = new NBTTagCompound();
   mte.writeToNBT(beNbt);                                  // 完整状态
   NBTTagCompound stored = new NBTTagCompound();
   stored.setString("RegistryName", mte.metaTileEntityId.toString());
   NBTUtil.writeBlockState(stored, world.getBlockState(pos));
   stored.setTag("BlockEntityNBT", beNbt);
   stored.setString("DisplayKey", mte.getMetaName() + ".name");
   TrolleyData.store(trolley, stored);                     // 写进手推车物品
   ```

4. **GT 语义上的"被移除"清理**：`mte.onRemoval()`（多方块解除注册、网络重建等；已逐个核对 2.8.x 的所有覆写，**都不会生成物品实体**）。
5. **删方块**：`world.setBlockToAir(pos)`。
6. 服务端日志 + 提示「已搬起机器」。

---

## 3. 放下逻辑（[`TrolleyHelper.place`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyHelper.java)）

1. 空车 → `PASS`（放行给 GT）。
2. 读 `RegistryName` + `BlockEntityNBT`；损坏则清空手推车并提示「手推车里的机器数据已损坏，已清空」。
3. 用 **`GregTechAPI.MTE_REGISTRY.getObject(id)`** 取样板 MTE（2.8.x API；`MTERegistry/MTEManager` 是 master 专有，**不用**）。
4. 目标位置 = 被点击面的相邻格，若不可放则退回到被点击格；仍不可放 → 提示「目标位置无法放置机器」。
5. 放置：

   ```java
   IBlockState state = resolveState(trolley, block, sample);   // 存档 BlockState + opaque 覆盖
   world.setBlockState(targetPos, state, 3);
   MetaTileEntity placed = ((IGregTechTileEntity) world.getTileEntity(targetPos))
           .setMetaTileEntity(sample);                          // ⚠ 2.8.x 只有单参重载
   placed.readFromNBT(blockEntityNbt);                          // 朝向来自 NBT 的 FrontFacing
   placed.onPlacement();                                        // ⚠ 2.8.x 是无参版本
   TrolleyData.clear(trolley);
   ```

6. `setBlockState` 之后拿不到 `IGregTechTileEntity`，或 `setMetaTileEntity` 返回 `null` → 回滚成空气 + 提示「机器恢复失败」（物品**不**清空，可重试）。

> **朝向**：因为 NBT 里有 `FrontFacing`，`readFromNBT` 会把它恢复，所以摆回去还是**原来的朝向**（不按玩家朝向重设）。

---

## 4. NBT 结构

```
物品根 NBT
 └─ StoredMachine（compound）
     ├─ RegistryName   : "gregtech:macerator"                 ← metaTileEntityId.toString()
     ├─ BlockState     : NBTUtil.writeBlockState(...)          ← 方块状态
     ├─ BlockEntityNBT : MetaTileEntity#writeToNBT(...) 的完整结果
     │                   （FrontFacing / 喷漆 / 静音 / 封面 / 物品与流体缓存 /
     │                    各 MTETrait：配方进度、充能槽、内部库存 …）
     └─ DisplayKey     : "gregtech.machine.macerator.lv.name"  ← 客户端翻译用
```

- **为什么不存名字字符串**：搬起发生在**服务端**，服务端的 `I18n` 没有语言表（只会返回原始键），所以只存**键**，等客户端画 tooltip 时再翻译。键的构造与 GT 的 `MachineItemBlock` / `MetaItem` 一致（`getMetaName() + ".name"`）。
- **为什么没有坐标**：GT 的 `MetaTileEntity#writeToNBT` 本来就不写 x/y/z 与维度（`MetaId` 是 `MetaTileEntityHolder` 另一层写的），所以放置时**无需清理坐标**。
- **为什么不用 `writeItemStackData`**：2.8.x 基类里它是**空实现**（只有量子箱/剪贴板等少数机器覆写），保存不了整机状态。
- **持久化**：物品 NBT 随玩家存档序列化 → **存档重启后仍在**。

---

## 5. 无掉落处理（关键）

掉落全部发生在 [`BlockMachine#breakBlock`](../somemods/GregTech-master/src/main/java/gregtech/api/block/machines/BlockMachine.java:344) / `#getDrops` 里：

```java
metaTileEntity.clearMachineInventory(inventoryContents);   // 物品/内容物掉落
for (...) Block.spawnAsEntity(worldIn, pos, itemStack);
metaTileEntity.dropAllCovers();                            // 封面掉落
```

**手推车故意不走这两条路**，只用 `world.setBlockToAir(pos)`：

- ❌ 不调 `breakBlock` → 不会 `clearMachineInventory`、不会 `dropAllCovers`、不会 `spawnAsEntity`；
- ❌ 不调 `getDrops` → 不会掉出「带 BlockEntityTag 的机器物品」；
- ✅ 封面不是"丢在地上"，而是**已经在 `BlockEntityNBT` 里**（`CoverSaveHandler.writeCoverNBT`），放下时随 `readFromNBT` 一起恢复；
- ✅ 物品/流体缓存同理（`ImportInventory` / `ExportInventory` / `ImportFluidInventory` / `ExportFluidInventory`），跟着机器一起搬走。

---

## 6. 冲突处理（结论：与 GT 工具无实质冲突）

已核对 GT 2.8.x 源码：

| 工具 | Shift + 右键行为 |
| --- | --- |
| 扳手 | [`onWrenchClick`](../somemods/GregTech-master/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java:612) **只旋转朝向**（`needsSneakToRotate()` 的机器要潜行才转），**不拆机器** |
| 撬棍 | 只拆封面 |
| 软锤 / 硬锤 / 螺丝刀 / 剪线钳 | 切换工作状态 / 静音 / 封面螺丝刀行为 / 无 |

- 拆机器在 GT 里是**左键破坏**（`getHarvestTool()` = `ToolClasses.WRENCH`），与右键无关。
- [`BlockMachine#onBlockActivated`](../somemods/GregTech-master/src/main/java/gregtech/api/block/machines/BlockMachine.java:386) 先用 `item.getItem().getToolClasses(stack)` 分流：**手推车不声明任何工具类**，因此永远走 `MetaTileEntity#onRightClick`，与上述工具互不干扰。
- 要"抢"的只有两条通道，都已处理：
  1. **潜行 + 右键**：本模组事件优先（`PlayerInteractEvent.RightClickBlock` → 搬起成功才 `setCanceled(true)`）；
  2. **非潜行右键**：仅在**车上已装载**时拦截（否则放行，保留 GT 开 GUI）。
- 与**配置器**（同为本模组的 `RightClickBlock` 处理器）互斥：两个处理器开头都有 `if (event.isCanceled()) return;`，主手/副手各拿一件也不会双重触发。

---

## 7. tooltip 与本地化键

tooltip（[`TrolleyBehaviour`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyBehaviour.java)）：

```
空                       ← susyplusplus.tooltip.trolley.empty
已装载：低压研磨机        ← susyplusplus.tooltip.trolley.loaded（%s = 机器名）
shift+右键机器：搬起      ← susyplusplus.tooltip.trolley.pickup
右键：放下               ← susyplusplus.tooltip.trolley.place
```

| 键 | 用途 |
| --- | --- |
| `metaitem.trolley.name` / `item.susyplusplus.trolley` | 物品名（两条都写，内容一致） |
| `susyplusplus.tooltip.trolley.empty` / `.loaded` / `.pickup` / `.place` | tooltip |
| `susyplusplus.message.trolley.picked_up` | 已搬起机器 |
| `susyplusplus.message.trolley.placed` | 已放下机器 |
| `susyplusplus.message.trolley.already_loaded` | 手推车已装载机器 |
| `susyplusplus.message.trolley.multiblock_denied` | 多方块控制器与多方块部件无法被搬起 |
| `susyplusplus.message.trolley.no_space` | 目标位置无法放置机器 |
| `susyplusplus.message.trolley.unknown_machine` | 未知机器：%s |
| `susyplusplus.message.trolley.failed` | 机器恢复失败 |
| `susyplusplus.message.trolley.corrupted` | 手推车里的机器数据已损坏，已清空 |

> 本模组使用 **`.lang`**（`assets/susyplusplus/lang/{en_us,zh_cn}.lang`）：**1.12.2 不支持 `.json` 语言文件**（那是 1.13+ 的格式）。

---

## 8. 实现文件

| 文件 | 作用 |
| --- | --- |
| [`ItemTrolley.java`](../src/main/java/com/susy/plusplus/item/trolley/ItemTrolley.java) | 物品注册（`StandardMetaItem`，不可堆叠，模型路径） |
| [`TrolleyBehaviour.java`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyBehaviour.java) | tooltip |
| [`TrolleyData.java`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyData.java) | NBT 读写（`StoredMachine`） |
| [`TrolleyHelper.java`](../src/main/java/com/susy/plusplus/item/trolley/TrolleyHelper.java) | 搬起 / 放下 |
| [`TrolleyEventHandler.java`](../src/main/java/com/susy/plusplus/event/TrolleyEventHandler.java) | `RightClickBlock` 事件（服务端） |
| `SuMetaItems` / `SuClientEvents` / `SuConfig` / `SuRecipes` | 注册、模型、开关、配方（改动） |
| `models/item/trolley.json` + `lang/{en_us,zh_cn}.lang` | 资源 |

---

## 9. 验证步骤

1. **编译**：`gradlew build` 通过；`gradlew copyModToRunMods` 把 jar 同步进 `run/mods`。
2. **创造模式可获取**：创造模式物品栏（GT 的 "SusyPlusPlus" 页）里找得到手推车；放进物品栏 tooltip 显示「空 / shift+右键机器：搬起 / 右键：放下」。
3. **搬起成功**：Shift + 右键一台单方块机器（如低压研磨机）→ 机器方块消失、聊天栏/状态栏提示「已搬起机器」，tooltip 变为「已装载：<机器名>」。
4. **无掉落**：搬起前先在机器里放几个物品（输入/输出槽）→ 搬起后周围**没有任何物品实体**、**没有机器物品掉落**；封面也不会被弹出来。
5. **重复搬起**：手上已有装载的手推车时再 Shift + 右键别的机器 → 提示「手推车已装载机器」，机器**保持原样**。
6. **放下成功**：右键空地 → 机器出现在被点击面的相邻格，提示「已放下机器」，tooltip 回到「空」。
7. **NBT 恢复**：放下的机器：朝向与搬起前一致、喷漆颜色一致、静音状态一致、**封面还在**、**物品/流体缓存内容还在**、配方进度（若搬起时正在加工）保留。
8. **多方块**：Shift + 右键多方块控制器或仓室 → 红字提示无法搬起，方块不动。
9. **空车右键**：空手推车右键机器 → 正常打开该机器的 GUI（说明没被拦截）。
10. **存档重启**：装载着手推车保存退出 → 重进游戏，tooltip 仍显示「已装载：…」，放下后机器状态完整。
11. **冲突**：手持扳手/撬棍/软锤 Shift + 右键机器的行为与装本模组前**完全一致**（旋转 / 拆封面 / 切换工作状态）。
