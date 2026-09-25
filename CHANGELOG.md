# Changelog

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