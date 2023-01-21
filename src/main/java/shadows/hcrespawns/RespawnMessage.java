package shadows.hcrespawns;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent.Context;
import shadows.hcrespawns.RespawnAcknowledge.Stage;
import shadows.placebo.network.MessageHelper;
import shadows.placebo.network.MessageProvider;
import shadows.placebo.network.PacketDistro;

public class RespawnMessage implements MessageProvider<RespawnMessage> {

	@Override
	public void write(RespawnMessage msg, FriendlyByteBuf buf) {
	}

	@Override
	public RespawnMessage read(FriendlyByteBuf buf) {
		return new RespawnMessage();
	}

	@Override
	public void handle(RespawnMessage msg, Supplier<Context> ctx) {
		MessageHelper.handlePacket(() -> () -> {
			ServerPlayer player = ctx.get().getSender();
			if (player.getHealth() > 0.0F || !HardcoreRespawns.isHardcore(player) || player.getRemovalReason() == RemovalReason.DISCARDED) return;
			HardcoreRespawns.CHANNEL.reply(new RespawnAcknowledge(Stage.RESPAWNING), ctx.get());
			respawn(player, false);
		}, ctx);
	}

	public ServerPlayer respawn(ServerPlayer player, boolean pKeepEverything) {
		PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.RESETTING_DATA), player);
		PlayerList pList = player.getServer().getPlayerList();
		pList.playersByUUID.remove(player.getUUID());
		pList.players.remove(player);
		pList.stats.remove(player.getUUID());
		pList.advancements.remove(player.getUUID());

		player.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);

		Path dataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
		File playerFile = new File(dataDir.toAbsolutePath().toFile(), player.getStringUUID() + ".dat");
		if (!playerFile.delete()) {
			HardcoreRespawns.LOGGER.error("Failed to delete playerdata file {} on respawn!", playerFile);
		}

		File duckFile = new File(dataDir.toAbsolutePath().toFile(), player.getStringUUID() + ".tdu");
		if (duckFile.exists() && !duckFile.delete()) {
			HardcoreRespawns.LOGGER.error("Failed to delete duck file {} on respawn!", duckFile);
		}

		Path advDir = player.getServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR);
		File advFile = new File(advDir.toAbsolutePath().toFile(), player.getStringUUID() + ".json");
		if (!advFile.delete()) {
			HardcoreRespawns.LOGGER.error("Failed to delete advancement file {} on respawn!", advFile);
		}

		Path statDir = player.getServer().getWorldPath(LevelResource.PLAYER_STATS_DIR);
		File statFile = new File(statDir.toAbsolutePath().toFile(), player.getStringUUID() + ".json");
		if (!statFile.delete()) {
			HardcoreRespawns.LOGGER.error("Failed to delete stats file {} on respawn!", statFile);
		}

		if (ModList.get().isLoaded("hardcorequesting")) HQMCompat.deleteHQM(player);

		int progress = 2;

		PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.FINDING_SPAWN, progress), player);

		ServerLevel overworld = player.server.overworld();
		BlockPos spawn = overworld.getSharedSpawnPos();
		BlockPos randomSpawn = new BlockPos(spawn.getX() + nextInt(player.getRandom()), spawn.getY(), spawn.getZ() + nextInt(player.getRandom()));
		overworld.getBlockState(randomSpawn); // Forcibly load the target chunk
		if (overworld.dimensionType().hasSkyLight() && HardcoreRespawns.useHeightmap) {
			randomSpawn = new BlockPos(randomSpawn.getX(), overworld.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, randomSpawn.getX(), randomSpawn.getZ()), randomSpawn.getZ());
		}
		overworld.setDefaultSpawnPos(randomSpawn, 0);

		ServerPlayer newPlayer = new ServerPlayer(player.server, overworld, player.getGameProfile());
		newPlayer.setPos(Vec3.atCenterOf(randomSpawn));
		newPlayer.connection = player.connection;
		newPlayer.connection.player = newPlayer;
		newPlayer.setId(player.getId());
		newPlayer.setMainArm(player.getMainArm());

		for (String s : player.getTags()) {
			newPlayer.addTag(s);
		}

		boolean success = overworld.noCollision(newPlayer);
		int tries = 0;

		while (!success) {
			if (newPlayer.getY() < (double) overworld.getMaxBuildHeight()) {
				newPlayer.setPos(newPlayer.getX(), newPlayer.getY() + (HardcoreRespawns.moveDownFromTop ? -1 : 1), newPlayer.getZ());
				tries++;
			}

			if (overworld.noCollision(newPlayer)) {
				success = true;
				overworld.setDefaultSpawnPos(newPlayer.blockPosition(), 0);
				break;
			}

			if (!success && tries >= 40) {
				PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.FINDING_SPAWN, (progress = progress + 2)), player);
				randomSpawn = new BlockPos(spawn.getX() + nextInt(player.getRandom()), spawn.getY(), spawn.getZ() + nextInt(player.getRandom()));
				overworld.getBlockState(randomSpawn); // Forcibly load the target chunk
				if (overworld.dimensionType().hasSkyLight() && HardcoreRespawns.useHeightmap) {
					randomSpawn = new BlockPos(randomSpawn.getX(), overworld.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, randomSpawn.getX(), randomSpawn.getZ()), randomSpawn.getZ());
				}
				overworld.setDefaultSpawnPos(randomSpawn, 0);
				newPlayer.setPos(Vec3.atCenterOf(randomSpawn));
				tries = 0;
			}
		}

		PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.GENERATING_CHUNKS), newPlayer);
		LevelData leveldata = newPlayer.level.getLevelData();
		newPlayer.connection.send(new ClientboundRespawnPacket(newPlayer.level.dimensionTypeRegistration(), newPlayer.level.dimension(), BiomeManager.obfuscateSeed(newPlayer.getLevel().getSeed()), newPlayer.gameMode.getGameModeForPlayer(), newPlayer.gameMode.getPreviousGameModeForPlayer(), newPlayer.getLevel().isDebug(), newPlayer.getLevel().isFlat(), pKeepEverything));
		newPlayer.connection.teleport(newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(), newPlayer.getYRot(), newPlayer.getXRot());
		newPlayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(overworld.getSharedSpawnPos(), overworld.getSharedSpawnAngle()));
		newPlayer.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
		newPlayer.connection.send(new ClientboundSetExperiencePacket(newPlayer.experienceProgress, newPlayer.totalExperience, newPlayer.experienceLevel));
		pList.sendLevelInfo(newPlayer, overworld);
		pList.sendPlayerPermissionLevel(newPlayer);
		overworld.addNewPlayer(newPlayer);
		PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.SPAWNING_NEW_PLAYER), newPlayer);
		pList.addPlayer(newPlayer);
		pList.playersByUUID.put(newPlayer.getUUID(), newPlayer);
		newPlayer.initInventoryMenu();
		newPlayer.setHealth(newPlayer.getHealth());
		net.minecraftforge.event.ForgeEventFactory.firePlayerRespawnEvent(newPlayer, pKeepEverything);

		player.getServer().getCommands().performCommand(player.getServer().createCommandSourceStack().withPosition(player.position()), HardcoreRespawns.respawnCommand.replace("<player>", player.getGameProfile().getName()));
		PacketDistro.sendTo(HardcoreRespawns.CHANNEL, new RespawnAcknowledge(Stage.DONE), newPlayer);
		return newPlayer;
	}

	private static int nextInt(Random rand) {
		return (rand.nextBoolean() ? -1 : 1) * (1000 + rand.nextInt(1337));
	}
}
