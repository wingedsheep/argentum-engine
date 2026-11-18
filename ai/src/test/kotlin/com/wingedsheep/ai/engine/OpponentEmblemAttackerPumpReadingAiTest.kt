package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Inside an iteration over the targeted opponent `you` is that opponent, so an emblem made there
 * whose trigger pumps "a creature [that] attacks you" makes every attack on them hit harder:
 * Garruk, Apex Predator's −8 ("target opponent gets an emblem with 'Whenever a creature attacks
 * you, it gets +5/+5 and gains trample until end of turn'") is aimed at the opponent's side.
 */
class OpponentEmblemAttackerPumpReadingAiTest : ScenarioTestBase() {

    init {
        test("the targeted opponent who gets the attacker-pumping emblem is a harmful slot") {
            val garruk = cardRegistry.getCard("Garruk, Apex Predator") ?: error("Garruk is not registered")
            val minusEight = garruk.script.activatedAbilities
                .single { it.description.startsWith("Target opponent gets an emblem") }
            TargetPolarityAnalyzer.forRequirements(minusEight.effect, minusEight.targetRequirements) shouldBe
                listOf(TargetPolarity.HARMFUL)
        }

        test("the emblem's pump on creatures attacking that opponent reads as our pump, not a gift to them") {
            val garruk = cardRegistry.getCard("Garruk, Apex Predator") ?: error("Garruk is not registered")
            CardIntentAnalyzer.analyze(garruk).tags shouldContain IntentTag.PUMP
        }
    }
}
