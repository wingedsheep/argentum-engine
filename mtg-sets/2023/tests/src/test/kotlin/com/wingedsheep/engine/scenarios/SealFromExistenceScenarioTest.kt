package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Seal from Existence. */
class SealFromExistenceScenarioTest : ScenarioTestBase() {

    init {
        context("Seal from Existence") {
            test("exiles an opponent's nonland permanent until it leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Seal from Existence")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Seal from Existence")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val selected = game.selectTargets(listOf(victim))
                withClue("ETB target selection should succeed: ${selected.error}") {
                    selected.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should be exiled while Seal from Existence is in play") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }

                // Its own controller targeting it does not trigger ward (opponents only).
                val seal = game.findPermanent("Seal from Existence")!!
                val dis = game.castSpell(1, "Disenchant", seal)
                withClue("Disenchant should be castable: ${dis.error}") { dis.error shouldBe null }
                game.resolveStack()

                withClue("Seal from Existence should be destroyed") {
                    game.isOnBattlefield("Seal from Existence") shouldBe false
                }
                withClue("Hill Giant should return when Seal from Existence leaves") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
