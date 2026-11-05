package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll

/**
 * "Return all non-Elemental creatures to their owners' hands" lowers to a gather of the battlefield
 * moved to hand: the bounce-all a group-targeted bounce already reads as — removal, and a sweeper.
 */
class BounceAllPipelineReadingAiTest : ScenarioTestBase() {
    init {
        test("a gathered group returned to hand reads as a sweeper") {
            for (card in listOf("Sunderflock", "Aetherize", "Hurkyl's Recall")) {
                withClue(card) {
                    CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags shouldContainAll
                        listOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
                }
            }
        }
    }
}
