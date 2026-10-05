# Minicraft+ Revived – Android port (AYN Thor target)

Build: `JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/`.
`prepare_sources.py` copies the unmodified desktop sources from `../src` into `app/build/port` and patches/replaces the
desktop-only parts (AWT/Swing window, sound, jamepad, file-system resource loading). `app/src/main/java` holds the shims
(`awtshim`, `javax.*`, jamepad) and the Android layer; `overlay/` holds replacement game classes.
Install: `adb install -r Minicraft-Plus-Thor.apk`. Untested on a device as of the first build.

## Handy for testing (adb)
- `adb shell am start -n com.zapata.minicraftplus/.MainActivity --es autoload <worldName>` jumps straight into an existing world.
- `--es font classic|monocraft` forces the game font; add `--display <id>` to launch on the Thor's bottom screen.
- `adb logcat -s MinicraftPerf MinicraftLayout` shows frame timings and the chosen layout (surface size, internal resolution, scale).
- Note: the game autosaves, so any world you open for a test is modified - use a throwaway world.
