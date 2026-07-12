package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for End the Festivities (VOW #155) — {R} Sorcery.
 *
 *   End the Festivities deals 1 damage to each opponent and each creature and planeswalker they
 *   control.
 *
 * Exercises the mass 1-damage sweep: the opponent takes 1 (life 20 -> 19), each of their creatures
 * takes 1 marked damage (a 1-toughness creature dies; a tougher one survives), and the caster's own
 * board is untouched.
 */
class EndTheFestivitiesScenarioTest : ScenarioTestBase() {

    init {
        context("End the Festivities — 1 damage to each opponent and their permanents") {

            test("opponent loses 1 life, their 1/1 dies, their 2/2 survives with 1 damage, own board untouched") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "End the Festivities")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    // Player1's own creature — must NOT be damaged.
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    // Opponent's board: a 1/1 that dies and a 2/2 that survives with 1 marked damage.
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownBears = game.findPermanents("Grizzly Bears")
                    .first { game.state.getBattlefield(game.player1Id).contains(it) }
                val opponentBears = game.findPermanents("Grizzly Bears")
                    .first { game.state.getBattlefield(game.player2Id).contains(it) }

                game.castSpell(1, "End the Festivities").error shouldBe null
                game.resolveStack()

                withClue("Player 2 (opponent) takes 1 damage (20 -> 19)") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("Opponent's 1/1 Savannah Lions is destroyed by 1 damage") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isInGraveyard(2, "Savannah Lions") shouldBe true
                }
                withClue("Opponent's 2/2 Grizzly Bears survives with 1 marked damage") {
                    game.state.getBattlefield(game.player2Id).contains(opponentBears) shouldBe true
                    val opponentDamage = game.state.getEntity(opponentBears)?.get<DamageComponent>()?.amount ?: 0
                    opponentDamage shouldBe 1
                }
                withClue("Player 1's own Grizzly Bears is untouched (an opponent's permanents only)") {
                    val ownDamage = game.state.getEntity(ownBears)?.get<DamageComponent>()?.amount ?: 0
                    ownDamage shouldBe 0
                }
            }
        }
    }
}
