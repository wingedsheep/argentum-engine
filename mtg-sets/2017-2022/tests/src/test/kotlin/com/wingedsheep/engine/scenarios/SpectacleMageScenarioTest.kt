package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spectacle Mage (STX #235) — "Instant and sorcery spells you cast with mana value 5 or greater
 * cost {1} less to cast." Ruling: an {X} in the spell's mana cost counts at its chosen value.
 */
class SpectacleMageScenarioTest : ScenarioTestBase() {

    init {
        context("Spectacle Mage") {

            fun board(mountains: Int) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Spectacle Mage")
                .withCardInHand(1, "Blaze")
                .withLandsOnBattlefield(1, "Mountain", mountains)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castBlaze(x: Int) = execute(
                CastSpell(
                    player1Id,
                    findCardsInHand(1, "Blaze").first(),
                    listOf(ChosenTarget.Player(player2Id)),
                    xValue = x,
                )
            )

            test("Blaze with X = 4 has mana value 5 and costs {1} less: four Mountains pay it") {
                val game = board(mountains = 4)
                val offer = game.getLegalActions(1).first {
                    it.actionType == "CastSpell" && it.description.contains("Blaze")
                }
                withClue("four Mountains reach X = 4 with the discount") { offer.maxAffordableX shouldBe 4 }

                val cast = game.castBlaze(4)
                withClue("Blaze for X = 4 should cost {3}{R}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 16
            }

            test("Blaze with X = 3 has mana value 4 and pays full price") {
                val game = board(mountains = 3)
                withClue("{3}{R} is out of reach of three Mountains") { game.castBlaze(3).error shouldNotBe null }
                game.castBlaze(2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
