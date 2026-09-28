package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Abyssal Nightstalker (Portal Second Age).
 *
 * Oracle: "Whenever this creature attacks and isn't blocked, defending player discards a card."
 */
class AbyssalNightstalkerScenarioTest : ScenarioTestBase() {

    init {
        context("Abyssal Nightstalker — attacks unblocked, defending player discards") {

            test("the defending player discards a card of their choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Abyssal Nightstalker", summoningSickness = false)
                    .withCardsInHand(1, "Swamp", 4)
                    .withCardsInHand(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Abyssal Nightstalker" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.resolveStack()

                withClue("the defending player chooses their own discard") {
                    game.state.pendingDecision?.playerId shouldBe game.player2Id
                }
                game.selectCards(game.findCardsInHand(2, "Plains").take(1))
                game.resolveStack()

                game.handSize(2) shouldBe 2
                game.handSize(1) shouldBe 4
            }

            test("no discard when it is blocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Abyssal Nightstalker", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardsInHand(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Abyssal Nightstalker" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Abyssal Nightstalker"))).error shouldBe null
                game.resolveStack()

                game.handSize(2) shouldBe 3
            }
        }
    }
}
