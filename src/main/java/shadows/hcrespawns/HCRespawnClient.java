package shadows.hcrespawns;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.InitScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import shadows.hcrespawns.RespawnAcknowledge.Stage;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.FORGE, modid = HardcoreRespawns.MODID)
public class HCRespawnClient {

	@SubscribeEvent
	public static void fakeHardcore(InitScreenEvent.Pre e) throws Throwable {
		Minecraft mc = Minecraft.getInstance();
		if (e.getScreen() instanceof DeathScreen ds && HardcoreRespawns.isHardcore(mc.player)) {
			ds.hardcore = true;
		}
	}

	@SubscribeEvent
	public static void hcrButtons(InitScreenEvent.Post e) throws Throwable {
		Minecraft mc = Minecraft.getInstance();
		if (e.getScreen() instanceof DeathScreen ds && HardcoreRespawns.isHardcore(mc.player)) {
			Button hcRespawnButton = new Button(ds.width / 2 - 100, ds.height / 4 + 120, 200, 20, new TranslatableComponent("hcrespawns.text.respawn"), btn -> {
				HardcoreRespawns.CHANNEL.sendToServer(new RespawnMessage());
				mc.setScreen((Screen) null);
			}, (btn, stack, x, y) -> {
				Minecraft.getInstance().screen.renderComponentTooltip(stack, List.of(new TranslatableComponent("hcrespawns.text.respawn_info")), x, y);
			});
			hcRespawnButton.active = false;
			e.addListener(hcRespawnButton);
			ds.exitButtons.add(hcRespawnButton);
			//if (!mc.level.getLevelData().isHardcore()) ds.exitButtons.get(0).visible = false;
		}
	}

	public static void ackMsg(Stage stage, int percentage) {
		Minecraft mc = Minecraft.getInstance();
		if (stage == Stage.RESPAWNING && mc.screen instanceof DeathScreen ds) {
			ProgressScreen ps = new ProgressScreen(true);
			ps.progressStart(stage.comp());
			mc.setScreen(ps);
		} else if (mc.screen instanceof ProgressScreen ps) {
			ps.progressStage(stage.comp());
			ps.progressStagePercentage(percentage);
			if (stage == Stage.DONE) ps.stop();
		}
	}

}
