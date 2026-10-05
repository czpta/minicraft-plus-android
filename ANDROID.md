# Minicraft+ for Android

An Android port of [Minicraft+ Revived](https://github.com/MinicraftPlus/minicraft-plus-revived), built against the
**AYN Thor** (Android 13, built-in gamepad) but usable on any Android 8+ device (on-screen controls are optional).

The desktop sources under `src/` are kept as upstream; `android-port/` copies and patches them at build time
(AWT/Swing window, sound, jamepad and jar-resource loading are replaced by Android equivalents).
See [`android-port/README.md`](android-port/README.md) for build instructions and the control map.

Differences from upstream in `src/`: only the random-tick fix in `Level.java`.
Licensed under GPL-3.0 like upstream (see `LICENSE`).
