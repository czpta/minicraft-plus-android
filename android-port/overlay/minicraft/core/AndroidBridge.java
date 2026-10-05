package minicraft.core;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import awtshim.event.KeyEvent;
import awtshim.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/** Glue between the Android UI layer and the (patched) desktop game. Lives in minicraft.core for package access. */
public final class AndroidBridge {
	private AndroidBridge() {}

	private static Activity activity;
	private static File resDir;
	private static boolean started = false;
	private static final Handler ui = new Handler(Looper.getMainLooper());
	public static volatile boolean crisp = true;
	/** Nanoseconds spent per second in game ticks, whole render() calls, and the part of render that hands pixels to the screen. */
	public static volatile long tickNs, renderNs, presentNs;
	public static String perf(int fps, int tps) {
		long t = tickNs, r = renderNs, p = presentNs;
		tickNs = renderNs = presentNs = 0;
		return String.format("fps=%d tps=%d | tick %.2f ms | render %.2f ms = draw %.2f + present %.2f", fps, tps,
			t / 1e6 / Math.max(1, tps), r / 1e6 / Math.max(1, fps), (r - p) / 1e6 / Math.max(1, fps), p / 1e6 / Math.max(1, fps));
	}
	/** Vertical shift (px, negative = up) of the rendered game, so a focused text field isn't hidden by the keyboard. */
	public static volatile int yShift = 0;

	public static void attach(Activity a) { activity = a; }

	// ---- resources: the desktop code expects a jar/classpath tree, so unpack the APK's copy once ----
	public static File resDir() { return resDir; }

	public static URL getResource(String path) {
		File f = new File(resDir, path.startsWith("/") ? path.substring(1) : path);
		if (!f.exists()) return null;
		try { return f.toURI().toURL(); } catch (MalformedURLException e) { return null; }
	}

	private static void extractResources(Activity a) throws Exception {
		resDir = new File(a.getFilesDir(), "jarres");
		long stamp = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).lastUpdateTime;
		File marker = new File(resDir, ".stamp");
		if (marker.exists() && Long.parseLong(new String(java.nio.file.Files.readAllBytes(marker.toPath())).trim()) == stamp) return;
		deleteRec(resDir);
		resDir.mkdirs();
		copyAssetDir(a.getAssets(), "jarres", resDir);
		java.nio.file.Files.write(marker.toPath(), String.valueOf(stamp).getBytes());
	}

	private static void copyAssetDir(android.content.res.AssetManager am, String path, File out) throws Exception {
		String[] kids = am.list(path);
		if (kids != null && kids.length > 0) {
			out.mkdirs();
			for (String k : kids) copyAssetDir(am, path + "/" + k, new File(out, k));
		} else {
			out.getParentFile().mkdirs();
			try (InputStream in = am.open(path); FileOutputStream os = new FileOutputStream(out)) {
				byte[] buf = new byte[16384]; int r;
				while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
			} catch (java.io.FileNotFoundException e) {
				out.mkdirs(); // an empty directory
			}
		}
	}

	private static void deleteRec(File f) {
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) deleteRec(k);
		f.delete();
	}

	// ---- lifecycle ----
	public static synchronized void startGame() {
		if (started) return;
		started = true;
		final Activity a = activity;
		Thread t = new Thread(null, () -> {
			try {
				extractResources(a);
				File save = a.getExternalFilesDir(null);
				if (save == null) save = a.getFilesDir();
				redirectLogFiles(save);
				applyFont(a);
				Game.main(new String[] { "--savedir", save.getAbsolutePath() });
			} catch (Throwable e) {
				fatal(e);
			}
		}, "minicraft-game", 16 * 1024 * 1024);
		t.start();
	}

	/** The game reads its font from the default pack's gui/font.png; copy the chosen sheet over it before the game loads. */
	private static void applyFont(Activity a) {
		try {
			String choice = a.getSharedPreferences("minicraft", Context.MODE_PRIVATE).getString("font", "classic");
			File src = new File(resDir, "extra_fonts/font_" + ("monocraft".equals(choice) ? "monocraft" : "classic") + ".png");
			File dst = new File(resDir, "assets/textures/gui/font.png");
			java.nio.file.Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (Throwable t) { android.util.Log.w("Minicraft", "font switch failed", t); }
	}

	/** tinylog.properties uses relative paths ("logs/log.txt"); on Android the working dir is "/", so point them at app storage. */
	private static void redirectLogFiles(File base) throws Exception {
		java.util.Properties props = new java.util.Properties();
		try (InputStream in = new java.io.FileInputStream(new File(resDir, "tinylog.properties"))) { props.load(in); }
		for (String k : props.stringPropertyNames()) {
			if (k.endsWith(".file")) System.setProperty("tinylog." + k, new File(base, props.getProperty(k).trim()).getAbsolutePath());
		}
	}

	public static void surfaceChanged(android.view.SurfaceHolder holder, int w, int h) {
		Renderer.canvas.holder = holder;
		Renderer.canvas.setSize(w, h);
		applyScale();
	}

	public static void surfaceDestroyed() { Renderer.canvas.holder = null; }

	/** Integer ("crisp") scaling by default; otherwise stretch-to-fit keeping the 3:2 aspect. */
	public static void applyScale() {
		int w = Renderer.canvas.getWidth(), h = Renderer.canvas.getHeight();
		if (w <= 0 || h <= 0) return;
		float fit = Math.min((float) w / Renderer.WIDTH, (float) h / Renderer.HEIGHT);
		Renderer.SCALE = crisp ? Math.max(1, (int) Math.floor(fit)) : fit;
	}

	// ---- input ----
	public static void key(int vk, boolean down) {
		KeyEvent ev = new KeyEvent(Renderer.canvas, down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
			System.currentTimeMillis(), 0, vk, (char) 0xFFFF);
		for (KeyListener l : Renderer.canvas.keyListeners) { if (down) l.keyPressed(ev); else l.keyReleased(ev); }
	}

	public static void typed(char c) {
		KeyEvent ev = new KeyEvent(Renderer.canvas, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, c);
		for (KeyListener l : Renderer.canvas.keyListeners) l.keyTyped(ev);
	}

	// ---- text input: the game polls getKeysTyped() every tick while a text field has focus ----
	private static volatile long textTick = -1000;
	public static void textFieldTick() { textTick = android.os.SystemClock.uptimeMillis(); }
	public static boolean textFieldActive() { return android.os.SystemClock.uptimeMillis() - textTick < 250; }

	// ---- platform services used by patched game code ----
	public static void openUrl(String url) {
		ui.post(() -> {
			try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
			catch (Throwable ignored) {}
		});
	}

	public static String getClipboard() {
		final String[] out = { "" };
		final Object lock = new Object();
		ui.post(() -> {
			try {
				ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
				if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0)
					out[0] = String.valueOf(cm.getPrimaryClip().getItemAt(0).coerceToText(activity));
			} catch (Throwable ignored) {}
			synchronized (lock) { lock.notifyAll(); }
		});
		synchronized (lock) { try { lock.wait(500); } catch (InterruptedException ignored) {} }
		return out[0];
	}

	public static void setClipboard(String s) {
		ui.post(() -> {
			try {
				((ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("minicraft", s));
			} catch (Throwable ignored) {}
		});
	}

	public static void toast(String msg) { ui.post(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()); }

	public static void fatal(Throwable t) {
		android.util.Log.e("Minicraft", "Fatal", t);
		StringBuilder sb = new StringBuilder(String.valueOf(t));
		for (StackTraceElement e : t.getStackTrace()) { sb.append("\n  at ").append(e); if (sb.length() > 600) break; }
		final String msg = sb.toString();
		ui.post(() -> new android.app.AlertDialog.Builder(activity).setTitle("Minicraft crashed")
			.setMessage(msg).setCancelable(false)
			.setPositiveButton("Close", (d, w) -> { activity.finishAndRemoveTask(); System.exit(1); }).show());
	}
}
