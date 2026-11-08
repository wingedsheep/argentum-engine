package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * "Draw cards equal to the damage dealt to target opponent this turn" — Knollspine Dragon's enters
 * trigger. The controller draws; the targeted player only sizes the draw. Unread, the target slot
 * had no leaf aimed at it and read [TargetPolarity.UNKNOWN] ("can't tell if this helps or hurts").
 */
class DrawSizedByTargetReadingAiTest : ScenarioTestBase() {

    init {
        test("a draw sized by the targeted player's damage taken keeps the slot open instead of unread") {
            val trigger = cardRegistry.getCard("Knollspine Dragon")!!.script.triggeredAbilities.single()

            TargetPolarityAnalyzer.forRequirements(
                trigger.effect,
                listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements,
            ) shouldBe listOf(TargetPolarity.FLEXIBLE)
        }
    }
}
