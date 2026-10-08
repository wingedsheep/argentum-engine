package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Toph, Earthbending Master — "Landfall — Whenever a land you control enters, you get an experience
 * counter. Whenever you attack, earthbend X, where X is the number of experience counters you have."
 *
 * Pins the landfall → player experience counter (CR 122.1) and that the attack trigger earthbends
 * for the player's experience total, counted as it resolves.
 */
class TophEarthbendingMasterScenarioTest : ScenarioTestBase() {

    init {
        test("landfall gives experience, and attacking earthbends X = experience counters") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Toph, Earthbending Master")
                .withCardInHand(1, "Forest")
                .withCardOnBattlefield(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val play = game.getLegalActions(1).first { it.actionType == "PlayLand" }
            game.execute(play.action).error shouldBe null
            game.resolveStack()

            val p1 = game.player1Id
            withClue("the land entering gave P1 one experience counter") {
                game.state.getEntity(p1)?.get<CountersComponent>()?.getCount(CounterType.EXPERIENCE) shouldBe 1
            }

            val plains = game.findPermanent("Plains")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Toph, Earthbending Master" to 2)).error shouldBe null
            if (game.hasPendingDecision()) game.selectTargets(listOf(plains))
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the targeted land is now a 1/1 land creature with haste (X = 1)") {
                projected.isCreature(plains) shouldBe true
                projected.getPower(plains) shouldBe 1
                projected.getToughness(plains) shouldBe 1
                projected.hasKeyword(plains, Keyword.HASTE) shouldBe true
            }
        }
    }
}
