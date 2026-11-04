package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Return target spell or creature to its owner's hand"
 * ([com.wingedsheep.sdk.scripting.effects.ReturnSpellOrPermanentToOwnersHandEffect]): Fatehold
 * Charm, Unsubstantiate, Divide by Zero.
 *
 * The bounce of a permanent, reaching the stack too, read the way a bounce is: flexible. It rescues
 * our creature from removal as readily as it sets back their spell or blocker, so both sides stay
 * candidates and the simulation compares them.
 */
class ReturnSpellOrPermanentReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a spell-or-permanent bounce reads flexible, as a bounce does") {
            for (name in listOf("Unsubstantiate", "Divide by Zero")) {
                withClue("$name: its slot is understood, and either side may be the right one") {
                    intents.spellTargetPolarities(name) shouldBe listOf(TargetPolarity.FLEXIBLE)
                }
            }
        }
    }
}
