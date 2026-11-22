package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * "Reveals cards from the top of their library until they reveal N land cards, then puts them into
 * their graveyard": a mill of unknown depth, which the intent reader calls MILL. The limited rater
 * prices it too under "each opponent" (Mind Grind), where only the iteration knows the body's
 * `you` is the opponent milled; the single-player pipeline (Mirko Vosk) gets the same term from
 * the rater's pipeline reading.
 */
class RevealUntilMillRatingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("Mind Grind's each-opponent reveal-until mill earns a rating term") {
            val grind = card("Mind Grind")
            CardIntentAnalyzer.leafTags(grind.script.spellEffect!!) shouldContain IntentTag.MILL
            withClue("the spell's effect is worth more than nothing") {
                LimitedCardRater.effectValue(grind.script.spellEffect!!) shouldBeGreaterThan 0.0
            }
        }
    }
}
