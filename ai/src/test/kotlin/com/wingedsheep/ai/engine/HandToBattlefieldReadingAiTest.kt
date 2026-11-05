package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * "Put a creature card from your hand onto the battlefield" skips the mana cost the way a free cast
 * does; "put a land card from your hand onto the battlefield" is an extra land drop.
 */
class HandToBattlefieldReadingAiTest : ScenarioTestBase() {

    private fun intentOf(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name))

    init {
        test("a creature put from hand onto the battlefield reads as a free spell") {
            for (card in listOf("Kinscaer Sentry", "Goblin Lackey")) {
                withClue(card) { intentOf(card).tags shouldContain IntentTag.FREE_SPELL }
            }
        }

        test("a land put from hand onto the battlefield reads as ramp, not a free spell") {
            val tags = intentOf("Arboreal Grazer").tags
            tags shouldContain IntentTag.RAMP
            tags shouldNotContain IntentTag.FREE_SPELL
        }

        test("a body that cheats a creature in on every attack is worth more than its stats") {
            withClue("Kinscaer Sentry's attack trigger is a repeatable engine, like a repeatable draw") {
                intentOf("Kinscaer Sentry").staticPriorValue shouldBeGreaterThan intentOf("Savannah Lions").staticPriorValue
            }
        }
    }
}
