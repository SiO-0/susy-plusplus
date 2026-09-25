# 流体样品存储（Fluid Sample Storage，MV / HV / EV）

> 新增：MV / HV / EV 三档流体样品存储。
> 代码：[`FluidSamplesStorageMachine.java`](../src/main/java/com/susy/plusplus/machine/FluidSamplesStorageMachine.java)
> 开关：`enableFluidSamplesStorage`（默认 **开**）

---

## 1. 这是什么

对齐 **Susy-Core 的同名机器** `supersymmetry:fluid_samples_storage`（注册 id 18525）：

- **32 个互相独立的储罐**（8×4 网格，每个罐子只能装一种流体）；
- **没有物品槽**，**不处理配方**，**不耗电**；
- 界面里就是 32 个储罐格子，可以直接用容器在格子上点击灌装 / 抽取。

本模组在此基础上提供 **MV / HV / EV** 三档，并按需求放大每格容量。

| 版本 | 注册名 | MTE id | 每格容量 | 总容量 | 基础贴图 |
| --- | --- | --- | --- | --- | --- |
| MV | `susyplusplus:fluid_samples_storage_mv` | 32120 | 32,000 L | 1,024,000 L | `VOLTAGE_CASINGS[MV]` |
| HV | `susyplusplus:fluid_samples_storage_hv` | 32121 | 64,000 L | 2,048,000 L | `VOLTAGE_CASINGS[HV]` |
| EV | `susyplusplus:fluid_samples_storage_ev` | 32122 | 128,000 L | 4,096,000 L | `VOLTAGE_CASINGS[EV]` |

> 对照：Susy-Core 原版只有一档（LV 外观），`TANK_COUNT = 32`、`TANK_CAPACITY = 8000`。

> ⚠ **适配原版 GT（`vanillaGtCompat = true`）时本机器不注册**（配方也不注册）：
> 它的外观与配方都沿用 Susy-Core 的 `fluid_samples_storage`，纯 GT 环境下不保证可用。
> 日志会打印 `[SusyPlusPlus] Fluid Sample Storage machines are DISABLED (vanilla GT compat mode).`

---

## 2. 行为细节

| 项目 | 说明 |
| --- | --- |
| 耗电 | **不耗电**（`MetaTileEntity` 本体没有能源容器） |
| 物品 | 无物品槽（不是箱子） |
| 流体能力 | `IFluidHandler` 对外暴露 **32 个储罐的共享列表**（`FluidTankList`），传进 / 传出都是它 |
| 每罐独立 | `new FluidTankList(false, ...)` → **每个罐子只能装一种流体**，32 个罐子互不影响 |
| `side == null` 查询 | 返回 `null`（与 Susy-Core 原版一致，避免 The One Probe 之类把 32 个罐全列出来） |
| 持久化 | 32 个储罐通过 `FluidTankList#serializeNBT()` 写进本方块 NBT 的 `FluidInventory` 键 |
| 朝向 | 有正面（用于覆盖层），可用扳手旋转、螺丝刀访问覆盖板 |

---

## 3. 配方

照 **原版 `fluid_samples_storage` 的形状**（GroovyScript 原文，见
[`run/groovy/postInit/mod/MachineRecipes.groovy`](../run/groovy/postInit/mod/MachineRecipes.groovy)：

```groovy
RecyclingHelper.addShaped("susy:fluid_samples_storage", metaitem('susy:fluid_samples_storage'), [
    [large_fluid_cell.steel, large_fluid_cell.steel, large_fluid_cell.steel],
    [large_fluid_cell.steel, item('gregtech:boiler_casing', 1), large_fluid_cell.steel],
    [large_fluid_cell.steel, large_fluid_cell.steel, large_fluid_cell.steel]])
```

也就是「**8 个大型流体单元 + 1 个外壳**」。三档保持同样形状，按需求换成铝 / 不锈钢 / 钛的相应物品：

| 产物 | 形状 | 8× 大型流体单元 | 1× 外壳 |
| --- | --- | --- | --- |
| 流体样品存储（MV） | `"CCC"` / `"CHC"` / `"CCC"` | `fluide_cell_large_aluminium`（铝） | MV 机器外壳 |
| 流体样品存储（HV） | 同上 | `fluid_cell_large_stainless_steel`（不锈钢） | HV 机器外壳 |
| 流体样品存储（EV） | 同上 | `fluid_cell_large_titanium`（钛） | EV 机器外壳 |

- 大型流体单元用 GT 的 `MetaItems.FLUID_CELL_LARGE_ALUMINIUM` /
  `FLUID_CELL_LARGE_STAINLESS_STEEL` / `FLUID_CELL_LARGE_TITANIUM`（与 GroovyScript 里
  `large_fluid_cell.<material>` 是同一批物品）；
- 外壳用 `MetaBlocks.MACHINE_CASING` 的 `MV` / `HV` / `EV` 机器外壳；
- 三条配方材质互不相同，且与 Susy-Core 原版（钢）不同 → **不会冲突**；
- 注册名统一 `susyplusplus_fluid_samples_storage_*`，不覆盖任何已有配方。

---

## 4. 材质路径

**不需要新增任何 PNG**：

| 用途 | 来源 |
| --- | --- |
| 基础外壳 | GT 的 `Textures.VOLTAGE_CASINGS[tier]` |
| 正面覆盖层 | GT 自带的 `machines/fluid_samples_storage`
（`assets/gregtech/textures/blocks/machines/fluid_samples_storage/overlay_front.png` 等，**gregtech jar 内已有**） |

---

## 5. 本地化键

| 键 | en_us | zh_cn |
| --- | --- | --- |
| `susyplusplus.machine.fluid_samples_storage_mv.name` | Fluid Sample Storage (MV) | 流体样品存储（MV） |
| `susyplusplus.machine.fluid_samples_storage_hv.name` | Fluid Sample Storage (HV) | 流体样品存储（HV） |
| `susyplusplus.machine.fluid_samples_storage_ev.name` | Fluid Sample Storage (EV) | 流体样品存储（EV） |
| `susyplusplus.machine.fluid_samples_storage.tooltip.tanks` | `%s` independent tanks, `%s` L each | `%s` 个独立储罐，每格 `%s` L |
| `susyplusplus.machine.fluid_samples_storage.tooltip.no_energy` | No energy required | 不耗电 |

（1.12.2 用 `.lang`，不是 `.json`。）

---

## 6. 验证步骤

1. **编译**：`gradlew.bat build` → `BUILD SUCCESSFUL`。
2. **创造模式**：能取到 MV / HV / EV 三件。
3. **tooltip**：显示「32 个独立储罐，每格 32000/64000/128000 L」「不耗电」与总容量。
4. **界面**：右键打开，看到 8×4 = 32 个储罐格子；用桶 / 单元在格子上点击可以灌装、抽取。
5. **每罐独立**：往第 1 格灌水、第 2 格灌岩浆 → 两格互不干扰（不会合并）。
6. **管道**：用流体管道接任意一面，`fill` / `drain` 正常工作。
7. **不耗电**：机器不带能源条，接入电网也不消耗。
8. **持久化**：灌入流体后退出重进存档 / 重启服务器，32 个罐子的内容物都保留。
9. **配置**：把 `enableFluidSamplesStorage` 设为 `false` 并重启 → 三件不再注册、3 条配方也不再注册。
