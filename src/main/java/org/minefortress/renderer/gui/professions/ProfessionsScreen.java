package org.minefortress.renderer.gui.professions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.remmintan.mods.minefortress.core.interfaces.client.IClientFortressManager;
import net.remmintan.mods.minefortress.core.interfaces.client.IClientManagersProvider;
import net.remmintan.mods.minefortress.core.interfaces.professions.IProfessionsManager;
import org.lwjgl.opengl.GL11;

public class ProfessionsScreen extends Screen {

    private final ProfessionsLayer professionsLayer;
    private final IClientFortressManager fortressManager;
    private final IProfessionsManager professionManager;

    private boolean movingLayer = false;
    private boolean startClick = false;

    public ProfessionsScreen(IClientManagersProvider client) {
        super(Text.translatable("key.minefortress.professions_screen"));
        this.professionsLayer = new ProfessionsLayer(client);
        this.fortressManager = client.get_ClientFortressManager();
        this.professionManager = fortressManager.getProfessionManager();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        int screenX = 0;
        int screenY = 0;
        this.renderBackground(drawContext, mouseX, mouseY, delta);

        this.drawProfessionsTree(drawContext, screenX, screenY);
        drawContext.drawTextWithShadow(textRenderer, this.title, screenX + 8, screenY + 6, 0xFFFFFF);

        final var matrices = RenderSystem.getModelViewStack();
        matrices.pushMatrix();
        matrices.translate((float) (0), (float) (0), 200.0f);
        final String availableColonistsText = String.format("Available colonists: %d/%d", professionManager.getFreeColonists(), fortressManager.getTotalColonistsCount());
        drawContext.drawTextWithShadow(textRenderer, availableColonistsText, screenX + 9, screenY + 18, 0xFFFFFF);
        drawContext.drawTextWithShadow(textRenderer, "left click on profession - add pawn to profession", 2, this.height - (2*this.textRenderer.fontHeight) - 2, 0xffffff);
        drawContext.drawTextWithShadow(textRenderer, "right click on profession - remove pawn from profession", 2, this.height -  this.textRenderer.fontHeight - 2, 0xffffff);
        matrices.popMatrix();

        this.drawWidgetTooltip(drawContext, mouseX, mouseY, screenX, screenY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.startClick = false;
        if (button != 0) {
            this.movingLayer = false;
            return false;
        }
        if (!this.movingLayer) {
            this.movingLayer = true;
        } else {
            this.professionsLayer.move(deltaX, deltaY);
        }
        return true;
    }

    private void drawProfessionsTree(DrawContext drawContext, int screenX, int screenY) {
        org.joml.Matrix4fStack i = RenderSystem.getModelViewStack(); // 1.20.5+: JOML stack
        i.pushMatrix();
        i.translate((float) (screenX), (float) (screenY), 0.0f);
        RenderSystem.applyModelViewMatrix();
        professionsLayer.setLayerSizes(this.width, this.height);
        professionsLayer.render(drawContext);
        i.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableDepthTest();
    }

    private void drawWidgetTooltip(DrawContext drawContext, int mouseX, int mouseY, int screenX, int screenY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        org.joml.Matrix4fStack matrixStack = RenderSystem.getModelViewStack(); // 1.20.5+: JOML stack
        matrixStack.pushMatrix();
        matrixStack.translate((float) (screenX), (float) (screenY), 400.0f);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
        this.professionsLayer.drawWidgetTooltip(drawContext, mouseX - screenX, mouseY - screenY, screenX, width);
        RenderSystem.disableDepthTest();
        matrixStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.startClick = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(startClick) {
            this.professionsLayer.onClick(mouseX, mouseY, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }
}
