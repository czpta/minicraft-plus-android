# Minicraft+ for Android

An Android port of [Minicraft+ Revived](https://github.com/MinicraftPlus/minicraft-plus-revived), built against the
**AYN Thor** (Android 13, built-in gamepad) but usable on any Android 8+ device (on-screen controls are optional).

The desktop sources under `src/` are kept as upstream; `android-port/` copies and patches them at build time
(AWT/Swing window, sound, jamepad and jar-resource loading are replaced by Android equivalents).
See [`android-port/README.md`](android-port/README.md) for build instructions and the control map.

Differences from upstream in `src/`: only the random-tick fix in `Level.java`.
Licensed under GPL-3.0 like upstream (see `LICENSE`).

## Controls (AYN Thor / any Android gamepad)

The game's own controller scheme is kept; actions it only has keyboard keys for use Select combos.

| Input | Action |
|---|---|
| D-pad / left stick | Move, menu cursor (hold = auto-repeat in menus), switch chest/inventory panel |
| A / B (or Back) | Attack, interact, select / exit, back |
| X / Y | Inventory and menu / crafting |
| L1 / R1 / R3 | Pick up / drop one / drop stack |
| L2 / R2 | Previous / next tab (L2 + R2 together in World Select = delete world) |
| Start | Pause; toggles the inventory search bar |
| R1 held + A | Move a whole stack (chest, creative item picker) |
| R1 held + D-pad | Scroll the quest tree |
| Select + A / B / X / Y | Quick-save / toggle HUD / potion effects / player info |
| Select + L1 / R1 | Screenshot / simple potion list |
| Select + L2 / R2 | Page up / down (inventory search) |
| L3 | Quest panel |

Text fields use Android's keyboard. The gear button at the top centre opens settings (on-screen controls, A/B swap, scaling).
