package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan

/**
 * An iteration over opponents only whose body is a gain — "each opponent may search their library
 * for a basic land card, put it onto the battlefield tapped" (Old-Growth Dryads) — is the card's
 * drawback: the body's `you` is the opponent visited. Read as our own tutor/ramp, the Dryads were
 * drafted at 4.0 as if the ETB were a payoff. What such an iteration takes from them (Rank Rat's
 * discard) stays read.
 */
class OpponentGiftIterationReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an opponent's search under 'each opponent' is not our tutor or ramp") {
            val tags = CardIntentAnalyzer.analyze(card("Old-Growth Dryads")).tags
            tags shouldNotContain IntentTag.TUTOR
            tags shouldNotContain IntentTag.RAMP
            withClue("Rank Rat: what an opponent iteration takes from them stays read") {
                CardIntentAnalyzer.analyze(card("Rank Rat")).tags shouldContain IntentTag.DISCARD
            }
        }

        test("the rater prices an opponent's gain as a drawback, and the pick drops") {
            val dryads = card("Old-Growth Dryads")
            val trigger = dryads.script.triggeredAbilities.single()
            LimitedCardRater.effectValue(trigger.effect) shouldBeLessThan 0.0
            withClue("a 1-mana 3/3 with a drawback is no 4.0 pick") {
                LimitedCardRater.rate(dryads) shouldBeLessThan 4.0
            }
        }

        test("a gain the body hands to the source's controller by name stays ours") {
            withClue("Adeline: 'for each opponent, create a 1/1' under ControllerOfSource makes our tokens") {
                CardIntentAnalyzer.analyze(card("Adeline, Resplendent Cathar")).tags shouldContain IntentTag.TOKEN_MAKER
                val trigger = card("Adeline, Resplendent Cathar").script.triggeredAbilities.single()
                LimitedCardRater.effectValue(trigger.effect) shouldBeGreaterThan 0.0
            }
            withClue("Ainok Strike Leader: the same per-opponent token is ours") {
                CardIntentAnalyzer.analyze(card("Ainok Strike Leader")).tags shouldContain IntentTag.TOKEN_MAKER
            }
            withClue("Garruk, Veiled Butcher's −3: 'you draw a card' for each opponent stays our draw") {
                val minusThree = card("Garruk, Veiled Butcher").script.activatedAbilities
                    .single { it.description.startsWith("Each opponent discards two cards") }
                CardIntentAnalyzer.analyzeEffect(minusThree.effect).tags shouldContain IntentTag.DRAW
            }
        }
    }
}
