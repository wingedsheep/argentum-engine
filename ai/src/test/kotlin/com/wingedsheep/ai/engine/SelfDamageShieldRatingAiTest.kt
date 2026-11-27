package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A creature's standing "prevent all damage that would be dealt to this creature" — Gloom Surgeon's
 * combat-only form. Unread, the draft heuristic priced Gloom Surgeon as a vanilla 2/1 for two. The
 * shield is part of its body, so it is not read as protecting the rest of its controller's side.
 */
class SelfDamageShieldRatingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("the draft heuristic gives Gloom Surgeon's self shield a replacement term") {
            val terms = LimitedCardRater.explain(card("Gloom Surgeon")).heuristic
            val replacement = terms.filter { it.part.startsWith("Replacement effect") }
            withClue("terms: $terms") {
                replacement.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }

        test("a shield over itself alone is not read as protecting its controller's side") {
            CardIntentAnalyzer.analyze(card("Gloom Surgeon")).tags shouldNotContain IntentTag.PROTECTION
        }

        test("a partial self shield earns no term") {
            // Callous Giant: "If a source would deal 3 or less damage to this creature, prevent that damage."
            val terms = LimitedCardRater.explain(card("Callous Giant")).heuristic
            terms.filter { it.part.startsWith("Replacement effect") }.sumOf { it.value } shouldBe 0.0
        }
    }
}
