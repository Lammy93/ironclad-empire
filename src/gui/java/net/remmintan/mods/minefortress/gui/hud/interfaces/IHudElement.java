package net.remmintan.mods.minefortress.gui.hud.interfaces;

import net.minecraft.client.gui.DrawContext;

public interface IHudElement {

    void setPos(int x, int y);
    int getAnchorX();
    int getAnchorY();
    default void setPosBasedOn(int basepointX, int basepointY) {
        setPos(basepointX + getAnchorX(), basepointY + getAnchorY());
    }
    /*
     * Named renderHudElement / isHudElementHovered (not render / isHovered) on purpose: HUD buttons extend vanilla
     * widgets whose render/isHovered are remapped to intermediary names in the shipped jar. With the same names here
     * the interface methods were left unimplemented at runtime (AbstractMethodError) once the widgets stopped
     * declaring render themselves (ClickableWidget.render is final since 1.20.3).
     */
    void renderHudElement(DrawContext drawContext, int mouseX, int mouseY, float delta);
    default boolean shouldRender(boolean isCreative) {
        return true;
    }
    default void tick() {}
    boolean isHudElementHovered();

}
