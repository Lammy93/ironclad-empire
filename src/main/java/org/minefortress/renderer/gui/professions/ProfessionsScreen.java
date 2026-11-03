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
        this.drawHelp(drawContext);
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

    // Play-test bugfix-1: the two old one-liners did not explain how professions work.
    private static final String[] HELP = {
            "How professions work",
            "Free colonists have no job yet. Left-click a profession to train one of them in it.",
            "Right-click a profession to take one pawn out of it again (they become free colonists).",
            "Hover a profession to see what it needs. Dark ones are still locked: build what they ask for,",
            "or unlock the profession they branch from first. Drag with the mouse to move the tree.",
    };

    private void drawHelp(DrawContext drawContext) {
        final int lineHeight = this.textRenderer.fontHeight + 2;
        int boxWidth = 0;
        for (String line : HELP) boxWidth = Math.max(boxWidth, this.textRenderer.getWidth(line));
        final int top = this.height - HELP.length * lineHeight - 6;
        drawContext.fill(0, top - 3, boxWidth + 8, this.height, 0xB0000000);
        for (int i = 0; i < HELP.length; i++) {
            drawContext.drawTextWithShadow(textRenderer, HELP[i], 4, top + i * lineHeight, i == 0 ? 0xFFD27F : 0xFFFFFF);
        }
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
