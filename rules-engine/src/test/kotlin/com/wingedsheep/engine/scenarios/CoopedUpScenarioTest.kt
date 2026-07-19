package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Cooped Up (WOE #8) — {1}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 *   {2}{W}: Exile enchanted creature.
 */
class CoopedUpScenarioTest : ScenarioTestBase() {

    private val exileAbilityId =
        cardRegistry.getCard("Cooped Up")!!.activatedAbilities.first().id

    init {
        context("Cooped Up") {

            test("enchanted creature can't attack or block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cooped Up")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Cooped Up", targetId = bears)
                withClue("Casting Cooped Up should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Cooped Up is on the battlefield") {
                    game.isOnBattlefield("Cooped Up") shouldBe true
                }
                withClue("the enchanted creature can't attack") {
                    game.state.projectedState.cantAttack(bears) shouldBe true
                }
                withClue("the enchanted creature can't block") {
                    game.state.projectedState.cantBlock(bears) shouldBe true
                }
            }

            test("{2}{W}: exiles the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cooped Up")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Cooped Up", targetId = bears)
                game.resolveStack()

                val aura = game.findPermanent("Cooped Up")
                withClue("Aura should be on the battlefield before activation") {
                    aura.shouldNotBeNull()
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura!!,
                        abilityId = exileAbilityId,
                    )
                )
                withClue("Activating the exile ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the enchanted creature is exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.state.getExile(game.player2Id).contains(bears) shouldBe true
                }
            }
        }
    }
}
