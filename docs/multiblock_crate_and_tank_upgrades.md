# 多方块板条箱与储罐升级（Multiblock Crate & Tank Upgrades）

> 新增：钢制多方块板条箱、洁净不锈钢 / 加强钛多方块储罐与板条箱、以及对应的物品阀门 / 储罐阀门。
> 代码位置：`com.susy.plusplus.multiblock.storage`
> 开关：`enableMultiblockStorage`（默认 **开**）

---

## 1. 功能说明

| 内容 | 说明 | 容量 |
| --- | --- | --- |
| 钢制多方块板条箱 | 单物品类型的大容量物品存储多方块 | 1,000,000 物品 |
| 洁净不锈钢多方块板条箱 | 同上，外壳升级 | 16,000,000 物品 |
| 加强钛多方块板条箱 | 同上，外壳再升级 | 32,000,000 物品 |
| 洁净不锈钢多方块储罐 | 流体存储多方块（对齐 GT 钢制储罐的行为） | 16,000,000 mB |
| 加强钛多方块储罐 | 同上 | 32,000,000 mB |
| 物品阀门 | 板条箱的取放口，暴露 `IItemHandlerModifiable`，**双向** | — |
| 储罐阀门 | 储罐的取放口，暴露 `IFluidHandler`，**双向** | — |

要点：

- **不需要能量**，**不需要维护仓**（与 GT 原版储罐一致，`hasMaintenanceMechanics() == false`）。
- 板条箱与储罐都通过 <b>能力（Capability）</b> 对外暴露库存：漏斗 / 管道 / AE2 都能直接存取。
- 板条箱的库存是 **"单物品类型 + long 计数"**（详见 §6），管道一次可搬运任意数量（受总量限制）。

---

## 2. 多方块结构图

与 **GT 的钢制多方块储罐完全一致**：固定 **3×3×3** 的空心正方体，控制器在**底层正中央**，
内部 1 格空腔，外壳至少 23 格，阀门最多 2 个。

```text
第 1 层（底层）        第 2 层（中间）        第 3 层（顶层）
  X X X                 X X X                 X X X
  X S X                 X . X                 X X X      S = 控制器（此层正中央）
  X X X                 X X X                 X X X      X = 外壳或阀门
                                                        . = 空气（内部空腔）
```

- `S`：控制器本体（`selfPredicate()`）
- `X`：该档外壳方块，或本档阀门（`setMaxGlobalLimited(2)`）
- `.`：必须是空气

对应的 `FactoryBlockPattern`（与 GT 原版逐字符相同）：

```java
FactoryBlockPattern.start()
        .aisle("XXX", "XXX", "XXX")
        .aisle("XXX", "X X", "XXX")
        .aisle("XXX", "XSX", "XXX")
        .where('S', selfPredicate())
        .where('X', states(casing).setMinGlobalLimited(23)
                .or(metaTileEntities(valve).setMaxGlobalLimited(2)))
        .where(' ', air())
        .build();
```

> ⚠ **钢制多方块储罐不在本模组** —— GT 本体已有 `gregtech:tank.steel`（注册 id **10044**，1,000,000 mB）。
> 本模组只新增"洁净不锈钢 / 加强钛"两档储罐。

---

## 3. 控制器与阀门注册名

| 类型 | 注册名 | MTE id | 本地化键 |
| --- | --- | --- | --- |
| 钢制多方块板条箱 | `susyplusplus:steel_multiblock_crate` | 32110 | `susyplusplus.machine.steel_multiblock_crate.name` |
| 洁净不锈钢多方块储罐 | `susyplusplus:clean_stainless_steel_multiblock_tank` | 32111 | `susyplusplus.machine.clean_stainless_steel_multiblock_tank.name` |
| 洁净不锈钢多方块板条箱 | `susyplusplus:clean_stainless_steel_multiblock_crate` | 32112 | `susyplusplus.machine.clean_stainless_steel_multiblock_crate.name` |
| 加强钛多方块储罐 | `susyplusplus:reinforced_titanium_multiblock_tank` | 32113 | `susyplusplus.machine.reinforced_titanium_multiblock_tank.name` |
| 加强钛多方块板条箱 | `susyplusplus:reinforced_titanium_multiblock_crate` | 32114 | `susyplusplus.machine.reinforced_titanium_multiblock_crate.name` |
| 钢制物品阀门 | `susyplusplus:steel_item_valve` | 32115 | `susyplusplus.machine.steel_item_valve.name` |
| 洁净不锈钢储罐阀门 | `susyplusplus:clean_stainless_steel_tank_valve` | 32116 | `susyplusplus.machine.clean_stainless_steel_tank_valve.name` |
| 洁净不锈钢物品阀门 | `susyplusplus:clean_stainless_steel_item_valve` | 32117 | `susyplusplus.machine.clean_stainless_steel_item_valve.name` |
| 加强钛储罐阀门 | `susyplusplus:reinforced_titanium_tank_valve` | 32118 | `susyplusplus.machine.reinforced_titanium_tank_valve.name` |
| 加强钛物品阀门 | `susyplusplus:reinforced_titanium_item_valve` | 32119 | `susyplusplus.machine.reinforced_titanium_item_valve.name` |

**复用 GT 现成方块（不重复注册）**：

| 类型 | 注册名 | id |
| --- | --- | --- |
| 钢制多方块储罐 | `gregtech:tank.steel` | 10044 |
| 钢制储罐阀门 | `gregtech:tank_valve.steel` | 11524 |

> 本地化键格式与 GT 一致：`<modid>.machine.<path>.name`（GT 的例子：`gregtech.machine.tank.steel.name`）。

---

## 4. 容量对比表

| 档位 | 外壳方块 | 板条箱容量 | 储罐容量 |
| --- | --- | --- | --- |
| 钢制 | `STEEL_SOLID` | 1,000,000 物品 | （用 GT 的 `gregtech:tank.steel`，1,000,000 mB） |
| 洁净不锈钢 | `STAINLESS_CLEAN` | 16,000,000 物品 | 16,000,000 mB |
| 加强钛 | `TITANIUM_STABLE` | 32,000,000 物品 | 32,000,000 mB |

---

## 5. 外壳方块替换规则

| 档位 | GT 外壳枚举 | 显示名 |
| --- | --- | --- |
| 钢制 | `BlockMetalCasing.MetalCasingType.STEEL_SOLID` | Solid Steel Machine Casing |
| 洁净不锈钢 | `BlockMetalCasing.MetalCasingType.STAINLESS_CLEAN` | Clean Stainless Steel Casing |
| 加强钛 | `BlockMetalCasing.MetalCasingType.TITANIUM_STABLE` | Stable Titanium Casing |

> ⚠ **GT 中并不存在"Reinforced Titanium Casing"，也没有"脱氧钢"**（已在源码中确认：
> `MetalCasingType` 枚举只有 `BRONZE_BRICKS / PRIMITIVE_BRICKS / INVAR_HEATPROOF /
> ALUMINIUM_FROSTPROOF / STEEL_SOLID / STAINLESS_CLEAN / TITANIUM_STABLE /
> TUNGSTENSTEEL_ROBUST / COKE_BRICKS / PTFE_INERT_CASING / HSSE_STURDY / PALLADIUM_SUBSTATION`；
> 搜索 `Deoxidized` 结果为 0）。
> 经确认，"加强钛"档使用 GT 的 `TITANIUM_STABLE`；原版钢制储罐实际用的是 `STEEL_SOLID`。

**实现方式**：

- 储罐：**直接继承** GT 的 `MetaTileEntityMultiblockTank`，只覆写 `createStructurePattern()`
  （GT 原类里 `getCasingState()/getValve()` 是 private，无法覆写）、`getBaseTexture(...)`、`createMetaTileEntity(...)`；
  库存 / 界面 / 能力 / tooltip / 无维护全部继承，避免抄错。
- 阀门：储罐阀门**直接继承** GT 的 `MetaTileEntityTankValve`（只覆写贴图与复刻）；
  物品阀门是新类（GT 没有物品阀门），完全对齐储罐阀门的行为。

---

## 6. 物品阀门 / 储罐阀门说明

### 6.1 物品阀门（`MetaTileEntityItemValve`）

- 继承 `MetaTileEntityMultiblockPart`，实现 `IMultiblockAbilityPart<IItemHandlerModifiable>`。
- 能力：**自定义** `SuStorageAbilities.ITEM_VALVE`
  （GT 2.8.x 的 `MultiblockAbility` 只有 `MultiblockAbility(String)` 构造器，
  自定义方式就是 `new MultiblockAbility<>("susyplusplus_item_valve")` + `registerMultiblockAbility(...)`）。
  没有复用 `IMPORT_ITEMS`/`EXPORT_ITEMS`，因为那两个语义是"输入/输出总线"，而阀门要的是"同一份库存的取放口"。
- 接入结构后 `addToMultiBlock` **直接把控制器的库存赋给自己**（不做代理）；脱离结构时换成 0 格空库存占位。
- 阀门本身不存物品（`shouldSerializeInventories() == false`），且**不参与结构拆分共享**（`canPartShare() == false`）。
- 朝下安装时，每 5 tick 自动把物品输出到下方方块（对齐储罐阀门"朝下自动输出"）。
- **没有界面**（`openGUIOnRightClick() == false`）；需要潜行才能用扳手旋转朝向。

### 6.2 储罐阀门（`SuTankValve`）

- 直接继承 GT 的 `MetaTileEntityTankValve`，行为完全一致：
  暴露 `MultiblockAbility.TANK_VALVE`、接入结构后直接引用控制器流体库存、朝下自动输出、
  无 GUI、需要潜行旋转。
- 唯一区别：未接入结构时显示本档外壳贴图（GT 原类只有 `isMetal` 布尔，表达不了洁净不锈钢 / 加强钛）。

### 6.3 板条箱的"单物品类型 + long 计数"

需求容量是 1,000,000 / 16,000,000 / 32,000,000 **物品**。
原版 `ItemStack` 的 count 上限是 **byte（127）**，槽位式需要 15,000+ 个槽位 —— GUI 放不下、也不可能原版同步。
因此采用与 **GT 量子箱**相同的范式（`MetaTileEntityQuantumChest`）：

- 只保存一个"展示用" `ItemStack`（数量恒为 1）+ 一个 `long` 计数器；
- 对外 `getStackInSlot` **始终把数量钳到 64**（保证原版封包合法），自动化通过
  `insertItem`/`extractItem` 一次可搬运任意多；
- `getSlotLimit` 返回 64（而不是百万），以免原版容器产生非法堆；
- **`setStackInSlot` 在服务端是空操作**，并且** GUI 里不提供任何物品槽位** ——
  原版"整槽替换"语义会把百万存量覆盖成 64，这正是 GT 量子箱不用原版槽位同步的原因。
- 物品种类 + 数量**写入主方块 NBT**（键 `StoredItem` / `StoredCount`），
  取放一律通过**物品阀门** / 管道 / 漏斗 / AE2。

**换物品种类需先取空**（与量子箱一致）。

---

## 7. JEI / REI 预览

**自动注册，无需自写插件**：`MetaTileEntities.registerMetaTileEntity(id, mte)` 会一并注册
多方块结构预览（`MultiblockInfoCategory.registerMultiblock`）。本模组的无线能量传输塔已经验证过这条路径。

> 名称与 GT 一致（全部用 GTJEI 的"多方块"分类）。

---

## 8. 本地化键

| 键 | en_us | zh_cn |
| --- | --- | --- |
| `susyplusplus.machine.steel_multiblock_crate.name` | Steel Multiblock Crate | 钢制多方块板条箱 |
| `susyplusplus.machine.clean_stainless_steel_multiblock_tank.name` | Clean Stainless Steel Multiblock Tank | 洁净不锈钢多方块储罐 |
| `susyplusplus.machine.clean_stainless_steel_multiblock_crate.name` | Clean Stainless Steel Multiblock Crate | 洁净不锈钢多方块板条箱 |
| `susyplusplus.machine.reinforced_titanium_multiblock_tank.name` | Reinforced Titanium Multiblock Tank | 加强钛多方块储罐 |
| `susyplusplus.machine.reinforced_titanium_multiblock_crate.name` | Reinforced Titanium Multiblock Crate | 加强钛多方块板条箱 |
| `susyplusplus.machine.steel_item_valve.name` | Steel Item Valve | 钢制物品阀门 |
| `susyplusplus.machine.clean_stainless_steel_tank_valve.name` | Clean Stainless Steel Tank Valve | 洁净不锈钢储罐阀门 |
| `susyplusplus.machine.clean_stainless_steel_item_valve.name` | Clean Stainless Steel Item Valve | 洁净不锈钢物品阀门 |
| `susyplusplus.machine.reinforced_titanium_tank_valve.name` | Reinforced Titanium Tank Valve | 加强钛储罐阀门 |
| `susyplusplus.machine.reinforced_titanium_item_valve.name` | Reinforced Titanium Item Valve | 加强钛物品阀门 |
| `susyplusplus.machine.multiblock_crate.tooltip.single_type` | Single-item-type bulk storage… | 单物品类型大容量存储（换物品种类需先取空） |
| `susyplusplus.machine.multiblock_crate.tooltip.valve` | Needs the matching Item Valve… | 需要对应物品阀门才能与管道 / 漏斗 / AE2 交互 |
| `susyplusplus.machine.item_valve.tooltip` | Access port of a multiblock crate… | 多方块板条箱的取放口（管道 / 漏斗 / AE2 可双向存取） |
| `susyplusplus.machine.item_valve.tooltip.down` | Auto-outputs items downward… | 朝下安装时自动把物品输出到下方 |
| `susyplusplus.gui.crate.capacity` | Capacity: %s items | 容量：%s 物品 |
| `susyplusplus.gui.crate.stored` | Stored: %s | 已存储：%s |

> ⚠ 1.12.2 **只支持 `.lang`**（`assets/susyplusplus/lang/en_us.lang` / `zh_cn.lang`），
> 不支持 `.json`。另外为兼容起见，同时补了 `block.susyplusplus.<path>` 形式的键。

---

## 9. 材质路径（纹理）

**无需新增任何 PNG** —— 全部复用 GT 现成的渲染器（`gregtech.client.renderer.texture.Textures`）：

| 用途 | 渲染器 |
| --- | --- |
| 钢制外壳 | `Textures.SOLID_STEEL_CASING` |
| 洁净不锈钢外壳 | `Textures.CLEAN_STAINLESS_STEEL_CASING` |
| 加强钛外壳 | `Textures.STABLE_TITANIUM_CASING` |
| 板条箱正面覆盖层 | `Textures.QUANTUM_CHEST_OVERLAY` |
| 储罐正面覆盖层 | `Textures.MULTIBLOCK_TANK_OVERLAY`（继承自 GT 储罐） |
| 物品阀门小口 | `Textures.PIPE_IN_OVERLAY` |

这些映射被放在单独的 `@SideOnly(Side.CLIENT)` 类 `SuStorageTextures` 里，
保证服务端永远不会加载 `Textures`（客户端专属类）。

---

## 10. 配方

**照 GT 原有的写法**：工作台有序合成（`ModHandler.addShapedRecipe`，与 GT 自己的
`steel_multiblock_tank` / `steel_tank_valve` / `steel_crate` 同款），
**不使用"上一档控制器 / 阀门"当材料**（无套娃）。
小写字母 `h` / `w` 由 GT 自动补上「硬锤 / 扳手」工具要求，无需自己写键。

| 产物 | 形状 | 材料 |
| --- | --- | --- |
| 洁净不锈钢多方块储罐 | `" R "` / `"hCw"` / `" R "` | R = 不锈钢**环** ×2，C = 洁净不锈钢机械方块 |
| 加强钛多方块储罐 | 同上 | R = 钛**环** ×2，C = 加强钛机械方块 |
| 钢制多方块板条箱 | `" P "` / `"hCw"` / `" P "` | P = 钢**板** ×2，C = 钢制机械方块 |
| 洁净不锈钢多方块板条箱 | 同上 | P = 不锈钢**板** ×2，C = 洁净不锈钢机械方块 |
| 加强钛多方块板条箱 | 同上 | P = 钛**板** ×2，C = 加强钛机械方块 |
| 洁净不锈钢储罐阀门 | `" R "` / `"hCw"` / `" O "` | R = 不锈钢**环**，C = 洁净不锈钢机械方块，O = 不锈钢**转子** |
| 加强钛储罐阀门 | 同上 | R = 钛**环**，C = 加强钛机械方块，O = 钛**转子** |
| 钢制物品阀门 | `" P "` / `"hCw"` / `" O "` | P = 钢**板**，C = 钢制机械方块，O = **LV 传送带** |
| 洁净不锈钢物品阀门 | 同上 | P = 不锈钢**板**，C = 洁净不锈钢机械方块，O = **MV 传送带** |
| 加强钛物品阀门 | 同上 | P = 钛**板**，C = 加强钛机械方块，O = **HV 传送带** |

**互不冲突**（有序配方逐格比对，任意两条在"形状 + 材料"上都不同）：

- 储罐用**环**（照 GT 储罐）、板条箱用**板**（照 GT 箱子）→ 不同；
- 储罐阀门底部是**转子**（照 GT 储罐阀门）、物品阀门底部是**传送带** → 不同；
- 三档材质不同（钢 / 不锈钢 / 钛）→ 不同；
- 也与 GT 自己的 `steel_multiblock_tank`（环 + 钢制外壳）、`steel_tank_valve`（+ 钢转子）、
  `steel_crate`（板 + 长杆）都不同。

> 配方注册名统一带 `susyplusplus_` 前缀，**不覆盖** GT 的任何配方。
> 材质形态已核实：`StainlessSteel` / `Titanium` 的 flags 都含 `EXT2_METAL`（有环）与
> `GENERATE_ROTOR`（有转子），`Steel` 同理。

---

## 11. 验证步骤

1. **编译**：`gradlew.bat build` → `BUILD SUCCESSFUL`（已实测）。
2. **创造模式**：GT 默认创造标签页中可获取 5 个控制器 + 5 个阀门（共 10 件）。
3. **JEI**：搜索上述任一名称，应能看到**多方块结构预览**（3×3×3 的 3 层剖面）。
4. **结构成型**：
   - 摆 3×3×3 外壳（底层正中央放控制器，内部 3×3×1 空腔）；
   - 用本档外壳替换任意 1~2 格外壳放阀门；
   - 右键控制器 → 结构成型（GUI 可打开，阀门会被"吸收"）。
5. **容量**：
   - 钢制板条箱 1,000,000 物品；洁净不锈钢 16,000,000；加强钛 32,000,000；
   - 洁净不锈钢储罐 16,000,000 mB；加强钛储罐 32,000,000 mB。
6. **阀门输入/输出**：
   - 物品阀门：管道/漏斗可**双向**存取；朝下安装时每 5 tick 自动输出到下方；
   - 储罐阀门：管道/流体容器可**双向**存取；朝下安装时自动输出到下方。
7. **自动化**：漏斗可直接从板条箱抽出（受 HTTP/Hopper 速率限制），AE2 存储总线可读写。
8. **内容物处理**：破坏控制器时，**内容物随控制器一起消失**（按需求不掉落）。
9. **持久化**：存入物品/流体后退出重进存档 / 重启服务器，数量仍然保留。
10. **配置**：把 `config/susyplusplus.cfg` 的 `enableMultiblockStorage` 设为 `false` 并重启 →
    上述 10 件不再注册、配方也不再注册，日志会打印
    `[SusyPlusPlus] Multiblock storage upgrades are DISABLED in config.`。
