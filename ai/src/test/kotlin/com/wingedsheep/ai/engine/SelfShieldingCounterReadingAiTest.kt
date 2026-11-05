package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * "Whenever this creature becomes the target of a spell or ability for the first time each turn,
 * counter that spell or ability" (Glyph Keeper) — `CounterTargetSource.TargetingSource` — reads as
 * ward-like [IntentTag.PROTECTION], not as a counterspell.
 *
 * Read as [IntentTag.COUNTERSPELL], the playout policy marked the creature a dead card whenever the
 * stack was empty (its main-phase cast), and its board value saw only a 5/3 flier.
 */
class SelfShieldingCounterReadingAiTest : ScenarioTestBase() {

    private fun intent(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!)

    init {
        test("countering whatever targets it is protection, not a counterspell") {
            val tags = intent("Glyph Keeper").tags
            tags shouldContain IntentTag.PROTECTION
            tags shouldNotContain IntentTag.COUNTERSPELL
        }

        test("a real counterspell keeps its tag") {
            intent("Counterspell").tags shouldContain IntentTag.COUNTERSPELL
        }

        test("the self-shield is priced in the creature's prior") {
            intent("Glyph Keeper").staticPriorValue shouldBeGreaterThan CardIntent.UNKNOWN.staticPriorValue
        }
    }
}
