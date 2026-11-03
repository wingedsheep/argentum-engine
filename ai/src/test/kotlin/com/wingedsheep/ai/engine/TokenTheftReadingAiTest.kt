package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A granted token-controller redirect — Crafty Cutpurse's "each token that would be created under
 * an opponent's control this turn is created under your control instead". Unread, its enters
 * trigger was "abilities not recognised" and the draft heuristic priced the card as a vanilla 2/2
 * with flash.
 */
class TokenTheftReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("Crafty Cutpurse's enters trigger reads as taking opponents' tokens, not as making tokens") {
            val tags = CardIntentAnalyzer.analyze(card("Crafty Cutpurse")).tags
            tags shouldContain IntentTag.TOKEN_THEFT
            withClue("it makes nothing on its own") { tags shouldNotContain IntentTag.TOKEN_MAKER }
        }

        test("it has no target, so it is not read as aimed at the opponent") {
            CardIntentAnalyzer.analyze(card("Crafty Cutpurse")).affectsOpponent shouldBe false
        }

        test("the draft heuristic prices the theft above the bare body") {
            val terms = LimitedCardRater.explain(card("Crafty Cutpurse")).heuristic
            val triggerTerms = terms.filter { it.part.startsWith("Trigger") }
            withClue("terms: $terms") {
                triggerTerms.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
