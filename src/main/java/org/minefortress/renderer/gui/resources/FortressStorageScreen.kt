package org.minefortress.renderer.gui.resources

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.remmintan.mods.minefortress.core.dtos.ItemInfo
import net.remmintan.mods.minefortress.core.utils.ClientModUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Survival fortress "Inventory": a read-only list of everything in the fortress's chests, with counts.
 * Replaces the creative item menu, which handed out free items (play-test bugfix-1).
 */
class FortressStorageScreen : Screen(Text.literal("Fortress storage")) {

    private val resources get() = ClientModUtils.getFortressManager().resourceManager

    private var ticksUntilRefresh = 0
    private var scrollRow = 0
    private var panelX = 0
    private var panelY = 0

    override fun init() {
        super.init()
        panelX = (width - PANEL_W) / 2
        panelY = max(4, (height - PANEL_H) / 2)
        ticksUntilRefresh = 0
    }

    override fun tick() {
        if (--ticksUntilRefresh <= 0) {
            resources.requestStorageContents()
            ticksUntilRefresh = REFRESH_TICKS
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val items = resources.storageContents
        scrollRow = min(scrollRow, maxScroll(items))

        context.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xE0201812.toInt())
        context.drawBorder(panelX, panelY, PANEL_W, PANEL_H, 0xFF8B6B3E.toInt())
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 7, 0xFFFFFF)
        val kinds = Text.literal("${items.size} kinds of item in your fortress chests").formatted(Formatting.GRAY)
        context.drawCenteredTextWithShadow(textRenderer, kinds, width / 2, panelY + 19, 0xFFFFFF)

        val gridX = panelX + (PANEL_W - COLUMNS * SLOT) / 2
        val gridY = panelY + GRID_TOP
        var hovered: ItemInfo? = null
        for (row in 0 until ROWS) {
            for (col in 0 until COLUMNS) {
                val x = gridX + col * SLOT
                val y = gridY + row * SLOT
                context.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0x60000000)
                val info = items.getOrNull((scrollRow + row) * COLUMNS + col) ?: continue
                val stack = info.item.toStack(1)
                context.drawItem(stack, x + 1, y + 1)
                context.drawItemInSlot(textRenderer, stack, x + 1, y + 1, shortCount(info.amount))
                if (mouseX in x until x + SLOT && mouseY in y until y + SLOT) hovered = info
            }
        }
        if (items.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No items yet"), width / 2, gridY + ROWS * SLOT / 2 - 4, 0xAAAAAA)
        }

        val footerY = panelY + PANEL_H - 22
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Pawns build and craft with these items.").formatted(Formatting.GRAY), width / 2, footerY, 0xFFFFFF)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Add items by putting them in a fortress chest.").formatted(Formatting.GRAY), width / 2, footerY + 10, 0xFFFFFF)

        hovered?.let {
            val name = it.item.toStack(1).name
            context.drawTooltip(textRenderer, listOf(name, Text.literal("%,d in storage".format(it.amount)).formatted(Formatting.GRAY)), mouseX, mouseY)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val items = resources.storageContents
        scrollRow = (scrollRow - verticalAmount.toInt()).coerceIn(0, maxScroll(items))
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (client?.options?.inventoryKey?.matchesKey(keyCode, scanCode) == true) {
            close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldPause() = false

    private fun maxScroll(items: List<ItemInfo>) = max(0, (items.size + COLUMNS - 1) / COLUMNS - ROWS)

    companion object {
        private const val COLUMNS = 9
        private const val ROWS = 6
        private const val SLOT = 18
        private const val GRID_TOP = 32
        private const val PANEL_W = 250
        private const val PANEL_H = GRID_TOP + ROWS * SLOT + 30
        private const val REFRESH_TICKS = 20

        fun shortCount(n: Long): String = when {
            n >= 1_000_000 -> "%.1fM".format(java.util.Locale.ROOT, n / 1_000_000.0).replace(".0M", "M")
            n >= 10_000 -> "${n / 1000}k"
            n >= 1_000 -> "%.1fk".format(java.util.Locale.ROOT, n / 1000.0).replace(".0k", "k")
            else -> n.toString()
        }
    }
}
