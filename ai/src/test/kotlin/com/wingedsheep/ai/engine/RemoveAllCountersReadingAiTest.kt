package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import io.kotest.matchers.shouldBe

/**
 * "Remove all counters from target creature" strips an opponent's +1/+1 counters or frees our own
 * creature of its -1/-1 counters (blight): which side it helps is the board's question, so the slot
 * reads as known either-way rather than unread.
 */
class RemoveAllCountersReadingAiTest : ScenarioTestBase() {
    init {
        test("stripping every counter off a creature is either-way: the board decides") {
            val modal = cardRegistry.requireCard("Perfect Intimidation").script.spellEffect as ModalEffect
            TargetPolarityAnalyzer.forMode(modal.modes[1]) shouldBe listOf(TargetPolarity.FLEXIBLE)
        }
    }
}
