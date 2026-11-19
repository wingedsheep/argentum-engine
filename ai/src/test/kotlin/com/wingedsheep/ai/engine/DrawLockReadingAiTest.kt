package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A static that caps how many cards opponents draw each turn — Spirit of the Labyrinth's "each
 * player can't draw more than one card each turn", Narset, Parter of Veils's opponents-only form.
 * Unread, Spirit of the Labyrinth's static was "abilities not recognised".
 */
class DrawLockReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a symmetric or opponents-only draw cap reads as a draw lock") {
            listOf("Spirit of the Labyrinth", "Narset, Parter of Veils").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.DRAW_LOCK }
            }
        }

        test("a cap over its controller alone is a price, not a lock") {
            CardIntentAnalyzer.staticTags(RestrictCardsDrawnPerTurn(affected = Player.You)) shouldNotContain
                IntentTag.DRAW_LOCK
        }

        test("the symmetric lock is not read as aimed at the opponent") {
            CardIntentAnalyzer.analyze(card("Spirit of the Labyrinth")).affectsOpponent shouldBe false
        }

        test("the draft heuristic gives the lock a static term") {
            val terms = LimitedCardRater.explain(card("Spirit of the Labyrinth")).heuristic
            withClue("terms: $terms") {
                terms.filter { it.part.startsWith("Static") }.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
