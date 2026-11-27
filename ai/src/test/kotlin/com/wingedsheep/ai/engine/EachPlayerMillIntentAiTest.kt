package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Each player mills five cards" (Chill of Foreboding): a mill inside a loop over every player,
 * whose `You` is the player visited, reaches each opponent's library — read as [IntentTag.MILL].
 */
class EachPlayerMillIntentAiTest : ScenarioTestBase() {
    init {
        test("each player milling reads as a mill of the opponents") {
            val card = cardRegistry.getCard("Chill of Foreboding")!!
            CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.MILL
            AiReadingAudit.read(card).readFully shouldBe true
        }
    }
}
