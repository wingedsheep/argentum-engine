package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Makindi Ox (ZNR #25) — {4}{W} Creature — Ox, 4/4.
 *
 *   Landfall — Whenever a land you control enters, tap target creature an opponent controls.
 */
class MakindiOxScenarioTest : ScenarioTestBase() {

    init {
        context("Makindi Ox's landfall trigger") {

            test("playing a land taps target creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Makindi Ox")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withCardInHand(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val plains = game.findCardsInHand(1, "Plains").single()

                game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(giant)).error shouldBe null
                }
                game.resolveStack()

                withClue("the opponent's creature is tapped; your own is not") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
