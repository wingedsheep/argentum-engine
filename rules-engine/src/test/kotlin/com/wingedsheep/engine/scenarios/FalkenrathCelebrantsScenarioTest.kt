package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Falkenrath Celebrants (VOW #306) — {4}{R} Creature — Vampire, 4/4.
 *
 *   Menace
 *   When this creature enters, create two Blood tokens.
 *
 * Exercises the ETB Blood-token creation (two tokens) and confirms the Menace keyword is present.
 */
class FalkenrathCelebrantsScenarioTest : ScenarioTestBase() {

    init {
        context("Falkenrath Celebrants") {

            test("entering the battlefield creates two Blood tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Falkenrath Celebrants")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Falkenrath Celebrants").error shouldBe null
                game.resolveStack()

                withClue("two Blood tokens are created on entering the battlefield") {
                    game.findPermanents("Blood").size shouldBe 2
                }

                val celebrants = game.findPermanent("Falkenrath Celebrants")!!
                withClue("Falkenrath Celebrants has Menace") {
                    game.state.projectedState.hasKeyword(celebrants, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
