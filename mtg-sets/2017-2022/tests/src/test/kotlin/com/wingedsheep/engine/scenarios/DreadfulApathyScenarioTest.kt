package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dreadful Apathy (THB #11) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 *   {2}{W}: Exile enchanted creature.
 */
class DreadfulApathyScenarioTest : ScenarioTestBase() {

    private val exileAbilityId =
        cardRegistry.getCard("Dreadful Apathy")!!.activatedAbilities.first().id

    init {
        context("Dreadful Apathy") {

            test("enchanted creature can't attack or block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dreadful Apathy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Dreadful Apathy", targetId = bears)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.state.projectedState.cantAttack(bears) shouldBe true
                game.state.projectedState.cantBlock(bears) shouldBe true
            }

            test("{2}{W}: exiles the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dreadful Apathy")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Dreadful Apathy", targetId = bears)
                game.resolveStack()

                val aura = game.findPermanent("Dreadful Apathy")
                aura.shouldNotBeNull()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura,
                        abilityId = exileAbilityId,
                    )
                )
                withClue("Activation should succeed: ${activation.error}") { activation.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.state.getExile(game.player2Id).contains(bears) shouldBe true
            }
        }
    }
}
