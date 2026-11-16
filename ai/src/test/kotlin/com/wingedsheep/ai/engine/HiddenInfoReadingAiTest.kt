package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A static that only shows its controller hidden cards — Keeper of the Lens's "you may look at
 * face-down creatures you don't control any time", Lens of Clarity's top-of-library peek. Unread,
 * each was "abilities not recognised" and Keeper of the Lens read as a vanilla 1/2.
 */
class HiddenInfoReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a look-at-hidden-cards static reads as hidden information") {
            listOf("Keeper of the Lens", "Lens of Clarity", "Found Footage").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.HIDDEN_INFO }
            }
        }

        test("Keeper of the Lens is no longer flagged 'abilities not recognised'") {
            AiCardRating.of(card("Keeper of the Lens"), listOf("DTK")).intent.unrecognised shouldBe false
        }
    }
}
