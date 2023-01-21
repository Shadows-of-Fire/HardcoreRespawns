package shadows.hcrespawns.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.collect.Iterables;

import hardcorequesting.common.forge.client.sounds.SoundHandler;
import hardcorequesting.common.forge.client.sounds.Sounds;
import hardcorequesting.common.forge.quests.QuestingData;
import hardcorequesting.common.forge.quests.QuestingDataManager;
import hardcorequesting.common.forge.team.Team;
import hardcorequesting.common.forge.team.TeamManager;
import hardcorequesting.common.forge.team.TeamUpdateSize;
import hardcorequesting.common.forge.util.Translator;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@Mixin(QuestingData.class)
public class QuestingDataMixin {

	@Overwrite(remap = false)
	private void outOfLives(Player player) {
		QuestingDataManager manager = QuestingDataManager.getInstance();
		QuestingData data = manager.getQuestingData(player);
		Team team = data.getTeam();
		if (!team.isSingle() && !Iterables.isEmpty(TeamManager.getInstance().getNamedTeams())) {
			team.removePlayer(player);
			if (team.getPlayerCount() == 0) {
				team.deleteTeam();
			} else {
				team.refreshTeamData(TeamUpdateSize.ALL);
			}
		}

		player.getInventory().clearContent(); //had some problem with tconstruct, clear all items to prevent it

		MinecraftServer mcServer = player.getServer();

		if (mcServer.isSingleplayer()) {
			//player.displayClientMessage(new TranslatableComponent("hqm.message.singlePlayerHardcore"), true);
		} else {
			((ServerPlayer) player).sendMessage(Translator.translatable("hqm.message.gameOver"), Util.NIL_UUID);
			SoundHandler.playToAll(Sounds.DEATH);
		}

	}
}
