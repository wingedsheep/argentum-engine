package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dreadwurm (ZNR #100) — {4}{B} Creature — Wurm Horror, 5/4.
 *
 *   Landfall — Whenever a land you control enters, this creature gains indestructible until end
 *   of turn.
 */
class DreadwurmScenarioTest : ScenarioTestBase() {

    init {
        test("a land entering under your control grants indestructible until end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dreadwurm")
                .withCardInHand(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Dreadwurm")!!
            game.state.projectedState.hasKeyword(wurm, Keyword.INDESTRUCTIBLE.name) shouldBe false

            val swamp = game.findCardsInHand(1, "Swamp").single()
            game.execute(PlayLand(game.player1Id, swamp)).error shouldBe null
            game.resolveStack()

            withClue("landfall grants indestructible") {
                game.state.projectedState.hasKeyword(wurm, Keyword.INDESTRUCTIBLE.name) shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("indestructible lasts only until end of turn") {
                game.state.projectedState.hasKeyword(wurm, Keyword.INDESTRUCTIBLE.name) shouldBe false
            }
        }
    }
}
