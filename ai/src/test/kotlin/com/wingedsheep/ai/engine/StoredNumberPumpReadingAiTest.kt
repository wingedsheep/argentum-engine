package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * "The chosen creatures get +X/+X, where X is the difference between their powers" stores X once and
 * pumps by the stored number: read through the variable, it is a pump of a never-negative amount.
 */
class StoredNumberPumpReadingAiTest : ScenarioTestBase() {
    init {
        test("a pump by a stored never-negative number helps its targets") {
            val spry = cardRegistry.requireCard("Spry and Mighty").script
            TargetPolarityAnalyzer.forRequirements(spry.spellEffect, spry.targetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
        }
    }
}
