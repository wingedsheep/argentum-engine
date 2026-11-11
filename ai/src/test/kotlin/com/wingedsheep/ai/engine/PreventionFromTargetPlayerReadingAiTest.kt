package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A prevention shield over the sources a *target player* controls — "Prevent all combat damage
 * that would be dealt this turn by creatures target opponent controls" (Encircling Fissure) —
 * muzzles that player's side, so the player slot reads [TargetPolarity.HARMFUL].
 *
 * Unread, the slot was an unknown in the reading audit: the shield names the player only through
 * its source filter, never as the effect's own target.
 */
class PreventionFromTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a shield over the creatures a target opponent controls reads harmful to that opponent") {
            withClue("Encircling Fissure: the targeted opponent's creatures deal no combat damage") {
                intents.spellTargetPolarities("Encircling Fissure") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }
    }
}
