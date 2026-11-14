package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * A replacement that locks out the opponents' life gain — Tainted Remedy's "if an opponent would
 * gain life, that player loses that much life instead", Archfiend of Despair's "your opponents
 * can't gain life". Unread, Tainted Remedy was "nothing to read" and sat on the board at the flat
 * value of a permanent nothing reads.
 */
class LifeGainLockReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("an opponents-only life-gain lock reads as a lifegain lock") {
            listOf("Tainted Remedy", "Archfiend of Despair").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.LIFEGAIN_LOCK }
            }
        }

        test("Tainted Remedy's replacement drains the opponent who would have gained") {
            CardIntentAnalyzer.analyze(card("Tainted Remedy")).tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
        }

        test("a lock over its controller's own life gain, or a symmetric one, is no lock of ours") {
            listOf("Lich", "Sulfuric Vortex").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldNotContain IntentTag.LIFEGAIN_LOCK }
            }
        }

        test("in play, Tainted Remedy is priced above a permanent nothing reads") {
            CardIntentAnalyzer.analyzeInPlay(card("Tainted Remedy")).staticPriorValue shouldBeGreaterThan
                CardIntent.UNKNOWN.staticPriorValue
        }

        test("the draft heuristic gives the lock a replacement term") {
            val terms = LimitedCardRater.explain(card("Tainted Remedy")).heuristic
            withClue("terms: $terms") {
                terms.filter { it.part.startsWith("Replacement effect") }.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
