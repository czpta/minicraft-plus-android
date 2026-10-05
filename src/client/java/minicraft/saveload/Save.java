package minicraft.saveload;

import minicraft.core.Game;
import minicraft.core.Renderer;
import minicraft.core.Updater;
import minicraft.core.World;
import minicraft.core.io.Localization;
import minicraft.core.io.Settings;
import minicraft.entity.Arrow;
import minicraft.entity.Entity;
import minicraft.entity.FireSpark;
import minicraft.entity.ItemEntity;
import minicraft.entity.Spark;
import minicraft.entity.furniture.Bed;
import minicraft.entity.furniture.Chest;
import minicraft.entity.furniture.Crafter;
import minicraft.entity.furniture.DeathChest;
import minicraft.entity.furniture.DungeonChest;
import minicraft.entity.furniture.KnightStatue;
import minicraft.entity.furniture.Lantern;
import minicraft.entity.furniture.Spawner;
import minicraft.entity.mob.AirWizard;
import minicraft.entity.mob.EnemyMob;
import minicraft.entity.mob.Mob;
import minicraft.entity.mob.ObsidianKnight;
import minicraft.entity.mob.Player;
import minicraft.entity.mob.Sheep;
import minicraft.entity.particle.Particle;
import minicraft.entity.particle.TextParticle;
import minicraft.entity.vehicle.Boat;
import minicraft.gfx.Point;
import minicraft.item.Inventory;
import minicraft.item.Item;
import minicraft.item.PotionType;
import minicraft.item.Recipe;
import minicraft.level.ChunkManager;
import minicraft.screen.AchievementsDisplay;
import minicraft.screen.CraftingDisplay;
import minicraft.screen.LoadingDisplay;
import minicraft.screen.MultiplayerDisplay;
import minicraft.screen.QuestsDisplay;
import minicraft.screen.ResourcePackDisplay;
import minicraft.screen.SignDisplay;
import minicraft.screen.SkinDisplay;
import minicraft.screen.TutorialDisplayHandler;
import minicraft.screen.WorldSelectDisplay;
import minicraft.util.AdvancementElement;
import minicraft.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Save {

	public String location = Game.gameDir;
	File folder;

	// Used to indent the .json files
	private static final int indent = 4;

	public static String extension = ".miniplussave";

	List<String> data;

	/**
	 * This is the main save method. Called by all Save() methods.
	 * @param worldFolder The folder of where to save
	 */
	private Save(File worldFolder) {
		data = new ArrayList<>();


		if (worldFolder.getParent().equals("saves")) {
			String worldName = worldFolder.getName();
			if (!worldName.toLowerCase().equals(worldName)) {
				Logging.SAVELOAD.debug("Renaming world in " + worldFolder + " to lowercase");
				String path = worldFolder.toString();
				path = path.substring(0, path.lastIndexOf(worldName));
				File newFolder = new File(path + worldName.toLowerCase());
				if (worldFolder.renameTo(newFolder))
					worldFolder = newFolder;
				else
					Logging.SAVELOAD.error("Failed to rename world folder " + worldFolder + " to " + newFolder);
			}
		}

		folder = worldFolder;
		location = worldFolder.getPath() + "/";

		folder.mkdirs();
	}

	/**
	 * This will save world options
	 * @param worldname The name of the world.
	 */
	public Save(String worldname) {
		this(new File(Game.gameDir + "/saves/" + worldname + "/"));

		writeGame("Game");
		writeWorld("Level");
		writePlayer("Player", Game.player);
		writeInventory("Inventory", Game.player);
		writeEntities("Entities");

		WorldSelectDisplay.updateWorlds();

		Updater.notifyAll("minicraft.notification.world_saved");
		Updater.asTick = 0;
		Updater.saving = false;
	}

	/**
	 * This will save the settings in the settings menu.
	 */
	public Save() {
		this(new File(Game.gameDir + "/"));

		if (Game.VERSION.isDev()) { // Is dev build
			Logging.SAVELOAD.debug("In dev build: Searching for old preferences...");
			Version prefVer;
			File prefFile = new File(location, "Preferences.json"); // Only this is checked when checking existence.
			File unlocFile = new File(location, "Unlocks.json");
			try {
				JSONObject json = new JSONObject(Load.loadFromFile(location + "Preferences.json", false));
				prefVer = new Version(json.getString("version"));
			} catch (FileNotFoundException e) {
				Logging.SAVELOAD.debug("Preferences.json is not found, ignoring...");
				prefVer = null;
			} catch (IOException e) {
				Logging.SAVELOAD.error(e, "Unable to load Preferences.json, saving aborted");
				return;
			}

			if (prefVer != null && prefVer.compareTo(Game.VERSION) < 0) {
				Logging.SAVELOAD.info("Old preferences detected, backup performing...");
				File prefBackupFile = new File(location + "Preferences.json.bak");
				if (prefBackupFile.exists()) Logging.SAVELOAD.info("Overwriting old Preferences.json backup...");
				if (prefBackupFile.delete()) Logging.SAVELOAD.trace("Preferences.json.bak is deleted.");
				if (prefFile.renameTo(prefBackupFile)) Logging.SAVELOAD.trace("Preferences.json is renamed to Preferences.json.bak");
				File unlocBackupFile = new File(location + "Unlocks.json.bak");
				if (unlocBackupFile.exists()) Logging.SAVELOAD.info("Overwriting old Unlocks.json backup...");
				if (unlocBackupFile.delete()) Logging.SAVELOAD.trace("Unlocks.json.bak is deleted.");
				if (unlocFile.renameTo(unlocBackupFile)) Logging.SAVELOAD.trace("Unlocks.json is renamed to Unlocks.json.bak");
			}
		}

		Logging.SAVELOAD.debug("Writing preferences and unlocks...");
		writePrefs();
		writeUnlocks();
	}

	public Save(Player player, boolean writePlayer) {
		// This is simply for access to writeToFile.
		this(new File(Game.gameDir + "/saves/" + WorldSelectDisplay.getWorldName() + "/"));
		if (writePlayer) {
			writePlayer("Player", player);
			writeInventory("Inventory", player);
		}
	}

	public static void writeFile(String filename, String[] lines) throws IOException {
		try (BufferedWriter br = new BufferedWriter(new FileWriter(filename))) {
			br.write(String.join(System.lineSeparator(), lines));
		}
	}

	public void writeToFile(String filename, List<String> savedata) {
		try {
			writeToFile(filename, savedata.toArray(new String[0]), true);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		data.clear();

		LoadingDisplay.progress(7);
		if (LoadingDisplay.getPercentage() > 100) {
			LoadingDisplay.setPercentage(100);
		}

		Renderer.render(); // AH HA!!! HERE'S AN IMPORTANT STATEMENT!!!!
	}

	public static void writeToFile(String filename, String[] savedata, boolean isWorldSave) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename))) {
			for (int i = 0; i < savedata.length; i++) {
				bufferedWriter.write(savedata[i]);
				if (isWorldSave) {
					bufferedWriter.write(",");
					if (filename.contains("Level5") && i == savedata.length - 1) {
						bufferedWriter.write(",");
					}
				} else
					bufferedWriter.write("\n");
			}
		}
	}

	public static void writeJSONToFile(String filename, String json) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename))) {
			bufferedWriter.write(json);
		}
	}

	private void writeGame(String filename) {
		data.add(String.valueOf(Game.VERSION));
		data.add(String.valueOf(World.getWorldSeed()));
		data.add(Settings.getIdx("mode") + (Game.isMode("minicraft.settings.mode.score") ? ";" + Updater.scoreTime + ";" + Settings.get("scoretime") : ""));
		data.add(String.valueOf(Updater.tickCount));
		data.add(String.valueOf(Updater.gameTime));
		data.add(String.valueOf(Settings.getIdx("diff")));
		data.add(String.valueOf(AirWizard.beaten));
		data.add(String.valueOf(Settings.get("quests")));
		data.add(String.valueOf(Settings.get("tutorials")));
		data.add(String.valueOf(ObsidianKnight.beaten));
		writeToFile(location + filename + extension, data);
	}

	private void writePrefs() {
		JSONObject json = new JSONObject();

		json.put("version", String.valueOf(Game.VERSION));
		json.put("sound", String.valueOf(Settings.get("sound")));
		json.put("autosave", String.valueOf(Settings.get("autosave")));
		json.put("fps", String.valueOf(Settings.get("fps")));
		json.put("lang", Localization.getSelectedLocale().toLanguageTag());
		json.put("skin", String.valueOf(SkinDisplay.getSelectedSkin()));
		json.put("savedIP", MultiplayerDisplay.savedIP);
		json.put("savedUUID", MultiplayerDisplay.savedUUID);
		json.put("savedUsername", MultiplayerDisplay.savedUsername);
		json.put("keymap", new JSONArray(Game.input.getKeyPrefs()));
		json.put("resourcePacks", new JSONArray(ResourcePackDisplay.getLoadedPacks()));
		json.put("showquests", String.valueOf(Settings.get("showquests")));
		json.put("hwa", String.valueOf(Settings.get("hwa")));

		// Save json
		try {
			writeJSONToFile(location + "Preferences.json", json.toString(indent));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeUnlocks() {
		JSONObject json = new JSONObject();

		JSONArray scoretimes = new JSONArray();
		if (Settings.getEntry("scoretime").getValueVisibility(10))
			scoretimes.put(10);
		if (Settings.getEntry("scoretime").getValueVisibility(120))
			scoretimes.put(120);
		json.put("visibleScoreTimes", scoretimes);

		json.put("unlockedAchievements", new JSONArray(AchievementsDisplay.getUnlockedAchievements()));

		try {
			writeJSONToFile(location + "Unlocks.json", json.toString(indent));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Binary world format: one deflate-compressed file per level, see {@link #writeLevelChunks}. */
	public static final int CHUNKS_MAGIC = 0x4D434C32; // "MCL2"

	/**
	 * Writes every generated chunk of a level (stage UNFINISHED_STAIRS or DONE, so generated structures and stairs are kept),
	 * with its stage, to {@code LevelN/chunks.bin}:
	 * magic, palette (short count + UTF tile names), chunk count, then per chunk: x, y, stage, CHUNK_SIZE^2 palette indices
	 * and CHUNK_SIZE^2 data values (shorts, row by row). The file is written to a temp name first, then moved into place.
	 */
	private void writeLevelChunks(String filename, int l) {
		java.io.File dir = new java.io.File(location + filename + l);
		dir.mkdirs();
		minicraft.level.Level level = World.levels[l];
		ChunkManager cm = level.chunkManager;
		final int S = ChunkManager.CHUNK_SIZE;

		List<Point> chunks = new ArrayList<>();
		for (Point p : cm.getAllChunks())
			if (cm.getChunkStage(p.x, p.y) >= ChunkManager.CHUNK_STAGE_UNFINISHED_STAIRS) chunks.add(p);

		java.util.LinkedHashMap<Integer, Integer> paletteIdx = new java.util.LinkedHashMap<>(); // tile id -> palette index
		List<String> paletteNames = new ArrayList<>();
		short[][] tiles = new short[chunks.size()][S * S];
		short[][] datas = new short[chunks.size()][S * S];
		for (int c = 0; c < chunks.size(); c++) {
			Point p = chunks.get(c);
			for (int y = 0; y < S; y++)
				for (int x = 0; x < S; x++) {
					int tX = p.x * S + x, tY = p.y * S + y;
					minicraft.level.tile.Tile t = cm.getTile(tX, tY);
					Integer pi = paletteIdx.get((int) t.id);
					if (pi == null) { pi = paletteNames.size(); paletteIdx.put((int) t.id, pi); paletteNames.add(t.name); }
					tiles[c][x + y * S] = (short) (int) pi;
					datas[c][x + y * S] = (short) cm.getData(tX, tY);
				}
		}

		java.io.File tmp = new java.io.File(dir, "chunks.bin.tmp"), out = new java.io.File(dir, "chunks.bin");
		try (java.io.DataOutputStream o = new java.io.DataOutputStream(new java.io.BufferedOutputStream(
			new java.util.zip.DeflaterOutputStream(new java.io.FileOutputStream(tmp), new java.util.zip.Deflater(java.util.zip.Deflater.BEST_SPEED)), 1 << 16))) {
			o.writeInt(CHUNKS_MAGIC);
			o.writeShort(paletteNames.size());
			for (String n : paletteNames) o.writeUTF(n);
			o.writeInt(chunks.size());
			for (int c = 0; c < chunks.size(); c++) {
				o.writeInt(chunks.get(c).x);
				o.writeInt(chunks.get(c).y);
				o.writeByte(cm.getChunkStage(chunks.get(c).x, chunks.get(c).y));
				for (short v : tiles[c]) o.writeShort(v);
				for (short v : datas[c]) o.writeShort(v);
			}
		} catch (IOException e) {
			minicraft.core.CrashHandler.errorHandle(e);
			return;
		}
		try {
			java.nio.file.Files.move(tmp.toPath(), out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			minicraft.core.CrashHandler.errorHandle(e);
			return;
		}
		// Drop the old text-format files of this level (index + t.x.y + d.x.y) so they can't be mistaken for current data.
		String[] old = dir.list((d, n) -> n.equals("index" + extension) || n.startsWith("t.") || n.startsWith("d."));
		if (old != null) for (String n : old) new java.io.File(dir, n).delete();

		LoadingDisplay.progress(100f / World.levels.length);
		Renderer.render();
	}

	private void writeWorld(String filename) {
		long saveStart = System.nanoTime();
		LoadingDisplay.setMessage("minicraft.displays.loading.message.levels");
		for (int l = 0; l < World.levels.length; l++) {
			/*String worldSize = String.valueOf(Settings.get("size"));
			data.add(worldSize);
			data.add(worldSize);
			data.add(Long.toString(World.levels[l].getSeed()));
			data.add(String.valueOf(World.levels[l].depth));

			for (int x = 0; x < World.levels[l].w; x++) {
				for (int y = 0; y < World.levels[l].h; y++) {
					data.add(String.valueOf(World.levels[l].getTile(x, y).name));
				}
			}

			writeToFile(location + filename + l + extension, data);*/
			writeLevelChunks(filename, l);
		}
		minicraft.util.Logging.SAVELOAD.debug("World chunks saved in {} ms.", (System.nanoTime() - saveStart) / 1_000_000);

		{ // Advancements
			JSONObject fileObj = new JSONObject();
			fileObj.put("Version", Game.VERSION.toString());
			TutorialDisplayHandler.save(fileObj);
			AdvancementElement.saveRecipeUnlockingElements(fileObj);
			QuestsDisplay.save(fileObj);
			try {
				writeJSONToFile(location + "advancements.json", fileObj.toString(4));
			} catch (IOException e) {
				e.printStackTrace();
				Logging.SAVELOAD.error("Unable to write advancements.json.");
			}
		}

		{ // Sign Data
			JSONObject fileObj = new JSONObject();
			fileObj.put("Version", Game.VERSION.toString());
			JSONArray dataObj = new JSONArray();
			SignDisplay.getSignTexts().forEach((key, value) -> dataObj.put(new JSONObject()
				.put("level", key.getKey())
				.put("x", key.getValue().x)
				.put("y", key.getValue().y)
				.put("lines", value)));
			fileObj.put("signs", dataObj);
			try {
				writeJSONToFile(location + "signs.json", fileObj.toString(4));
			} catch (IOException e) {
				e.printStackTrace();
				Logging.SAVELOAD.error("Unable to write signs.json.");
			}
		}
	}

	private void writePlayer(String filename, Player player) {
		LoadingDisplay.setMessage("Player");
		writePlayer(player, data);
		writeToFile(location + filename + extension, data);
	}

	public static void writePlayer(Player player, List<String> data) {
		data.clear();
		data.add(String.valueOf(player.x));
		data.add(String.valueOf(player.y));
		data.add(String.valueOf(player.spawnx));
		data.add(String.valueOf(player.spawny));
		data.add(String.valueOf(player.health));
		data.add(String.valueOf(player.extraHealth));
		data.add(String.valueOf(player.hunger));
		data.add(String.valueOf(player.armor));
		data.add(String.valueOf(player.armorDamageBuffer));
		data.add(String.valueOf(player.curArmor == null ? "NULL" : player.curArmor.getName()));
		data.add(String.valueOf(player.getScore()));
		data.add(String.valueOf(Game.currentLevel));

		StringBuilder subdata = new StringBuilder("PotionEffects[");

		for (java.util.Map.Entry<PotionType, Integer> potion : player.potioneffects.entrySet())
			subdata.append(potion.getKey()).append(";").append(potion.getValue()).append(":");

		if (player.potioneffects.size() > 0)
			subdata = new StringBuilder(subdata.substring(0, subdata.length() - (1)) + "]"); // Cuts off extra ":" and appends "]"
		else subdata.append("]");
		data.add(subdata.toString());

		data.add(String.valueOf(player.shirtColor));

		JSONObject unlockedRecipes = new JSONObject();
		for (Recipe recipe : CraftingDisplay.getUnlockedRecipes()) {
			JSONArray costs = new JSONArray();
			recipe.getCosts().forEach((c, i) -> costs.put(c + "_" + i));
			unlockedRecipes.put(recipe.getProduct().getName() + "_" + recipe.getAmount(), costs);
		}
		data.add(unlockedRecipes.toString());
	}

	private void writeInventory(String filename, Player player) {
		writeInventory(player, data);
		writeToFile(location + filename + extension, data);
	}

	public static void writeInventory(Player player, List<String> data) {
		data.clear();
		if (player.activeItem != null) {
			data.add(player.activeItem.getData());
		}

		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.invSize(); i++) {
			data.add(inventory.get(i).getData());
		}
	}

	private void writeEntities(String filename) {
		LoadingDisplay.setMessage("minicraft.displays.loading.message.entities");
		for (int l = 0; l < World.levels.length; l++) {
			for (Entity e : World.levels[l].getEntitiesToSave()) {
				String saved = writeEntity(e, true);
				if (saved.length() > 0)
					data.add(saved);
			}
		}

		writeToFile(location + filename + extension, data);
	}

	public static String writeEntity(Entity e, boolean isLocalSave) {
		String name = e.getClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		StringBuilder extradata = new StringBuilder();

		// Don't even write ItemEntities or particle effects; Spark... will probably is saved, eventually; it presents an unfair cheat to remove the sparks by reloading the Game.

		if (isLocalSave && (e instanceof ItemEntity || e instanceof Arrow || e instanceof Spark || e instanceof FireSpark || e instanceof Particle)) // Write these only when sending a world, not writing it. (RemotePlayers are saved separately, when their info is received.)
			return "";

		if (!isLocalSave)
			extradata.append(":").append(e.eid);

		if (e instanceof Mob) {
			Mob m = (Mob) e;
			extradata.append(":").append(m.health);
			if (e instanceof EnemyMob)
				extradata.append(":").append(((EnemyMob) m).lvl);
			else if (e instanceof Sheep)
				extradata.append(":").append(((Sheep) m).cut); // Saves if the sheep is cut. If not, we could reload the save and the wool would regenerate.
		} else if (e instanceof Chest) {
			Chest chest = (Chest) e;

			for (int ii = 0; ii < chest.getInventory().invSize(); ii++) {
				Item item = chest.getInventory().get(ii);
				extradata.append(":").append(item.getData());
			}

			if (chest instanceof DeathChest) extradata.append(":").append(((DeathChest) chest).time);
			if (chest instanceof DungeonChest) extradata.append(":").append(((DungeonChest) chest).isLocked());
		} else if (e instanceof Spawner) {
			Spawner egg = (Spawner) e;
			String mobname = egg.mob.getClass().getName();
			mobname = mobname.substring(mobname.lastIndexOf(".") + 1);
			extradata.append(":").append(mobname).append(":").append(egg.mob instanceof EnemyMob ? ((EnemyMob) egg.mob).lvl : 1);
		} else if (e instanceof Lantern) {
			extradata.append(":").append(((Lantern) e).type.ordinal());
		} else if (e instanceof Crafter) {
			name = ((Crafter) e).type.name();
		} else if (e instanceof KnightStatue) {
			extradata.append(":").append(((KnightStatue) e).getBossHealth());
		} else if (e instanceof Bed) {
			name = ((Bed) e).name;
		} else if (e instanceof Boat) {
			extradata.append(":").append(((Boat) e).getDir().getDir());
		}

		if (!isLocalSave) {
			if (e instanceof ItemEntity) extradata.append(":").append(((ItemEntity) e).getData());
			if (e instanceof Arrow) extradata.append(":").append(((Arrow) e).getData());
			if (e instanceof Spark) extradata.append(":").append(((Spark) e).getData());
			if (e instanceof TextParticle) extradata.append(":").append(((TextParticle) e).getData());
		}
		//else // is a local save

		int depth = 0;
		if (e.getLevel() == null)
			Logging.SAVELOAD.warn("Saving entity with no level reference: " + e + "; setting level to surface");
		else
			depth = e.getLevel().depth;

		extradata.append(":").append(World.lvlIdx(depth));

		return name + "[" + e.x + ":" + e.y + extradata + "]";
	}
}
