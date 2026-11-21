package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Rakdos Drake (DGM #28) — {2}{B} Creature — Drake 1/2, Flying, Unleash.
 *
 * Cast in Player 2's second main phase, so the next combat is Player 1's: unleashed it is a 2/3
 * flier that can't block; declined it stays a 1/2 that blocks.
 */
class RakdosDrakeScenarioTest : ScenarioTestBase() {

    private fun TestGame.castDrake(unleash: Boolean) {
        castSpell(2, "Rakdos Drake").error shouldBe null
        resolveStack()
        val pick = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(pick.id, if (unleash) 0 else 1)).error shouldBe null
    }

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(2, "Rakdos Drake")
        .withLandsOnBattlefield(2, "Swamp", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(2)
        .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        .build()

    init {
        context("Rakdos Drake") {

            test("unleashed, it enters as a 2/3 flier with a counter and can't block") {
                val game = build()
                game.castDrake(unleash = true)
                val drake = game.findPermanent("Rakdos Drake")!!
                game.state.getEntity(drake)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(drake) shouldBe 2
                game.state.projectedState.getToughness(drake) shouldBe 3

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("an unleashed Drake with its counter can't block") {
                    game.declareBlockers(mapOf("Rakdos Drake" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
            }

            test("not unleashed, it stays a 1/2 that can block") {
                val game = build()
                game.castDrake(unleash = false)
                val drake = game.findPermanent("Rakdos Drake")!!
                game.state.projectedState.getPower(drake) shouldBe 1

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Rakdos Drake" to listOf("Grizzly Bears"))).error shouldBe null
            }
        }
    }
}
