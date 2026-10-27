package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tundra Fumarole (KHM #156).
 *
 *   Tundra Fumarole deals 4 damage to target creature or planeswalker. Add {C} for each {S} spent
 *   to cast this spell. Until end of turn, you don't lose this mana as steps and phases end.
 *
 * The colourless amount is linked to the cast's payment: three Snow-Covered Mountains give {C}{C}{C}
 * that survives into combat; plain Mountains give nothing.
 */
class TundraFumaroleScenarioTest : ScenarioTestBase() {

    private fun TestGame.keptColorless(): Int =
        state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana
            .count { it.color == null && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }

    init {
        context("Tundra Fumarole") {

            test("snow mana spent adds that much colourless mana kept through the turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tundra Fumarole")
                    .withLandsOnBattlefield(1, "Snow-Covered Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Tundra Fumarole", giant).error shouldBe null
                game.resolveStack()

                withClue("4 damage kills the 3/3") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                withClue("three {S} spent adds {C}{C}{C}") { game.keptColorless() shouldBe 3 }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the mana is not lost as steps and phases end") { game.keptColorless() shouldBe 3 }
            }

            test("without snow mana it only deals the damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tundra Fumarole")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Tundra Fumarole", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
            }
        }
    }
}
