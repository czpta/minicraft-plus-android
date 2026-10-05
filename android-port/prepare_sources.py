#!/usr/bin/env python3
"""Copies the unmodified desktop sources into build/port and patches the desktop-only bits for Android.
usage: prepare_sources.py <repo src dir> <overlay dir> <out dir> <org.json source dir>
"""
import os, re, shutil, sys

src, overlay, out, jsonsrc = sys.argv[1:5]
java, res, assets = (os.path.join(out, d) for d in ('java', 'resources', 'assets'))
for d in (java, res, assets):
    shutil.rmtree(d, ignore_errors=True)
    os.makedirs(d)

def copytree(a, b):
    if os.path.isdir(a):
        shutil.copytree(a, b, dirs_exist_ok=True)

# ---- sources, resources ----
copytree(os.path.join(src, 'client/java'), java)
copytree(os.path.join(src, 'common/java'), java)
for mod in ('client', 'common'):
    r = os.path.join(src, mod, 'resources')
    if os.path.isdir(r):
        # Java resources: everything except top-level assets/ (that name is reserved in an APK)
        for name in os.listdir(r):
            if name == 'assets':
                continue
            p = os.path.join(r, name)
            (shutil.copytree(p, os.path.join(res, name), dirs_exist_ok=True) if os.path.isdir(p)
             else shutil.copy(p, os.path.join(res, name)))
        # the same tree, complete, as an Android asset; unpacked at first launch for the file-based loaders
        copytree(r, os.path.join(assets, 'jarres'))

# extra font sheets (unpacked next to the default pack at first launch; AndroidBridge picks the active one)
extra = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'extra')
for name in ('font_classic.png', 'font_monocraft.png'):
    d = os.path.join(assets, 'jarres', 'extra_fonts'); os.makedirs(d, exist_ok=True)
    shutil.copy(os.path.join(extra, name), d)

# org.json relocated to orgjson
for root, _, files in os.walk(os.path.join(jsonsrc, 'org', 'json')):
    for f in files:
        if f.endswith('.java'):
            t = open(os.path.join(root, f), encoding='utf-8').read().replace('org.json', 'orgjson')
            os.makedirs(os.path.join(java, 'orgjson'), exist_ok=True)
            open(os.path.join(java, 'orgjson', f), 'w', encoding='utf-8').write(t)

def path(rel): return os.path.join(java, 'minicraft', rel)
def read(rel): return open(path(rel), encoding='utf-8').read()
def write(rel, t): open(path(rel), 'w', encoding='utf-8').write(t)

def span(text, sig):
    m = re.search(sig, text)
    if not m: raise SystemExit('patch anchor not found: ' + sig)
    i = text.index('{', m.end() - 1 if text[m.end() - 1] == '{' else m.end())
    depth, j = 0, i
    while True:
        c = text[j]
        if c == '{': depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0: return m.start(), j + 1
        j += 1

def replace_method(rel, sig, new):
    t = read(rel); a, b = span(t, sig); write(rel, t[:a] + new + t[b:])

def drop_imports(rel, pattern):
    t = read(rel); write(rel, re.sub(r'^import (%s);\n' % pattern, '', t, flags=re.M))

def sub(rel, old, new, count=1):
    t = read(rel)
    if old not in t: raise SystemExit('patch text not found in %s: %s' % (rel, old))
    write(rel, t.replace(old, new, count))

# per-second fps/tps line for performance work (adb logcat -s MinicraftPerf)
sub('core/Initializer.java', 'tik = (int) Math.round(ticks * 1000D / interval); // Saves total ticks in last second',
    'tik = (int) Math.round(ticks * 1000D / interval); // Saves total ticks in last second\n\t\t\t\tandroid.util.Log.i("MinicraftPerf", minicraft.core.AndroidBridge.perf(fra, tik));')

sub('core/Initializer.java', 'Updater.tick(); // Calls the tick method (in which it calls the other tick methods throughout the code.',
    '{ long t0 = System.nanoTime(); Updater.tick(); minicraft.core.AndroidBridge.tickNs += System.nanoTime() - t0; }')
sub('core/Initializer.java', 'Renderer.render();', '{ long t0 = System.nanoTime(); Renderer.render(); minicraft.core.AndroidBridge.renderNs += System.nanoTime() - t0; }')

# Options menu: font + scaling live here (the desktop "OpenGL hardware acceleration" toggle means nothing on Android)
sub('screen/OptionsMainMenuDisplay.java', 'Settings.getEntry("hwa"),',
    'minicraft.core.AndroidBridge.fontEntry(),\n\t\t\tminicraft.core.AndroidBridge.layoutEntry(),')

# ---- window / frame ----
drop_imports('core/Initializer.java', r'javax\.swing\.WindowConstants|java\.awt\.(BorderLayout|Color)|java\.awt\.event\.\w+')
replace_method('core/Initializer.java', r'static void createAndDisplayFrame\(\) \{', 'static void createAndDisplayFrame() {\n\t}')
replace_method('core/Initializer.java', r'static void launchWindow\(\) \{', 'static void launchWindow() {\n\t}')
sub('core/Initializer.java', '((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).init();',
    'try { ((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).init(); } catch (Throwable t) { android.util.Log.w("Minicraft", "tinylog init failed", t); }')
drop_imports('core/Updater.java', r'java\.awt\.GraphicsDevice')
replace_method('core/Updater.java', r'static void updateFullscreen\(\) \{', 'static void updateFullscreen() {\n\t}')
drop_imports('core/io/Settings.java', r'java\.awt\.(DisplayMode|GraphicsEnvironment|HeadlessException)')
replace_method('core/io/Settings.java', r'private static int getDefaultRefreshRate\(\) \{', 'private static int getDefaultRefreshRate() {\n\t\treturn 60;\n\t}')

# ---- resources live in a file tree unpacked from the APK ----
replace_method('core/io/FileHandler.java', r'public static Path getJarResourcesPath\(\) \{',
    'public static Path getJarResourcesPath() {\n\t\tif (jarResourcesPath == null) jarResourcesPath = Paths.get(minicraft.core.AndroidBridge.resDir().getAbsolutePath());\n\t\treturn jarResourcesPath;\n\t}')

# ---- network: no desktop HTTP stack needed at startup ----
replace_method('network/Network.java', r'public static void findLatestVersion\(Action callback\) \{',
    'public static void findLatestVersion(Action callback) {\n\t\tnew Thread(() -> {\n\t\t\tlatestVersion = new VersionInfo(VERSION, "", "");\n\t\t\tcallback.act();\n\t\t}).start();\n\t}')

# ---- input ----
drop_imports('core/io/InputHandler.java', r'com\.badlogic\.gdx\.utils\.SharedLibraryLoadRuntimeException')
sub('core/io/InputHandler.java', 'IllegalStateException | SharedLibraryLoadRuntimeException | UnsatisfiedLinkError e', 'Throwable e')

# ---- text fields use Android's native keyboard ----
sub('core/io/InputHandler.java', 'public String getKeysTyped(@Nullable String typing, @Nullable String pattern, boolean multiline) {',
    'public String getKeysTyped(@Nullable String typing, @Nullable String pattern, boolean multiline) {\n\t\tminicraft.core.AndroidBridge.textFieldTick();')
sub('core/io/InputHandler.java', 'keyTypedBuffer = String.valueOf(ke.getKeyChar());', 'keyTypedBuffer += String.valueOf(ke.getKeyChar()); // append: an IME can commit several characters per tick')
replace_method('screen/OnScreenKeyboardMenu.java', r'public static OnScreenKeyboardMenu checkAndCreateMenu\(\) \{', 'public static OnScreenKeyboardMenu checkAndCreateMenu() {\n\t\treturn null; // Android\'s keyboard is used instead\n\t}')

# Done/A on the world-name (or seed) field confirms it: jump to "Create World" (entry 4); the keyboard closes by itself
sub('screen/WorldGenDisplay.java', '		if (onScreenKeyboardMenu == null)\n			super.tick(input);',
    '		if (onScreenKeyboardMenu == null) {\n\t\t\tif (menus[0].getCurEntry() instanceof InputEntry && input.inputPressed("select")) {\n\t\t\t\tmenus[0].setSelection(4);\n\t\t\t\treturn;\n\t\t\t}\n\t\t\tsuper.tick(input);\n\t\t}')

# Popup confirm/cancel (delete world, etc.) only listened for keyboard keys; accept the pad mapping too (A = select, B = exit)
sub('screen/PopupDisplay.java', 'input.getMappedKey(callback.key).isClicked()', 'input.inputPressed(callback.key)')

# Tutorial hint names the keyboard key for the quest panel; on a pad it is L3
sub('screen/TutorialDisplayHandler.java', 'Game.input.getMapping("expandQuestDisplay")', '"L3"')

# ---- desktop-only developer tools (Swing) ----
os.remove(path('level/LevelViewer.java'))
drop_imports('level/LevelGen.java', r'javax\.swing\.\w+')
replace_method('level/LevelGen.java', r'public static void main\(String\[\] args\) \{', '')
drop_imports('item/Recipes.java', r'(javax\.swing\.[\w.]+|java\.awt\.[\w.]+)')
t = read('item/Recipes.java'); a, _ = span(t, r'public static void main\(String\[\] args\) \{')
write('item/Recipes.java', t[:a] + '\n}\n')

# ---- crash/error dialogs: keep ErrorInfo, replace the Swing UI ----
t = read('core/CrashHandler.java')
info = t[t.index('public static class ErrorInfo'):]
write('core/CrashHandler.java', '''package minicraft.core;

import org.jetbrains.annotations.Nullable;

public class CrashHandler {
	public static void crashHandle(Thread thread, Throwable throwable) {
		crashHandle(throwable);
	}

	public static void crashHandle(Throwable throwable) {
		crashHandle(throwable, new ErrorInfo());
	}

	public static void crashHandle(Throwable throwable, ErrorInfo info) {
		AndroidBridge.fatal(throwable);
	}

	public static void errorHandle(Throwable throwable) {
		errorHandle(throwable, new ErrorInfo());
	}

	public static void errorHandle(Throwable throwable, ErrorInfo info) {
		errorHandle(throwable, info, null);
	}

	public static void errorHandle(Throwable throwable, ErrorInfo info, @Nullable Action handling) {
		android.util.Log.e("Minicraft", info.title + ": " + info.message, throwable);
		if (handling != null) handling.act();
	}

	''' + info)

# ---- overlay (Sound, ClipboardHandler, AndroidBridge ...) ----
copytree(overlay, java)

# ---- java.awt -> awtshim everywhere, org.json -> orgjson ----
count = 0
for root, _, files in os.walk(java):
    for f in files:
        if not f.endswith('.java'): continue
        p = os.path.join(root, f)
        t = open(p, encoding='utf-8').read()
        n = t.replace('java.awt.', 'awtshim.').replace('org.json', 'orgjson') if not p.startswith(os.path.join(java, 'orgjson')) else t
        if n != t: count += 1; open(p, 'w', encoding='utf-8').write(n)
print('prepared; rewrote %d files' % count)
