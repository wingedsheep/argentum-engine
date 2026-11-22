package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "That player reveals a card at random from their hand" (Merfolk Spy): another player's hand card
 * shown and moved nowhere is information — [IntentTag.HIDDEN_INFO] — not an ability the AI can't
 * place, and never the hand disruption a discard is.
 */
class OpponentHandRevealReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("revealing a random card from the damaged player's hand reads as hidden information") {
            val tags = intents.forName("Merfolk Spy")!!.tags
            withClue("Merfolk Spy's tags: $tags") {
                (IntentTag.HIDDEN_INFO in tags) shouldBe true
                (IntentTag.DISCARD in tags) shouldBe false
            }
        }
    }
}
