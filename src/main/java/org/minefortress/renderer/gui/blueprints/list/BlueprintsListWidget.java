package org.minefortress.renderer.gui.blueprints.list;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;

public class BlueprintsListWidget extends AlwaysSelectedEntryListWidget<BlueprintListEntry> {

    public BlueprintsListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, bottom - top, top, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return width;
    }

    @Override
    protected int getScrollbarX() {
        return this.getRight() - 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
