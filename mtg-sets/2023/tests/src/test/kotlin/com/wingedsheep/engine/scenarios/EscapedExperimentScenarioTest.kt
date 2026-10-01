package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Escaped Experiment (ONE #48) — "Whenever this creature attacks, target creature an opponent
 * controls gets -X/-0 until end of turn, where X is the number of artifacts you control."
 */
class EscapedExperimentScenarioTest : ScenarioTestBase() {

    init {
        test("attack trigger gives an opponent's creature -X/-0, X counting only your artifacts") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Escaped Experiment")
                .withCardOnBattlefield(1, "Chrome Prowler")      // your second artifact
                .withCardOnBattlefield(2, "Chrome Prowler")      // opponent's artifact — not counted
                .withCardOnBattlefield(2, "Hill Giant")          // 3/3
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val experiment = game.findPermanent("Escaped Experiment")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Escaped Experiment" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) game.resolveStack()
            val td = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a target prompt; got ${game.state.pendingDecision}")

            withClue("only creatures an opponent controls are legal") {
                td.legalTargets[0]!! shouldContain giant
                td.legalTargets[0]!! shouldNotContain experiment
            }

            game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(giant)))).error shouldBe null
            game.resolveStack()

            withClue("two artifacts you control: Hill Giant becomes 1/3") {
                game.state.projectedState.getPower(giant) shouldBe 1
                game.state.projectedState.getToughness(giant) shouldBe 3
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the penalty ends with the turn") {
                game.state.projectedState.getPower(giant) shouldBe 3
            }
        }
    }
}
