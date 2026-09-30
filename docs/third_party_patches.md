# 第三方 mod 兼容补丁（XNet 汉化 / HoloInventory 机器名）

> 新增：针对两个**第三方** mod 的可选运行时补丁。
> 总开关：[`SuConfig.enableThirdPartyPatches`](../src/main/java/com/susy/plusplus/config/SuConfig.java)（默认**开**）
> 代码：[`mixin/xnet`](../src/main/java/com/susy/plusplus/mixin/xnet)、[`mixin/mcjtylib`](../src/main/java/com/susy/plusplus/mixin/mcjtylib)、[`mixin/holoinventory`](../src/main/java/com/susy/plusplus/mixin/holoinventory)

---

## 0. 基本原则：**没装就完全不执行**

两处保护，缺一不可：

| 保护 | 作用 |
| --- | --- |
| `@Pseudo`（Mixin 注解） | 目标类在**编译期**不存在也不报错；运行期目标缺失时该 mixin **直接丢弃** |
| [`SuMixinPlugin`](../src/main/java/com/susy/plusplus/mixin/SuMixinPlugin.java)（`IMixinConfigPlugin`） | `shouldApplyMixin()` 里检查 `Loader.isModLoaded("xnet")` / `("holoinventory")`，**没装就返回 false**，连解析都不做 |

> 我们的 `mixins.susyplusplus.json` 是 `"required": true`：如果没有这两个保护，
> 在没装 XNet / HoloInventory 的整合包里会因为"required mixin 未能应用"而**崩游戏**。
> 现在则是：装了才补、没装则完全不碰。

---

## 1. XNet 汉化（①②）

### 1.1 问题（实测证据）

`xnet-1.12-1.8.4-ynet.jar`（SUSY 用的 XNet 分支）：

1. **没有 `zh_cn.lang`** —— 它的 `assets/xnet/lang/` 里只有 `de_de / ru_ru / tr_tr / en_us`，
   所以中文环境下所有方块名 / 物品名 / Shift 提示都是英文。
2. **部分界面文字是硬编码**（根本不走 I18n，补语言文件也没用）。从 jar 的常量池实测确认：
   - `mcjty/xnet/blocks/controller/gui/GuiController.class`：
     `"The block is now highlighted"`、`"Copied channel"`、`"Really remove channel "`
   - `mcjty/xnet/blocks/redstoneproxy/RedstoneProxyBlock.class`、
     `RedstoneProxyUBlock.class`：`"Acts as a proxy block for"`、
     `"redstone. XNet can connect to this"`、`"This version does/no block update!"`
   - `mcjty/xnet/blocks/controller/TileEntityController.class`：
     `"Error copying connector!"`、`"Channel does not support this!"`、`"Invalid connector json!"`、
     `"Wrong channel type!"`、`"Advanced connector is needed!"`、`"Error pasting clipboard data!"`、
     `"Invalid channel json!"`、`"Not everything could be pasted!"`
3. **频道类型下拉框显示的是原始键**（例如 `xnet.item`）：
   `GuiController` 建下拉框时直接 `type.addChoices(channelType.getID())`（源码里还留着注释
   `// Show names?`），而 `IChannelType#getID()` 就是 `"xnet.item"` / `"xnet.energy"` /
   `"xnet.fluid"` / `"xnet.logic"`（javap 常量池实测）。
   更关键的是：**mcjtylib 的 GUI 控件从不调用 I18n** —— 全量扫描
   `mcjtylib-refilmed-3.5.5.jar` 后，整个库里只有 `BaseBlock` 引用过 I18n，
   控件（`AbstractLabel` / `ChoiceLabel`）是把私有字段 `text` 原样丢给 `FontRenderer` 的。
   所以光往语言文件里补 `xnet.item=物品` **不会**生效，必须额外补一刀 Mixin。

### 1.2 ① 语言文件（零风险，纯资源）

本模组在自己的 jar 里补一份 [`assets/xnet/lang/zh_cn.lang`](../src/main/resources/assets/xnet/lang/zh_cn.lang)：

- 键名、`%s` 占位符、`@f`/`@6` 颜色码、`\n` 换行**全部照抄** upstream 的 `en_us.lang`（含首行 `#PARSE_ESCAPES`）；
- 因为 XNet 自己**没有** zh_cn，不存在"文件级覆盖"冲突（游戏只在找不到 zh_cn 时才退回 en_us）；
- **不受** `enableThirdPartyPatches` 影响：即使把总开关关掉，中文语言文件依然生效。

### 1.3 ② 硬编码文字 → 可本地化键（Mixin）

手法：`@ModifyConstant(method = "*", constant = @Constant(stringValue = "原文"))`
—— **直接替换字符串常量**，不依赖任何方法名，因此：

- 不需要 refmap（方法名不参与匹配）；
- XNet 小版本更新导致方法/lambda 改名也不影响；
- 找不到时 Mixin 默认只记一条日志（`require` 默认不强制），不会崩。

| 补丁类 | 目标 | 生效侧 |
| --- | --- | --- |
| `xnet.GuiControllerMixin` | `mcjty.xnet.blocks.controller.gui.GuiController`（控制器界面：按钮 / 标签 / tooltip / 剪贴板与错误提示，约 30 处） | 客户端 |
| `xnet.AbstractEditorPanelMixin` | `…controller.gui.AbstractEditorPanel`（**仅**红石模式那四条 tooltip；选项名同时是序列化取值，绝不能替换） | 客户端 |
| `xnet.GuiConnectorMixin` | `mcjty.xnet.blocks.cables.GuiConnector`（`Name:` / `Directions:` 与提示） | 客户端 |
| `xnet.GuiRouterMixin` | `mcjty.xnet.blocks.router.GuiRouter`（`Ch` / `Pos` / `Index` 表头） | 客户端 |
| `xnet.ItemConnectorSettingsMixin` | `…apiimpl.items.ItemConnectorSettings`（物品连接器标签与 tooltip） | 客户端 |
| `xnet.ItemChannelSettingsMixin` | `…apiimpl.items.ItemChannelSettings` | 客户端 |
| `xnet.FluidConnectorSettingsMixin` | `…apiimpl.fluids.FluidConnectorSettings` | 客户端 |
| `xnet.FluidChannelSettingsMixin` | `…apiimpl.fluids.FluidChannelSettings` | 客户端 |
| `xnet.EnergyConnectorSettingsMixin` | `…apiimpl.energy.EnergyConnectorSettings` | 客户端 |
| `xnet.LogicConnectorSettingsMixin` | `…apiimpl.logic.LogicConnectorSettings` | 客户端 |
| `xnet.AbstractConnectorSettingsMixin` | `…api.helper.AbstractConnectorSettings`（侧面 / 颜色 tooltip） | 客户端 |
| `xnet.RedstoneProxyBlockMixin` | `…blocks.redstoneproxy.RedstoneProxyBlock` | 客户端 |
| `xnet.RedstoneProxyUBlockMixin` | `…blocks.redstoneproxy.RedstoneProxyUBlock` | 客户端 |
| `xnet.TileEntityControllerMixin` | `mcjty.xnet.blocks.controller.TileEntityController` | 通用（**服务端**发错误包前翻译，用服务端可用的 `net.minecraft.util.text.translation.I18n`） |

> 上面这批"客户端"补丁共用一个助手
> [`XNetPatchText`](../src/main/java/com/susy/plusplus/integration/xnet/XNetPatchText.java)
> （内部用 `net.minecraft.client.resources.I18n`）；它们只改**显示文本**，
> 因此都放在 `"client"` 列表里。
>
> ⚠ 这个助手**必须放在 `com.susy.plusplus.mixin` 之外**（现在在 `integration.xnet`）：
> Mixin 会把配置里的 `"package"` 整个加入**类加载器排除名单**，该包下的"非 mixin 类"
> 普通类加载器根本找不到。踩过的坑：`@ModifyConstant` 的替换代码是被**合并进目标类**
> （XNet 的 `GuiController`）执行的，运行时解析引用直接
> `NoClassDefFoundError: com/susy/plusplus/mixin/xnet/XNetPatchText`，一开 XNet GUI 就崩。
> 老补丁之所以没这个问题，是因为 `localize` 直接写在 mixin 类里、会被一起合并进目标类。
>
> **刻意不去动**的字符串：GUI 命令标签 / NBT 键 / JSON 字段
> （`"channel"`、`"type"`、`"name"`、`"enabled"`、`"mode"`、`"priority"`、`"count"`、
> `"speed"`、`"flt"`、`"od"`、`"meta"`、`"nbt"`、`"stack"`、`"extract"`、`"block"`、
> `"color0"`…）以及纯符号按钮（`"x"`、`"C"`、`">"`、`"<"`、`"?"`）——
> 翻译它们会破坏存档 / GUI 同步。
>
> 中文译文统一写成**单行**，用于拼接的片段也按原样保留拼接顺序，例如
> `"Fluid extraction rate|(max "` + 速率 + `"mb)"` → `流体提取速率（最大` + 速率 + `mb）`。

中文文本放在本模组自己的语言文件里（键 `susyplusplus.patch.xnet.*`）——
I18n 是**全局键表**，跨命名空间也能取到，不必往 `assets/xnet/lang/` 里塞额外键。

> 颜色（如红石代理最后一行的 `TextFormatting.YELLOW`）**保持原样**，只替换文本。

### 1.4 补充：控制器 GUI 的"频道类型"下拉框 → 中文

实测症状：控制器 GUI 里新建频道时，类型下拉框显示的是 `xnet.item` 这种原始键（见 1.1 第 3 条）。
两处一起改才有效：

1. 在 [`assets/xnet/lang/zh_cn.lang`](../src/main/resources/assets/xnet/lang/zh_cn.lang) 里补键：
   `xnet.item=物品`、`xnet.energy=能量`、`xnet.fluid=流体`、`xnet.logic=逻辑`；
2. 新增 [`McjtyAbstractLabelI18nMixin`](../src/main/java/com/susy/plusplus/mixin/mcjtylib/McjtyAbstractLabelI18nMixin.java)：
   在 `mcjty.lib.gui.widgets.AbstractLabel#setText(String)` 的**入参**上做
   `@ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true)`，
   **只当字符串以 `xnet.` 开头时**先 `I18n.format` 再写入显示字段。

为什么这样最稳：

- **只改"显示"，不动"取值"**：`ChoiceLabel` 的取值来自它自己的 `currentChoice` 字段，
  点 "Create" 时 `createChannel(type.getCurrentChoice())` 拿到的仍是原始 ID，
  服务端 `XNetApi#findType(id)` 照常工作 —— 不会出现"汉化了却建不出频道"；
- **作用域极窄**：非 `xnet.` 前缀的文本一律原样返回，RFTools 等其它 mcjty 系 mod 完全不受影响；
- 该补丁放在 `"client"` 列表，并由 `SuMixinPlugin` 按 `Loader.isModLoaded("xnet")` 门控
  （mcjtylib 是 XNet 的前置库，没有 XNet 就没有意义）。

> 仍然未汉化的部分（现在只剩 mcjtylib 的 `.gui` 布局文件里的**字面量**）：
> `Local Channels`、`Remote Channels`、`Edit channel 1..8`、`Make public`、
> `If selected this channel` / `will be publicly available` 等。
> 它们是**布局文件的内容**而不是 Java 常量，既没有 key、也不经过 I18n，
> `@ModifyConstant` 根本够不着；唯一办法是整份覆盖 `assets/xnet/gui/*.gui`，
> 但那会让英文用户也看到中文，因此**有意不做**。

### 1.5 补充：运行时拼出来的选项名（`Ins` / `Ext` / `Priority` …）

上一版做完后仍然漏了连接器设置里的"模式"按钮：它显示 `Ins`/`Ext`，但**字节码里根本没有这两个字符串**。
原因在 XNet 自己这里：

```java
// mcjty/xnet/blocks/controller/gui/AbstractEditorPanel.java:259
public <T extends Enum<T>> IEditorGui choices(String tag, String tooltip, T current, T... values) {
    String[] strings = new String[values.length];
    int i = 0;
    for (T s : values) {
        strings[i++] = StringUtils.capitalize(s.toString().toLowerCase());   // INS -> "Ins"
    }
    return choices(tag, tooltip, StringUtils.capitalize(current.toString().toLowerCase()), strings);
}
```

这些显示名是**运行时拼出来的**，`@ModifyConstant` 无从下手；而直接替换枚举名/选项值又会破坏序列化
（服务端 `ItemMode.valueOf` / `RSMode.valueOf` 会抛异常）。

所以改为在**显示层**兜底：把
[`XNetPatchText#localizeLabel`](../src/main/java/com/susy/plusplus/integration/xnet/XNetPatchText.java)
挂到 `AbstractLabel#setText` 上，用一张「显示文本 → 语言键」**严格白名单**翻译：

| 来源 | 显示文本 | 中文 |
| --- | --- | --- |
| `ItemMode` / `FluidMode` / `EnergyMode` | `Ins` / `Ext` | 输入 / 提取 |
| `StackMode` | `Single` / `Stack` / `Count` | 单个 / 一组 / 指定数量 |
| `ExtractMode` | `First` / `Rnd` / `Order` | 最先可用 / 随机槽位 / 按顺序 |
| `ItemChannelSettings.ChannelMode` | `Priority` / `Roundrobin` | 优先级 / 轮询 |
| `FluidChannelSettings.ChannelMode` | `Priority` / `Distribute` | 优先级 / 平均分配 |
| `LogicMode` | `Sensor` / `Output` | 传感器 / 输出 |
| `SensorMode` | `Item` / `Fluid` / `Energy` / `Rs` | 物品 / 流体 / 能量 / 红石 |
| 红石模式（`redstoneMode`） | `Ignored` / `Off` / `On` / `Pulse` | 忽略 / 关 / 开 / 脉冲 |

为什么这样安全：

- **只改显示、不改取值**：`ChoiceLabel` 的取值在它自己的 `currentChoice` 字段里，
  点击时 `addChoiceEvent` 回调拿到的仍是原始英文（`"Ins"`、`"Ignored"`…），
  服务端 `valueOf(...)` 照常工作 —— 不会"汉化了但设置存不上/报错"；
- 白名单是**精确匹配**，不在表里的文本（数字、机器名、玩家输入）原样返回；
- 钩子只挂在 `AbstractLabel` 上，`TextField extends AbstractWidget`
  （输入框不继承它），所以**编辑中的文本不会被误翻**。

---

## 2. HoloInventory 显示 GT 机器名（③）

### 2.1 问题（实测证据）

HoloInventory 的 `api.INamedItemHandler`（只有一个 `String getItemHandlerName()`）
在整个 jar 里**只被 `net.dries007.holoInventory.network.request.TileRequest$Handler` 引用**，
服务端流程是：

1. `world.getTileEntity(pos)`
2. 不是箱子 / 末影箱 / 唱片机 → `te.hasCapability(ITEM_HANDLER, side)` 取 **`IItemHandler`**
3. **`if (iih instanceof INamedItemHandler) name = getItemHandlerName()`**
4. `new PlainInventory(pos, name, iih)` 回包给客户端渲染

也就是说：名字来自"**物品库存对象**"是否实现了它的接口。
GT 2.8.x 早于这个 API，返回的是自己的一堆 `IItemHandler` 实现（每种机器还可能不同），
都不含该接口 → **全息板拿不到名字**（对准 GT 机器时空白）。

> 为什么不去"让 GT 的库存实现该接口"：那些库存分散在
> `NotifiableItemStackHandler` / `GTItemStackHandler` / `ItemHandlerList` / 各机器私有类里，补不全；
> 而"包一层 wrapper"会污染 GT 所有管道/自动化的能力查询，风险更大。

### 2.2 做法

[`HoloInventoryTileRequestMixin`](../src/main/java/com/susy/plusplus/mixin/holoinventory/HoloInventoryTileRequestMixin.java)：
在该处理器 `onMessage(...)` 的 **HEAD** 拦下：

1. 从 `MessageContext` 拿玩家（服务端）→ `getServerWorld()` → `getTileEntity(pos)`；
2. 用 **`IGregTechTileEntity`**（GT 自己的接口，避免引用 `MetaTileEntityHolder` ——
   它通过 `@Optional.Interface` 实现了 AE2 的 `IActionHost`，编译期会要求 AE2 类）判断是否 GT 机器；
3. 取 `ITEM_HANDLER` 能力；**没有物品槽的机器直接放行**（保持原行为：没有全息信息）；
4. 名字取 `mte.getMetaFullName()` 的服务端翻译（`I18n.translateToLocal`，键格式与 GT 一致，
   例如 `susyplusplus.machine.steel_multiblock_crate.name`）；
5. 用**反射**构造 `PlainInventory(pos, name, handler)` 并 `setReturnValue(...)`。

⚠ 反射的原因：本模组**不把 HoloInventory 写进编译期依赖**（别人不一定装），
所以补丁只依赖 Forge 的 `IMessage` / `MessageContext`。
任何一步失败只打一条 WARN 并走回原逻辑，不影响游戏。

### 2.3 踩坑记录：为什么第一版补丁修好后仍然显示 `tile.unnamed`

第一版把注入点修对（完整描述符指向桥接方法 `onMessage(IMessage, MessageContext)`，
见 [`HoloInventoryTileRequestMixin`](../src/main/java/com/susy/plusplus/mixin/holoinventory/HoloInventoryTileRequestMixin.java)
的类注释）之后，游戏里仍然显示 `tile.unnamed`。根因有两个：

1. **`TileRequest` 根本没有 getter**。javap 的结果只有 `private BlockPos pos` 字段、
   两个构造函数、`fromBytes/toBytes` 和编译器生成的 `access$000`。
   第一版用 `message.getClass().getMethod("getPos")` 取值，运行时抛
   `NoSuchMethodException` 被 `catch` 吞掉 → 每次都提前 `return`，注入器形同虚设。
   → 现在改为**沿父类链反射读取 `pos` 字段**（`pos` 属于 HoloInventory 自己的类，
   不参与 Forge 的混淆映射，运行时名字不变）。
2. **`tile.unnamed` 是 GT 自己的"兜底名"**。HoloInventory 拿不到名字时会退回
   `te.getBlockType().getUnlocalizedName()`，而 GT 的机器方块
   `gregtech/api/block/machines/BlockMachine` 里就是 `setTranslationKey("unnamed")`，
   翻译键正好是 `tile.unnamed`。所以"对准机器显示 `tile.unnamed`"等价于
   "本补丁**没有**生效"，是一条很好用的自检信号。

---

## 3. 配置

```ini
# 第三方补丁总开关（默认 true）
B:enableThirdPartyPatches=true
```

关掉后：②③（含 1.4 的下拉框翻译、第 2 节的机器名）都恢复第三方 mod 的原始行为
（① 的中文语言文件仍然生效）。

---

## 4. 验证步骤

| # | 操作 | 预期 |
| --- | --- | --- |
| 1 | 中文环境进游戏，看 XNet 的方块/物品名与 Shift 提示 | 中文（来自 ①） |
| 2 | 打开 XNet 控制器界面：复制频道、删除频道、高亮方块 | 提示为中文（来自 ②） |
| 3 | 手持红石代理方块看 tooltip | 三行中文（来自 ②） |
| 4 | 打开控制器 GUI → 新建频道，看"类型"下拉框 | 显示"物品 / 能量 / 流体 / 逻辑"，**并且仍能正常建出频道**（来自 1.4） |
| 5 | 对准任意 GT 机器（箱子 / 机器 / 多方块控制器） | 全息板显示**机器名**（来自 ③），不再是 `tile.unnamed` |
| 6 | 把 `enableThirdPartyPatches=false` 重启 | ②③（含 1.4 下拉框、机器名）恢复英文/空白；① 仍是中文 |
| 7 | 在**不装** XNet 或 HoloInventory 的整合包里启动 | 正常启动，**不崩**（`@Pseudo` + `SuMixinPlugin`） |

---

## 5. 风险与回退

- **风险**：②③ 属于"给第三方 mod 打运行时补丁"。若对方大改实现，最坏情况是补丁静默失效（英文/空白），
  不会导致崩溃；把 `enableThirdPartyPatches` 设为 `false` 即可完全退回原状。
- 本模组**没有**修改任何第三方 mod 的源码或 jar，只在自己的 jar 里带了资源与 Mixin。
