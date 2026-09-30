# Changelog

## [1.0.5] - 2026-09-30

### Fixed
- **Crash when opening any XNet GUI** (`NoClassDefFoundError: com/susy/plusplus/mixin/xnet/XNetPatchText`): the shared localisation helper had been placed inside `com.susy.plusplus.mixin` — the very package listed as `"package"` in `mixins.susyplusplus.json`, which Mixin adds to the **class-loader exclusion list**. A `@ModifyConstant` replacement is *merged into the target class* (here XNet's `GuiController`), so at runtime the reference is resolved by the ordinary class loader, which cannot see anything in the mixin package. The helper now lives in [`com.susy.plusplus.integration.xnet`](src/main/java/com/susy/plusplus/integration/xnet/XNetPatchText.java) and all 11 patched GUI classes import it. (The older patches were unaffected because they declared `localize` *inside* the mixin class, so it got merged into the target together with the handler.)
- The controller GUI's channel-type dropdown was still showing the raw key `xnet.item`: `GuiController` feeds `IChannelType#getID()` straight into a `ChoiceLabel`, and mcjtylib's widgets never call `I18n`. Fixed by adding the `xnet.item`/`xnet.energy`/`xnet.fluid`/`xnet.logic` keys *and* a `mcjtylib` mixin that localises only the **displayed** text of `AbstractLabel#setText` for strings starting with `xnet.` — the underlying value stays the raw id, so channels are still created correctly.
- Aiming at a GT machine with HoloInventory still showed `tile.unnamed`: `TileRequest` has **no getter at all** for its `pos` field, so the earlier `getMethod("getPos")` reflection threw `NoSuchMethodException`, was swallowed, and the injection returned early every time. The handler now reflects the private `pos` field (walking up the superclass chain).

### Added
- **Third-party compatibility patches** (master switch `enableThirdPartyPatches`, default on). Both are optional and **completely skipped when the target mod is absent** (`@Pseudo` on every patched mixin + a `SuMixinPlugin` gating on `Loader.isModLoaded`), so packs without those mods are unaffected:
  - **XNet localisation**: this mod now ships `assets/xnet/lang/zh_cn.lang` (XNet itself only ships de_de/ru_ru/tr_tr/en_us, so Chinese users only ever saw English). It mirrors upstream's keys/`%s`/`@f`/`@6`/`\n` exactly. This is a plain resource and is **not** affected by the config switch.
  - **XNet hardcoded strings**: the UI texts hardcoded in xnet's bytecode are replaced with localisable keys via `@ModifyConstant(method = "*", ...)` — no refmap, no dependency on xnet's internal method names. Coverage was extended from the original 3 controller messages + 8 controller errors + 3 redstone-proxy tooltips to **~90 strings across 14 target classes**: the whole controller GUI (buttons `Create`/`Paste`/`Cancel`/`OK`, labels `Channel`/`Connector:`/`Block:`/`Position:`, clipboard messages and their tooltips), the redstone-mode tooltips (`AbstractEditorPanel`), the connector GUI (`Name:`/`Directions:`), the router table headers (`Ch`/`Pos`/`Index`), and every channel/connector settings panel (item / fluid / energy / logic, including the `Pri`/`Rate`/`Min`/`Max`/`Filter`/`BL`/`Ore`/`Meta` labels and all their tooltips). GUI-command tags, NBT keys and JSON fields (`"mode"`, `"priority"`, `"flt"`, …) plus pure-symbol buttons (`x`, `C`, `?`) are deliberately left untouched. Likewise, `AbstractEditorPanel`'s redstone option names (`"Ignored"`/`"Off"`/`"On"`/`"Pulse"`) are **not** replaced even though they look localisable: they double as the *serialized value* (`redstoneMode.addChoiceEvent((parent, newChoice) -> update(tag, newChoice))` → server `RSMode.valueOf(((String) data.get(TAG_RS)).toUpperCase())`), so translating the constant would make the server throw `IllegalArgumentException` and break the setting. Only their tooltips are replaced; the visible option name is localised at the display layer instead.
  - **XNet dropdowns and generated option names**: the controller GUI's "type" dropdown (shown when creating a channel) listed the raw `IChannelType#getID()` keys (`xnet.item`, `xnet.energy`, `xnet.fluid`, `xnet.logic`), because mcjtylib's widgets never call `I18n` (verified: the whole library only references I18n in `BaseBlock`). Those keys are now added to `zh_cn.lang`, and a new `mcjtylib` mixin (`@ModifyVariable` on `AbstractLabel#setText`) localises the *displayed* text while leaving the underlying value intact — so "Create" still sends the real id and channels are built normally. The same hook also translates a strict **whitelist** of XNet's runtime-generated option names: `AbstractEditorPanel#choices(Enum...)` builds each label with `StringUtils.capitalize(enum.toString().toLowerCase())` (so `INS`→`Ins`, `PRIORITY`→`Priority`, `RND`→`Rnd`), which **no `@ModifyConstant` can ever see** because those strings only exist at runtime. Covered now: `Ins`/`Ext`, `Single`/`Stack`/`Count`, `First`/`Rnd`/`Order`, `Priority`/`Roundrobin`/`Distribute`, `Sensor`/`Output`, `Item`/`Fluid`/`Energy`/`Rs`, and `Ignored`/`Off`/`On`/`Pulse`. Anything that is neither `xnet.*` nor in the whitelist is passed through untouched (numbers, machine names and user input are never mangled), and because only the displayed text changes while `ChoiceLabel` keeps the raw value in its own `currentChoice` field, what is sent to the server is unchanged — so settings still save and no `valueOf(...)` throws.
  - **HoloInventory machine names**: aiming at a GT machine now shows the machine's real name. HoloInventory names a hologram only when the *item handler* implements its `api.INamedItemHandler` (GT predates that API), so the patch intercepts `TileRequest$Handler#onMessage` server-side, recognises GT machines via `IGregTechTileEntity`, and supplies the name (response built reflectively so HoloInventory is not a compile-time dependency). The handler reads `TileRequest`'s **private `pos` field** reflectively — the class has *no getter at all*, and the earlier attempt with `getMethod("getPos")` threw `NoSuchMethodException` that was swallowed, so the injection silently did nothing and the hologram kept showing GT's own block fallback name `tile.unnamed` (`BlockMachine` is registered with `setTranslationKey("unnamed")`).
  - Docs: [docs/third_party_patches.md](docs/third_party_patches.md).
- New config option `enableThirdPartyPatches` (default `true`).

## [1.0.4] - 2026-09-25

### Added
- **Multiblock storage upgrades** (`enableMultiblockStorage`, default on), registered with MTE ids 32110–32119:
  - **Steel / Clean Stainless Steel / Reinforced Titanium Multiblock Crate** — single-item-type bulk storage of 1,000,000 / 16,000,000 / 32,000,000 items, using the same `long`-counter approach as GT's quantum chest.
  - **Clean Stainless Steel / Reinforced Titanium Multiblock Tank** — 16,000,000 / 32,000,000 mB. Implemented as subclasses of GT's own `MetaTileEntityMultiblockTank` (only the structure casing/valve, the base texture and `createMetaTileEntity` are overridden), so inventory, GUI, `IFluidHandler` capability, tooltip and "no maintenance" behaviour are inherited unchanged.
  - **Item Valve / Tank Valve** for each tier — expose the controller's inventory (`IItemHandlerModifiable` / `IFluidHandler`) bidirectionally, auto-output downward when facing down, and have no GUI. The item valve uses a new custom `MultiblockAbility` (`susyplusplus_item_valve`).
  - Structure is identical to GT's steel multiblock tank (fixed 3x3x3, controller at the bottom centre, casing >= 23, at most 2 valves).
  - Casings reuse GT's existing blocks: `STEEL_SOLID`, `STAINLESS_CLEAN`, `TITANIUM_STABLE`; textures reuse GT's existing renderers (no new PNGs).
  - Recipes (10) written the same way as GT's own: **shaped crafting-table recipes** (`ModHandler.addShapedRecipe`), mirroring GT's `steel_multiblock_tank` (`" R "` / `"hCw"` / `" R "`), `steel_tank_valve` (bottom rotor) and `steel_crate` (plates). No recipe uses a lower-tier controller/valve as an ingredient (no chained crafting). JEI multiblock preview is automatic via `MetaTileEntities.registerMetaTileEntity`.
  - Docs: [docs/multiblock_crate_and_tank_upgrades.md](docs/multiblock_crate_and_tank_upgrades.md).
- New config option `enableMultiblockStorage` (default `true`).
- **Fluid Sample Storage (MV / HV / EV)** (MTE ids 32120–32122) — mirrors Susy-Core's `fluid_samples_storage`: **32 independent fluid tanks** per machine, no item slots, no recipe processing and **no energy usage**. Per-tank capacity 32,000 / 64,000 / 128,000 L. Crafted exactly like the original (8 large fluid cells around 1 casing), using the aluminium / stainless-steel / titanium large fluid cells and the matching MV / HV / EV machine casing. Textures are reused from GT (no new PNGs). Docs: [docs/fluid_samples_storage.md](docs/fluid_samples_storage.md).
- New config option `enableFluidSamplesStorage` (default `true`).

### Changed
- Multiblock crate GUI: the item slot was **removed** — the stored item type and its count are written into the controller's NBT (`StoredItem` / `StoredCount`) and displayed as text; all insertion/extraction goes through the Item Valve / pipes / hoppers / AE2.
- `vanillaGtCompat` now also disables the **Fluid Sample Storage** machines (and their recipes), since they reuse Susy-Core's `fluid_samples_storage` look and recipe.

### Fixed
- **Texture crash** ("材质崩溃") on the Fluid Sample Storage machines: the custom `OrientedOverlayRenderer` was held in a static field of a *common* class, so (a) a client-only renderer was constructed on the server side too and (b) it could miss `Textures.iconRegisters`' one-shot pass at texture-stitch time, leaving `getParticleSprite()` null and NPE'ing when the machine's item/particles were rendered. The overlay now lives in the `@SideOnly(CLIENT)` `SuStorageTextures`, is force-initialised during **client preInit** (the same rule already documented for `SuTextures`), and `getParticleTexture()` falls back to `VOLTAGE_CASINGS[tier]` if the sprite is still null.

## [1.0.3] - 2026-09-25

### Fixed
- **Crash on startup when The One Probe is not installed** (pure GregTech packs): `SusyPlusPlus.init` called `SuTopIntegration.init()` unconditionally, and loading `SuTopIntegration` links `mcjty/theoneprobe/api/ITheOneProbe` (it appears in the method's stack map table), so the JVM threw `NoClassDefFoundError` **before** the `Loader.isModLoaded("theoneprobe")` guard inside that class could run. The check is now performed in the caller, so `SuTopIntegration` (and the TOP providers it uses) is never touched when TOP is absent.

## [1.0.2] - 2026-09-25

### Added
- New top-of-file config option `vanillaGtCompat` (default `false`) to adapt the mod to **vanilla GregTech** (no SUSY / GroovyScript extras). When enabled:
  - the `Waterproof Paint` fluid material is **not registered**;
  - the Waterproof Spray Can is instead made in the **canner** from Empty Spray Can + **Liquid Silicone Rubber 576 mB** (`Materials.SiliconeRubber`, a vanilla GT fluid); no mixer recipe;
  - the **Wireless Energy Transmission Tower** is not registered;
  - the rubber fluid-pipe tweaks and the Pyrotech recipe tweaks are skipped.
- Docs: new "适配原版 GT" section in [docs/config.md](docs/config.md).

## [1.0.1] - 2026-09-25

### Added
- **Configurator** (`susyplusplus:configurator`): `Shift + V` opens the main UI; three modes — modify machine output faces (with a confirm step), copy / paste machine config, machine toolbox. Assembler recipe; docs: [docs/configurator.md](docs/configurator.md).
- **Trolley** (`susyplusplus:trolley`): `Shift + right-click` a GT machine to pick it up with **zero drops** and full NBT (orientation, covers, item / fluid caches, trait data), `right-click` to put it back down; multiblock controllers / parts are refused. Assembler recipe (programmed circuit #12, not consumed); docs: [docs/trolley.md](docs/trolley.md).
- **Storage Scanner** (`susyplusplus:storage_scanner`, MV single block machine): scans nearby item containers (including non player placed ones) and exposes them as **one writable aggregate inventory** — implemented with GT's own `ItemHandlerList`, the same approach GT's workbench uses for its `connectedInventory`, so hoppers and pipes can both extract and insert. Scan range 3~20 (default 5x5x5), 120 EU/t, on/off via soft mallet, scanned container list persisted to NBT; docs: [docs/storage_scanner.md](docs/storage_scanner.md).
- New config options: `enableConfigurator`, `enableTrolley`, `enableStorageScanner`, `storageScannerDedupeMultiblockStorage`.

### Fixed
- **Recipe conflict** between the configurator and the trolley: the trolley's inputs were a strict subset of the configurator's, so both recipes matched the same assembler inventory. The trolley now uses iron plates plus a programmed circuit #12 (not consumed) and no longer overlaps.
- Duplicate tooltip line on the configurator (the "Shift+V to open" hint was printed twice).
- Storage Scanner slots showed no items (a read-only view cannot accept the vanilla container slot sync).
- Multiblock storages (e.g. Industrial Renewal's storage rank) made the scanned items count N times (once per block); shared back-end inventories are now de-duplicated.
- Removed the useless "tape maintenance hatch" button from the machine toolbox UI.

### Changed
- Storage Scanner GUI only shows scan range / on-off / status / counts (no item grid, no paging, no hint line); item access goes through automation.

## [1.0.0] - 2023-09-15

### Added
- This is a default template changelog that follows the [KeepAChangelog Convention](https://keepachangelog.com/en/1.1.0/)