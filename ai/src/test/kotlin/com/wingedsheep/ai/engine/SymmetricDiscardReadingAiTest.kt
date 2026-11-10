package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Each player discards a card" with no draw beside it (Miasmic Mummy's enters trigger) is the
 * discard deck's parity attack: every opponent loses a card too, so it reads as [IntentTag.DISCARD].
 * Written as a loop that collects each player's pick and discards them all after it (Creeping Dread),
 * it reads the same. A wheel — the same discard followed by a refill (Windfall) — pays its discard as
 * the price of the draw and stays unread as discard.
 */
class SymmetricDiscardReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a symmetric each-player discard reads as hand disruption") {
            val mummy = card("Miasmic Mummy")
            CardIntentAnalyzer.analyze(mummy).tags shouldContain IntentTag.DISCARD
            withClue("Enemy of Enlightenment's upkeep trigger has the same shape") {
                CardIntentAnalyzer.analyze(card("Enemy of Enlightenment")).tags shouldContain IntentTag.DISCARD
            }
        }

        test("each player picking a card in turn, then all discarding at once, reads as hand disruption") {
            // Creeping Dread: the picks are collected out of an each-player loop and discarded together
            // after it, so the move to the graveyard sits outside the loop that gathered each hand.
            CardIntentAnalyzer.analyze(card("Creeping Dread")).tags shouldContain IntentTag.DISCARD
            withClue("Liliana, Waker of the Dead's +1 collects and discards the same way") {
                CardIntentAnalyzer.analyze(card("Liliana, Waker of the Dead")).tags shouldContain IntentTag.DISCARD
            }
        }

        test("a wheel's discard is the price of its draw, not an attack") {
            CardIntentAnalyzer.analyze(card("Windfall")).tags shouldNotContain IntentTag.DISCARD
        }
    }
}
