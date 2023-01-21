package shadows.hcrespawns;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hardcorequesting.common.forge.event.PlayerTracker;
import hardcorequesting.common.forge.quests.QuestingDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

public class HQMCompat {

	private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void deleteHQM(ServerPlayer player) {
		QuestingDataManager.getInstance().remove(player);
		File dir = new File(player.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().toFile(), "/hqm");
		File dataFile = new File(dir, "data.json");
		File deathsFile = new File(dir, "deaths.json");

		if (dataFile.exists()) try {
			FileReader fr = new FileReader(dataFile);
			JsonArray arr = GSON.fromJson(fr, JsonArray.class);
			fr.close();
			for (int i = 0; i < arr.size(); i++) {
				JsonObject obj = arr.get(i).getAsJsonObject();
				UUID id = UUID.fromString(GsonHelper.getAsString(obj, "uuid"));
				if (id.equals(player.getUUID())) {
					arr.remove(i);
					break;
				}
			}
			FileWriter fw = new FileWriter(dataFile);
			GSON.toJson(arr, fw);
			fw.close();
		} catch (IOException ex) {
			HardcoreRespawns.LOGGER.error("Failed to adjust HQM data file {} on respawn!", dataFile);
		}

		if (deathsFile.exists()) try {
			FileReader fr = new FileReader(deathsFile);
			JsonArray arr = GSON.fromJson(fr, JsonArray.class);
			fr.close();
			for (int i = 0; i < arr.size(); i++) {
				JsonObject obj = arr.get(i).getAsJsonObject();
				if (obj.has(player.getStringUUID())) {
					arr.remove(i);
					break;
				}
			}
			FileWriter fw = new FileWriter(deathsFile);
			GSON.toJson(arr, fw);
			fw.close();
		} catch (IOException ex) {
			HardcoreRespawns.LOGGER.error("Failed to adjust HQM deaths file {} on respawn!", deathsFile);
		}
		PlayerTracker.instance.onPlayerLogin(player);
	}

	public static int getLives(Player player) {
		return PlayerTracker.instance.getRemainingLives(player);
	}

	public static boolean isHardcore() {
		return QuestingDataManager.getInstance().isHardcoreActive();
	}

}
