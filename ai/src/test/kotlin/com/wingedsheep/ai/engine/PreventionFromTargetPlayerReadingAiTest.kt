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
 *
 * The same holds for a shield over one *target creature* as the source — "Prevent all damage that
 * would be dealt to this creature by target creature this turn" (Stonewise Fortifier,
 * `PreventionSourceFilter.Specific`): the targeted creature is the one muzzled, so the AI aims it
 * at a threat rather than at its own creature.
 */
class PreventionFromTargetPlayerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a shield over the creatures a target opponent controls reads harmful to that opponent") {
            withClue("Encircling Fissure: the targeted opponent's creatures deal no combat damage") {
                intents.spellTargetPolarities("Encircling Fissure") shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("a shield against one target creature's damage reads harmful to that creature") {
            val ability = cardRegistry.getCard("Stonewise Fortifier")!!.activatedAbilities.single()
            withClue("Stonewise Fortifier: the targeted creature's damage to the Fortifier is prevented") {
                intents.abilityTargetPolarities("Stonewise Fortifier", ability.id) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }
    }
}
