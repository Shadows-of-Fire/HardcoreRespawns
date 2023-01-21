package shadows.hcrespawns;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.network.NetworkEvent.Context;
import shadows.placebo.network.MessageHelper;
import shadows.placebo.network.MessageProvider;

public class RespawnAcknowledge implements MessageProvider<RespawnAcknowledge> {

	private Stage stage;
	private int percentage;

	public RespawnAcknowledge(Stage stage, int percentage) {
		this.stage = stage;
		this.percentage = percentage;
	}

	public RespawnAcknowledge(Stage stage) {
		this(stage, 50);
	}

	@Override
	public void write(RespawnAcknowledge msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.stage.ordinal());
		buf.writeInt(msg.percentage);
	}

	@Override
	public RespawnAcknowledge read(FriendlyByteBuf buf) {
		return new RespawnAcknowledge(Stage.values()[buf.readInt()], buf.readInt());
	}

	@Override
	public void handle(RespawnAcknowledge msg, Supplier<Context> ctx) {
		MessageHelper.handlePacket(() -> () -> {
			HCRespawnClient.ackMsg(msg.stage, msg.percentage);
		}, ctx);
	}

	public static enum Stage {
		RESPAWNING,
		RESETTING_DATA,
		FINDING_SPAWN,
		GENERATING_CHUNKS,
		SPAWNING_NEW_PLAYER,
		DONE;

		public Component comp() {
			return new TranslatableComponent("hcrespawns.text." + this.name().toLowerCase(Locale.ROOT));
		}
	}

}
