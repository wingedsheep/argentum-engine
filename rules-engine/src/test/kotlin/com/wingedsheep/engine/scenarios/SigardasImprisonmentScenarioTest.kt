package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sigarda's Imprisonment (VOW #35) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 *   {4}{W}: Exile enchanted creature. Create a Blood token.
 *
 * Exercises the Pacifism-style "can't attack or block" static abilities and the activated
 * ability that exiles the enchanted creature and creates a Blood token.
 */
class SigardasImprisonmentScenarioTest : ScenarioTestBase() {

    private val exileAbilityId =
        cardRegistry.getCard("Sigarda's Imprisonment")!!.activatedAbilities.first().id

    init {
        context("Sigarda's Imprisonment") {

            test("enchanted creature can't attack or block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sigarda's Imprisonment")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Sigarda's Imprisonment", targetId = bears)
                withClue("Casting Sigarda's Imprisonment should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Sigarda's Imprisonment is on the battlefield") {
                    game.isOnBattlefield("Sigarda's Imprisonment") shouldBe true
                }
                withClue("the enchanted creature can't attack") {
                    game.state.projectedState.cantAttack(bears) shouldBe true
                }
                withClue("the enchanted creature can't block") {
                    game.state.projectedState.cantBlock(bears) shouldBe true
                }
            }

            test("{4}{W}: exiles the enchanted creature and creates a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sigarda's Imprisonment")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Sigarda's Imprisonment", targetId = bears)
                game.resolveStack()

                val aura = game.findPermanent("Sigarda's Imprisonment")
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
                withClue("a Blood token is created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
