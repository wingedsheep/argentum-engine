package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "Creatures your opponents control lose hexproof and can't have or gain hexproof" (the Born of the
 * Gods Archetypes) strips a keyword from the whole opposing board. Unread, the draft heuristic priced
 * Archetype of Endurance's second static at zero.
 */
class KeywordProhibitionReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("an opponents-scope keyword removal reads as a combat enhancement; a self-scope one doesn't") {
            val (grant, prohibition) = card("Archetype of Endurance").script.staticAbilities
            (IntentTag.COMBAT_ENHANCEMENT in CardIntentAnalyzer.staticTags(prohibition)) shouldBe true
            (IntentTag.COMBAT_ENHANCEMENT in CardIntentAnalyzer.staticTags(grant)) shouldBe false
            val gravity = card("Gravity Sphere").script.staticAbilities.single()
            (IntentTag.COMBAT_ENHANCEMENT in CardIntentAnalyzer.staticTags(gravity)) shouldBe false
        }

        test("the draft heuristic prices the prohibition static") {
            val terms = LimitedCardRater.explain(card("Archetype of Endurance")).heuristic
            withClue("terms: $terms") {
                terms.single { it.part == "Static ability 2" }.value shouldBeGreaterThan 0.0
            }
        }
    }
}
