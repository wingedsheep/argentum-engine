package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A prevention shield over every source *other than* a target creature — "Prevent all combat
 * damage that would be dealt by creatures other than target creature this turn" (Terrifying
 * Presence, `Creature.otherThan(target)`) — leaves that creature the only one dealing combat
 * damage, so the slot reads [TargetPolarity.BENEFICIAL]: the AI aims it at its own best attacker
 * or blocker, never at the opponent's.
 *
 * Unread, the slot was an unknown: the shield names the creature only through its source filter's
 * exclusion, never as the effect's own target.
 */
class PreventionSparesTargetReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a shield sparing one target creature reads beneficial to that creature") {
            withClue("Terrifying Presence: only the targeted creature keeps its combat damage") {
                intents.spellTargetPolarities("Terrifying Presence") shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }
    }
}
