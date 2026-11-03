package org.minefortress.gametest

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory
import org.spongepowered.asm.mixin.MixinEnvironment

/**
 * Dev-only check (game-test source set, never shipped): with -Dmfc.mixinAudit=true the client applies every
 * mixin right away instead of when each target class happens to load, logs the outcome and exits.
 * Catches client mixins whose targets moved, which the headless server game tests never load.
 */
class MixinAudit : ClientModInitializer {
    override fun onInitializeClient() {
        if (System.getProperty("mfc.mixinAudit") != "true") return
        val log = LoggerFactory.getLogger("MFC-MIXIN-AUDIT")
        try {
            MixinEnvironment.getCurrentEnvironment().audit()
            log.info("MIXIN AUDIT OK")
        } catch (t: Throwable) {
            log.error("MIXIN AUDIT FAILED", t)
        }
        Runtime.getRuntime().halt(0)
    }
}
