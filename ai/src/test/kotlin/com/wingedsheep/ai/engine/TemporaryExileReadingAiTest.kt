package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Exile target creature. At the beginning of the next end step, return that card to the
 * battlefield" (Long Road Home, Otherworldly Journey, Galepowder Mage): the creature comes back, so
 * the exile is never the permanent answer [IntentTag.EXILE_REMOVAL] claims. Aimed at an opponent's
 * creature it takes a blocker away for the turn — tempo [IntentTag.REMOVAL] — and since our own
 * creatures can fill the slot too, it is the slow blink of ours [IntentTag.PROTECTION] reads,
 * which is what Long Road Home is mostly cast for. Confined to our own side (Liberate) it is the
 * blink alone.
 */
class TemporaryExileReadingAiTest : ScenarioTestBase() {

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("an exile that returns at the end step reads as tempo removal and a blink, never exile removal") {
            for (card in listOf("Long Road Home", "Otherworldly Journey", "Galepowder Mage")) {
                withClue(card) {
                    val tags = tags(card)
                    tags shouldContain IntentTag.REMOVAL
                    tags shouldContain IntentTag.PROTECTION
                    tags shouldNotContain IntentTag.EXILE_REMOVAL
                }
            }
            tags("Liberate") shouldContainExactlyInAnyOrder listOf(IntentTag.PROTECTION)
        }
    }
}
