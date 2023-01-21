package shadows.hcrespawns;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import shadows.placebo.config.Configuration;
import shadows.placebo.network.MessageHelper;

@Mod(HardcoreRespawns.MODID)
public class HardcoreRespawns {

	public static final String MODID = "hcrespawns";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, MODID))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on

	public static boolean useHeightmap = true;
	public static boolean moveDownFromTop = false;
	public static String respawnCommand = "";

	public HardcoreRespawns() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		loadConfig();
		MinecraftForge.EVENT_BUS.addListener((Consumer<AddReloadListenerEvent>) e -> loadConfig());
	}

	@SubscribeEvent
	public void init(FMLCommonSetupEvent e) {
		MessageHelper.registerMessage(CHANNEL, 0, new RespawnMessage());
		MessageHelper.registerMessage(CHANNEL, 1, new RespawnAcknowledge(null));
	}

	public static void loadConfig() {
		Configuration cfg = new Configuration(MODID);
		useHeightmap = cfg.getBoolean("Use Heightmap", "general", true, "If the worlds heightmap is consulted for finding a spawn position during hardcore respawns.");
		moveDownFromTop = cfg.getBoolean("Move Down", "general", false, "If spawn-searching should move downwards instead of upwards. Use if the spawn world has a ceiling.");
		respawnCommand = cfg.getString("Respawn Command", "general", "", "A command to be executed when a respawn occurs. The leading slash should be omitted. The keyphrase <player> will be replaced with the player's name.");
		if (cfg.hasChanged()) cfg.save();
	}

	public static boolean isHardcore(Player player) {
		if (player.level.getLevelData().isHardcore()) return true;

		if (ModList.get().isLoaded("hardcorequesting")) {
			return HQMCompat.isHardcore() && HQMCompat.getLives(player) <= 0;
		}

		return false;
	}

}
