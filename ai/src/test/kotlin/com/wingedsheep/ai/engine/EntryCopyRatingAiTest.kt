package com.wingedsheep.ai.engine

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * An as-enters copy (Clone's "you may have this creature enter as a copy of any creature on the
 * battlefield", Protean Raider's raid-gated one) is most of the card's value, but the draft heuristic
 * only saw the printed body: Protean Raider rated as a vanilla 2/2 for three.
 */
class EntryCopyRatingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun copyTerm(name: String) =
        LimitedCardRater.explain(card(name)).heuristic.singleOrNull { it.part.startsWith("Replacement effect") }

    init {
        test("the heuristic prices an entry copy, a raid-gated one at half") {
            val clone = copyTerm("Clone")
            val raider = copyTerm("Protean Raider")
            withClue("Clone: $clone, Protean Raider: $raider") {
                clone!!.value shouldBeGreaterThan 0.0
                raider!!.value shouldBe clone.value / 2
            }
        }

        test("a copy of a land isn't priced as a creature copy") {
            withClue("Echoing Deeps copies a land card") { copyTerm("Echoing Deeps") shouldBe null }
        }
    }
}
