package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Trained Condor (M14 #76) — "Whenever this creature attacks, another target creature you control
 * gains flying until end of turn."
 */
class TrainedCondorScenarioTest : ScenarioTestBase() {

    init {
        context("Trained Condor") {
            test("attacking gives another creature you control flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Trained Condor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Trained Condor" to 2, "Grizzly Bears" to 2)).error shouldBe null

                val td = game.getPendingDecision()
                td.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("another creature you control: not the Condor, not the opponent's Giant") {
                    td.legalTargets[0]!!.shouldContainExactlyInAnyOrder(bears)
                }
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(bears)))).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }
        }
    }
}
