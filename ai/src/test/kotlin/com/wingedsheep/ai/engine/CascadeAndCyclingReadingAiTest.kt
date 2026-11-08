package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Two readings the Foundations Jumpstart sweep surfaced:
 *
 * - [com.wingedsheep.sdk.scripting.effects.CascadeEffect] had no intent, so Enlisted Wurm read as
 *   "abilities not recognised" and Bituminous Blast's cascade trigger as a blank. Cascade casts one
 *   more spell off the library top for free — the same card advantage as discover, read as a draw.
 * - Cycling and its typed variants sit in `keywordAbilities`, which the limited heuristic never
 *   looked at: Shardless Outlander (a {7} 6/5 trampler with basic landcycling {2}) was rated as the
 *   body alone. The heuristic now carries a flat "Cycling" term.
 */
class CascadeAndCyclingReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("cascade reads as card draw") {
            for (name in listOf("Enlisted Wurm", "Bituminous Blast", "Bloodbraid Elf")) {
                withClue("$name's cascade casts one more spell for free") {
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.DRAW
                }
            }
        }

        test("a cycling card's heuristic rating credits the cycling") {
            for (name in listOf("Shardless Outlander", "Absorb Vis")) {
                val terms = LimitedCardRater.explain(card(name)).heuristic
                withClue("$name terms: $terms") {
                    terms.singleOrNull { it.part == "Cycling" }?.value shouldBe 0.3
                }
            }
            val vanilla = LimitedCardRater.explain(card("Grizzly Bears")).heuristic
            withClue("a card without cycling has no cycling term: $vanilla") {
                vanilla.none { it.part == "Cycling" } shouldBe true
            }
            LimitedCardRater.explain(card("Shardless Outlander")).heuristicRating shouldBeGreaterThan 2.1
        }
    }
}
