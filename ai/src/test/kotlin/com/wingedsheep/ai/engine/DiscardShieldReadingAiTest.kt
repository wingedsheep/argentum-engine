package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.OptionalEffectDiscardDestination
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * An effect-discard destination replacement over its controller's hand — Library of Leng's "if an
 * effect causes you to discard a card, … you may put it on top of your library instead", Nephalia
 * Academy's opponent-caused, revealed form. Unread, Library of Leng's replacement was "abilities not
 * recognised" and the draft heuristic gave it no term.
 */
class DiscardShieldReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("Library of Leng and Nephalia Academy read as shielding their controller's hand") {
            CardIntentAnalyzer.analyze(card("Library of Leng")).tags shouldContain IntentTag.DISCARD_SHIELD
            CardIntentAnalyzer.analyze(card("Nephalia Academy")).tags shouldContain IntentTag.DISCARD_SHIELD
        }

        test("a destination replacement over opponents' discards is no shield of ours") {
            val opponents = card("Discard Shield Test Opponents") {
                typeLine = "Artifact"
                replacementEffect(OptionalEffectDiscardDestination(CardDestination.ToZone(Zone.LIBRARY),
                    EventPattern.DiscardEvent(Player.EachOpponent)))
            }
            CardIntentAnalyzer.analyze(opponents).tags shouldNotContain IntentTag.DISCARD_SHIELD
        }

        test("the shield has no target, so it is not read as aimed at the opponent") {
            CardIntentAnalyzer.analyze(card("Nephalia Academy")).affectsOpponent shouldBe false
        }

        test("the draft heuristic gives the shield a small replacement term") {
            val terms = LimitedCardRater.explain(card("Library of Leng")).heuristic
            val replacement = terms.filter { it.part.startsWith("Replacement effect") }
            withClue("terms: $terms") {
                replacement.sumOf { it.value } shouldBeGreaterThan 0.0
            }
        }
    }
}
