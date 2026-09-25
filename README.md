## Susy Plus Plus

面向 **SUSY** 整合包的实用附属模组，基于 Minecraft 1.12.2 Forge 开发。

本工程派生自 [TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) 模板（模板部分仍为 MIT 许可，见 [`LICENSE-TemplateDevEnv-MIT.txt`](LICENSE-TemplateDevEnv-MIT.txt)），本工程自身采用 **GNU Lesser General Public License v3.0（LGPL-3.0）** 许可，详见 [许可证](#许可证--license)。

This template runs on **Java 25**, **Gradle 9.7.0** + **[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 2.0.3** + **Forge 14.23.5.2847**.

With **coremod and mixin support** that is easy to configure.

### Instructions:

1. Click `use this template` at the top.
2. Clone the repository that you have created with this template to your local machine.
3. Make sure IDEA is using Java 25 for Gradle before you sync the project. Verify this by going to IDEA's `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM`.
4. Open the project folder in IDEA. When prompted, click "Load Gradle Project" as it detects the `build.gradle`, if you weren't prompted, right-click the project's `build.gradle` in IDEA, select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Run gradle tasks such as `runClient` and `runServer` in the IDEA gradle tab, or use the auto-imported run configurations like `1. Run Client`.

### Notes:
- Dependencies script in [gradle/scripts/dependencies.gradle](gradle/scripts/dependencies.gradle), explanations are commented in the file.
- Publishing script in [gradle/scripts/publishing.gradle](gradle/scripts/publishing.gradle).
- When writing Mixins on IntelliJ, it is advisable to use latest [MinecraftDev Fork for RetroFuturaGradle](https://github.com/eigenraven/MinecraftDev/releases).

---

## 内容 / Features

| 内容 | 说明 | 开关（默认） | 文档 |
| --- | --- | --- | --- |
| **适配原版 GT** | 总开关：不注册「防水漆液」、防水喷漆改用液态硅橡胶、禁用无线能量塔 / 橡胶管道修改 / 火种科技配方 | `vanillaGtCompat`（**关**） | [docs/config.md](docs/config.md) |
| 防水喷漆 | 让机器防水（阻止遇水/地形爆炸），TOP 显示防水状态 | `enableWaterproofSprayCan`（开） | [docs/waterproof_spray_can.md](docs/waterproof_spray_can.md) |
| 电池盒 | 可放电池的饰品/物品，带 GUI 与释能模式 | `enableBatteryCase`（开） | [docs/battery_case.md](docs/battery_case.md) |
| 强化土高炉 | 用原版土高炉配方、不耗电、无需维护、4 并行、可换仓室 | `enableReinforcedPbf`（开） | [docs/reinforced_pbf.md](docs/reinforced_pbf.md) |
| 无线能量传输塔 | 用铁砧改名的 Susy-Core 无人机作为目标，电池总容量决定上限、最低电池电压决定传输电压，最后 3 秒输出 `电压 × 电池个数 × 64` | `enableWirelessEnergyTower`（开） | [docs/wireless_energy_tower.md](docs/wireless_energy_tower.md) |
| **配置器** | Shift+V 选模式：改机器输出面 / 复制机器配置 / 机器工具箱 | `enableConfigurator`（开） | [docs/configurator.md](docs/configurator.md) |
| **手推车** | Shift+右键搬起机器（零掉落、封面/缓存/朝向全保留），右键放下；多方块不可搬 | `enableTrolley`（开） | [docs/trolley.md](docs/trolley.md) |
| **存储检测器** | MV 单方块机器：扫描周围容器（含非玩家放置）并聚合成一个库存，漏斗可直接抽取 | `enableStorageScanner`（开） | [docs/storage_scanner.md](docs/storage_scanner.md) |
| **多方块板条箱** | 单物品类型的大容量物品存储（钢制 1M / 洁净不锈钢 16M / 加强钛 32M 物品） | `enableMultiblockStorage`（开） | [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md) |
| **多方块储罐升级** | 洁净不锈钢 16M mB / 加强钛 32M mB 储罐；钢制档直接用 GT 的 `gregtech:tank.steel` | `enableMultiblockStorage`（开） | [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md) |
| **物品阀门 / 储罐阀门** | 板条箱 / 储罐的取放口（双向，暴露 IItemHandler / IFluidHandler，朝下自动输出） | `enableMultiblockStorage`（开） | [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md) |
| **流体样品存储** | 32 个独立储罐、无物品槽、不耗电；MV / HV / EV 每格 32,000 / 64,000 / 128,000 L | `enableFluidSamplesStorage`（开） | [docs/fluid_samples_storage.md](docs/fluid_samples_storage.md) |
| 橡胶管道修改 | 橡胶流体管道速率对齐钢 + 合金炉配方 | `enableRubberPipeTweaks`（**关**） | [docs/config.md](docs/config.md) |
| 火种科技配方 | 干燥机 / 提取机 / 锻造锤配方 | `enablePyrotechRecipeTweaks`（开） | [docs/config.md](docs/config.md) |

全部开关在 `config/susyplusplus.cfg`，均为**加载期**开关（改动后需重启）。

---

## 许可证 / License

本工程（Susy Plus Plus）采用 **GNU Lesser General Public License version 3**。

- [`LICENSE`](LICENSE) —— GNU Lesser General Public License v3（LGPL-3.0）全文
- [`COPYING`](COPYING) —— GNU General Public License v3（GPL-3.0）全文（LGPL-3.0 以其为基础条款）
- [`LICENSE-TemplateDevEnv-MIT.txt`](LICENSE-TemplateDevEnv-MIT.txt) —— 上游 [TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) 构建脚本的 MIT 许可与 CleanroomMC 版权声明（按 MIT 要求予以保留）

> SPDX-License-Identifier: `LGPL-3.0-only`
