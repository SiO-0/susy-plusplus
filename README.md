## Susy Plus Plus

面向 **SUSY** 整合包的实用附属模组，基于 Minecraft 1.12.2 Forge 开发。

当前版本：**v1.1.1**（变更记录见 [`CHANGELOG.md`](CHANGELOG.md)）。

本工程派生自 [TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) 模板（模板部分仍为 MIT 许可，见 [`LICENSE-TemplateDevEnv-MIT.txt`](LICENSE-TemplateDevEnv-MIT.txt)），本工程自身采用 **GNU Lesser General Public License v3.0（LGPL-3.0）** 许可，详见 [许可证](#许可证--license)。

构建工具链：**Java 25** + **Gradle 9.7.0** + **[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 2.0.3** + **Forge 14.23.5.2847**，内置易于配置的 **coremod / Mixin** 支持。

### 开发环境 / Development

- **JDK**：Gradle 守护进程用 **JDK 25**（`CurseForgeGradle` 等插件要求），编译目标为 **Java 8**。`gradle.properties` 中的 `org.gradle.java.home` 与 `org.gradle.java.installations.paths` 指向本机安装位置，换机器时按需修改。
- **构建**：`gradlew.bat build --console=plain`，产物为 `build/libs/susyplusplus-<版本>.jar`（另有 `-dev.jar` 与 `-sources.jar`）。
- **客户端开发运行**：用仓库根目录的 [`run-client.bat`](run-client.bat) 启动（已针对 220+ mod 整合包与 15 GB 内存机器调整堆与原生内存参数；背景与排错见 [`docs/dev_environment.md`](docs/dev_environment.md)）。直接 `gradlew.bat runClient` 也可，但更容易遇到原生 OOM。
- **依赖**：[`gradle/scripts/dependencies.gradle`](gradle/scripts/dependencies.gradle)（文件内有逐条注释）；发布：[`gradle/scripts/publishing.gradle`](gradle/scripts/publishing.gradle)。
- **Mixin**：在 IDEA 中编写时建议使用最新 [MinecraftDev Fork for RetroFuturaGradle](https://github.com/eigenraven/MinecraftDev/releases)。

---

## 内容 / Features

| 内容 | 说明 | 开关（默认） | 文档 |
| --- | --- | --- | --- |
| **适配原版 GT** | 总开关：不注册「防水漆液」、防水喷漆改用液态硅橡胶、禁用无线能量塔 / 橡胶管道修改 / 火种科技配方 | `vanillaGtCompat`（**关**） | [docs/config.md](docs/config.md) |
| 防水喷漆 | 让机器防水（阻止遇水/地形爆炸），TOP 显示防水状态 | `enableWaterproofSprayCan`（开） | [docs/waterproof_spray_can.md](docs/waterproof_spray_can.md) |
| 电池盒 | 可放电池的饰品/物品，带 GUI 与释能模式 | `enableBatteryCase`（开） | [docs/battery_case.md](docs/battery_case.md) |
| 强化土高炉 | 用原版土高炉配方、不耗电、无需维护、4 并行、可换仓室 | `enableReinforcedPbf`（开） | [docs/reinforced_pbf.md](docs/reinforced_pbf.md) |
| **电动碾磨机** | LV 电动矿石粗碎：**直接复用研磨机配方表**（产率 / 副产物 / 耗时完全跟随研磨机），**32 并行**；填「蒸汽研磨机 → 偏心破碎机」之间的空档，**不需要锰钢** | `enableElectricGrinder`（开） | [docs/electric_grinder.md](docs/electric_grinder.md) |
| 无线能量传输塔 | 用铁砧改名的 Susy-Core 无人机作为目标，电池总容量决定上限、最低电池电压决定传输电压，最后 3 秒输出 `电压 × 电池个数 × 64` | `enableWirelessEnergyTower`（开） | [docs/wireless_energy_tower.md](docs/wireless_energy_tower.md) |
| **蒸汽泵封面** | 机器封面：**1024 L/t** 蒸汽专送（仅 `gregtech:steam` / `susy:dense_steam`），**无电耗、无缓存**，复用 GT 的 LV 泵贴图 | `enableSteamPump`（开） | [docs/steam_pump_and_pipes.md](docs/steam_pump_and_pipes.md) |
| **蒸汽管道** | 2 个压力等级 × 5 种粗细 = **10 种方块**，仅通蒸汽；低压 **64 → 1536 L/t**（**橙色**）、高压 **256 → 6144 L/t**（**黑色**）；同压力才连接，沿用 GT 管道的连接/扳手/封面/碰撞箱行为 | 始终注册（无开关） | [docs/steam_pump_and_pipes.md](docs/steam_pump_and_pipes.md) |
| **红石控制阀** | 三种单方块通断阀：流体阀 / 物流阀 / IR 流体阀（无限速主动抽取）；**无红石=开、通电=关**；扳手设输入面、Shift+扳手设输出面；关闭时管道仍保持连接 | `enableRedstoneValves`（开） | [docs/redstone_valves.md](docs/redstone_valves.md) |
| **配置器** | Shift+V 选模式：改机器输出面 / 复制机器配置 / 机器工具箱 / 流体容量限制 / **重命名机器**（带输入框的 UI，名称显示在机器上方） | `enableConfigurator`（开） | [docs/configurator.md](docs/configurator.md) |
| **手推车** | Shift+右键搬起机器（零掉落、封面/缓存/朝向全保留），右键放下；多方块不可搬 | `enableTrolley`（开） | [docs/trolley.md](docs/trolley.md) |
| **存储检测器** | MV 单方块机器：扫描周围容器（含非玩家放置）并聚合成一个库存，漏斗可直接抽取 | `enableStorageScanner`（开） | [docs/storage_scanner.md](docs/storage_scanner.md) |
| **多方块板条箱** | 单物品类型的大容量物品存储（钢制 4M / 洁净不锈钢 64M / 加强钛 128M 物品） | `enableMultiblockStorage`（开） | [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md) |
| **物品阀门** | 板条箱的取放口（双向，暴露 IItemHandler，朝下自动输出） | `enableMultiblockStorage`（开） | [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md) |
| **流体样品存储** | 32 个独立储罐、无物品槽、不耗电；MV / HV / EV 每格 32,000 / 64,000 / 128,000 L | `enableFluidSamplesStorage`（开） | [docs/fluid_samples_storage.md](docs/fluid_samples_storage.md) |
| **第三方补丁** | XNet 中文语言文件 + XNet 硬编码文字本地化 + HoloInventory 显示 GT 机器名（**没装对应 mod 则完全不执行**） | `enableThirdPartyPatches`（开） | [docs/third_party_patches.md](docs/third_party_patches.md) |
| Susy 中文补充翻译 | 为 Susy-Core 中未翻译的内容补上中文（**只增不改**，不覆盖原文；反射注入语言表，失败只记警告） | `enableSusyTranslations`（**关**） | [docs/steam_pump_and_pipes.md](docs/steam_pump_and_pipes.md) |
| 橡胶管道修改 | 橡胶流体管道速率对齐钢 + 合金炉配方 | `enableRubberPipeTweaks`（**关**） | [docs/config.md](docs/config.md) |
| 火种科技配方 | 干燥机 / 提取机 / 锻造锤配方 | `enablePyrotechRecipeTweaks`（开） | [docs/config.md](docs/config.md) |

全部开关在 `config/susyplusplus.cfg`，均为**加载期**开关（改动后需重启）。蒸汽管道不受开关控制、始终注册（注册时机早于配置注入，见 [`SuSteamPipes`](src/main/java/com/susy/plusplus/pipe/steam/SuSteamPipes.java) 的说明）。

---

## 许可证 / License

本工程（Susy Plus Plus）采用 **GNU Lesser General Public License version 3**。

- [`LICENSE`](LICENSE) —— GNU Lesser General Public License v3（LGPL-3.0）全文
- [`COPYING`](COPYING) —— GNU General Public License v3（GPL-3.0）全文（LGPL-3.0 以其为基础条款）
- [`LICENSE-TemplateDevEnv-MIT.txt`](LICENSE-TemplateDevEnv-MIT.txt) —— 上游 [TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) 构建脚本的 MIT 许可与 CleanroomMC 版权声明（按 MIT 要求予以保留）

> SPDX-License-Identifier: `LGPL-3.0-only`
