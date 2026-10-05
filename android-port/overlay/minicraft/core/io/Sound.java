package minicraft.core.io;

import android.media.AudioAttributes;
import android.media.SoundPool;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import minicraft.core.AndroidBridge;
import minicraft.util.Logging;
import org.jetbrains.annotations.Nullable;

/** Android replacement for the javax.sound mixer: same public API, sounds played through SoundPool. */
public class Sound {
	private static final HashMap<String, Sound> sounds = new HashMap<>();
	private static SoundPool pool;

	private final int id;

	private Sound(int id) { this.id = id; }

	private static synchronized SoundPool pool() {
		if (pool == null) {
			pool = new SoundPool.Builder().setMaxStreams(8)
				.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
		}
		return pool;
	}

	public static void resetSounds() {
		synchronized (Sound.class) {
			for (Sound s : sounds.values()) pool().unload(s.id);
			sounds.clear();
		}
	}

	public static void loadSound(String key, InputStream in, String pack) {
		try {
			File dir = new File(AndroidBridge.resDir().getParentFile(), "sndcache");
			dir.mkdirs();
			File f = new File(dir, key.replaceAll("[^A-Za-z0-9_.-]", "_") + "_" + Math.abs(pack.hashCode()) + ".wav");
			try (FileOutputStream out = new FileOutputStream(f)) {
				byte[] buf = new byte[8192]; int n;
				while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
			}
			int id = pool().load(f.getAbsolutePath(), 1);
			synchronized (Sound.class) { sounds.put(key, new Sound(id)); }
		} catch (IOException | RuntimeException e) {
			Logging.RESOURCEHANDLER_SOUND.error("Could not load audio \"{}\" in pack \"{}\": {}", key, pack, e.toString());
		}
	}

	@Nullable
	public static Sound getSound(String key) { synchronized (Sound.class) { return sounds.get(key); } }

	public static void play(String key) {
		Sound s = getSound(key);
		if (s != null) s.play();
	}

	public static void loop(String key, int count) {
		Sound s = getSound(key);
		if (s != null) s.loop(count);
	}

	public static void tick() {}

	public void play() {
		if (!(boolean) Settings.get("sound")) return;
		pool().play(id, 1f, 1f, 1, 0, 1f);
	}

	/** @deprecated no longer supported, but reserved for future implementation. */
	@Deprecated
	public void loop(int count) {}
}
