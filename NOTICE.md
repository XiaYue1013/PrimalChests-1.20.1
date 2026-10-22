# Notice

**PrimalChests** was created by **Jared (jaredlll08)** — https://github.com/jaredlll08/PrimalChests —
and is released under the **GNU Lesser General Public License, version 2.1** (see `LICENSE`).

This project is an **unofficial port** of PrimalChests 1.0.3 (Minecraft 1.12 / 1.12.2) to
Minecraft Forge 1.20.1. It is not made, endorsed or supported by the original author. Please report
problems with this port to the port, not to the original author.

## Changes made in this port (LGPL-2.1 §2(b))

Modified 2026-10-21: every Java source file was rewritten for Forge 1.20.1 (official Mojang mappings);
each file carries a header saying so. In summary:

- `TileEntity`/`TileEntitySpecialRenderer`/`Container`/`GuiContainer` replaced by `BlockEntity`,
  `BlockEntityRenderer`, `AbstractContainerMenu` and `AbstractContainerScreen`; the duplicated
  per-chest classes were merged (`ChestTier` holds the differences).
- Registration moved to `DeferredRegister`; the proxies and the GUI handler were removed.
- The chest facing moved from the tile entity NBT into the block state.
- The inventory now marks the block entity dirty when it changes, and the capability is a
  `LazyOptional` that is invalidated with the block entity.
- The 1.12 `ModelChest` geometry is reproduced as a 1.20 model layer so the original textures are used unchanged.
- Recipes, loot tables, the axe tag, the block atlas entry, the JSON lang files (`en_us`, and a new
  `zh_tw`) and `mods.toml` were added or converted for 1.20.1.

The textures, GUI textures and block/item models are the original files, unchanged.

## Source code

The complete corresponding source code of this port is published together with each binary
(`PrimalChests-<version>-sources.jar`, produced by `gradlew build`), and is the content of this repository.
