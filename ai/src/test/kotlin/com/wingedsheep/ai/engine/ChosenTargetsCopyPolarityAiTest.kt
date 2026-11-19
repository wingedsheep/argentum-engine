package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * "Any number of target creatures you control each become a copy of that creature until end of
 * turn" (Polymorphous Rush): the pipeline gathers its chosen targets and loops over them, each
 * becoming the copy as `IterationEntity`, which names no slot by itself. Gathered unnarrowed from
 * the chosen targets, the loop reaches every target, so each permanent slot reads as a declared
 * copy's `affected` slot does: either way — a weak body made big, not a big one made small, is
 * the board's question for the simulation, not "our best first".
 */
class ChosenTargetsCopyPolarityAiTest : ScenarioTestBase() {
    init {
        test("the targets a gathered chosen-targets loop turns into copies read either way") {
            val rush = cardRegistry.requireCard("Polymorphous Rush").script
            TargetPolarityAnalyzer.forRequirements(rush.spellEffect!!, rush.targetRequirements) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
        }
    }
}
