# HasteMod
A client-side Fabric mod that abuses the instamine feature to quickly mine surrounding blocks.

## Requirements
* **[Fabric API](https://modrinth.com/mod/fabric-api)** (Required)
* **[YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)** (Optional, for in-game config GUI)
* **[Mod Menu](https://modrinth.com/mod/modmenu)** (Optional, to access the config screen)

## Default Controls
* `U`: Toggle mod on / off
* `X`: Hold to break blocks (when instamine is possible)
* `Y`: Toggle Block Selection Mode on / off
> *Note: Ensure your keybinds do not conflict in the Minecraft Controls menu.*

## How to Use
1. Press `U` to enable the mod.
2. Hold `X` while looking in your desired direction to break instamineable blocks around you.
3. *(Optional)* Press `Y` to enable **Block Selection Mode**, then mine a block (e.g. Stone). The mod will now only break blocks of that specific type.

## Configuration
When YACL and Mod Menu are installed, you can configure:
* **Mining Shape**: `Cube`, `Sphere`, `Layer` (at your feet), or `Tunnel` (3×3 in your facing direction)
* **Radius**: How far the shape extends (1–16 blocks)
* **Throttle**: Max blocks broken per tick and tick delay between attempts (useful to avoid server anticheat kicks)