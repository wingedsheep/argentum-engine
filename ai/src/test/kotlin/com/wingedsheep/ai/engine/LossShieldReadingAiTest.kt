package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * A second life for its controller — Exquisite Archangel's "if you would lose the game, instead
 * exile this creature and your life total becomes equal to your starting life total", Enduring
 * Angel's "if your life total would be reduced to 0 or less, instead …". Unread, both were priced
 * and drafted as their bodies alone.
 */
class LossShieldReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a loss or lethal-life replacement over you reads as protection") {
            listOf("Exquisite Archangel", "Enduring Angel").forEach { name ->
                CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.PROTECTION
            }
        }

        test("the shield is worth more in play than the body that carries it") {
            listOf("Exquisite Archangel", "Enduring Angel").forEach { name ->
                CardIntentAnalyzer.analyze(card(name)).staticPriorValue shouldBeGreaterThan
                    CardIntent.UNKNOWN.staticPriorValue
            }
        }

        test("the draft heuristic prices the replacement, not just the 5/5 flier") {
            val archangel = card("Exquisite Archangel")
            LimitedCardRater.explain(archangel).heuristic.map { it.part } shouldContain "Replacement effect 1"
        }
    }
}
