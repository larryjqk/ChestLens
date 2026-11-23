# Chest Lens

A lightweight, **client-side** Fabric mod that adds a search bar above storage screens. Type a word and Chest Lens dims every item that doesn't match, so you can find what you need in a full chest in a second, while the container stays fully usable.

## Features

- **Search bar above the storage GUI**, placed outside the container window so it never covers your slots.
- **Case-insensitive substring match** on the item's name and, optionally, its tooltip (lore, enchantments, attributes, potion effects, ...).
- **Visual filter**: non-matching slots are dimmed, matching slots get a cyan highlight. The tooltip of the item under your cursor is always drawn on top and stays readable.
- **Fully interactive while filtering**: take items out, put items in, shift-click and so on work as usual.
- **Storage inventory only**: the filter applies to the container's slots, never to your own inventory.
- **Query persistence**: your search text and the bar's visibility are kept when you close a container and open another one (for example, chest to chest, or chest to barrel).
- **Storage-only**: the search bar appears only on storage screens; other GUIs (crafting table, furnace, anvil, player inventory, ...) are left untouched.
- **Safe typing**: while the search bar is focused, keys like `E`, number keys and `Q` are fed to the text box and do not close the screen, swap hotbar items or drop items.
- **Tooltip search toggle** for when long tooltips hurt performance or add noise. Match results are cached to keep the filter cheap.

## Supported containers

Chest Lens detects storage screens by their menu type:

- Chests (including trapped chests and ender chests) and large chests
- Shulker boxes
- Barrels
- Hoppers
- Dispensers and droppers

## Controls

| Action | Default key | Notes |
| --- | --- | --- |
| Focus the search bar | `K` | Also shows the bar if it was hidden |
| Show / hide the search bar | `F8` | Works while typing too |
| Toggle tooltip search | `F9` | Shows a short message on the HUD |
| Clear the text | `Esc` | While the bar is focused and not empty |
| Leave the search bar | `Esc` | While the bar is focused and empty; press `Esc` again to close the screen |

All keys can be rebound under **Options > Controls > Chest Lens**. Click the search bar with the mouse to focus it at any time.

## How matching works

1. Your query is lowercased and compared against the item's display name.
2. If tooltip search is on and there is no name match, each tooltip line is checked as well.
3. A slot matches if any of those texts **contains** your query.

For example, `aqua` finds an *Aqua Affinity* enchanted book, and `sword` finds every sword, whatever its custom name. Empty slots never match.

## Requirements

- Minecraft: *(fill in the supported version)*
- Fabric Loader: *(fill in the minimum version)*
- Fabric API
- Java: *(fill in the required version)*

Chest Lens is purely client-side. Install it on the client only; it works on any server, including vanilla ones, and does not need to be installed on the server.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and put it in your `mods` folder.
3. Download the latest `chest-lens-*.jar` and put it in the same `mods` folder.
4. Launch the game and open any chest.

## Building from source

```bash
git clone <your-repo-url>
cd chest-lens
./gradlew build        # on Windows: gradlew.bat build
```

The built jar is in `build/libs/`. Use `./gradlew runClient` to launch a development client.

## Project structure

```
src/client/java/com/larryjqk/chestlens/client/
├── ChestLensClient.java                  # keybinds, search box, matching and filter rendering
└── mixin/
    ├── AbstractContainerScreenAccessor.java  # exposes leftPos, topPos, hoveredSlot
    └── AbstractContainerScreenMixin.java     # key handling and filter drawing hooks
src/client/resources/
├── assets/chest-lens/lang/en_us.json     # translations
└── chest-lens.client.mixins.json         # mixin config
```


## License

See the [LICENSE](LICENSE) file.