package shadows.hcrespawns;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;

public class Label implements Widget {
	private final Component text;
	private final int x;
	private final int y;
	private final int color;

	public Label(Component pText, int pX, int pY, int pColor) {
		this.text = pText;
		this.x = pX;
		this.y = pY;
		this.color = pColor;
	}

	public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
		GuiComponent.drawCenteredString(pPoseStack, Minecraft.getInstance().font, this.text, this.x, this.y, this.color);
	}

	public Component getText() {
		return this.text;
	}
}