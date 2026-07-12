package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Selhoff Entomber (VOW #76) — {1}{U} Creature — Zombie, 1/3.
 *
 *   {T}, Discard a creature card: Draw a card.
 *
 * Exercises the tap + discard-a-creature-card cost paying off with a card draw.
 */
class SelhoffEntomberScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Selhoff Entomber")!!.activatedAbilities.first().id

    init {
        context("Selhoff Entomber") {

            test("tapping and discarding a creature card draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Selhoff Entomber", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val entomber = game.findPermanent("Selhoff Entomber")!!
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                val handBefore = game.handSize(1)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = entomber,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
                    )
                )
                withClue("Activating Selhoff Entomber should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears was discarded to the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("hand size is unchanged (discarded one, drew one)") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("a new card (Plains) was drawn from the library") {
                    game.isInHand(1, "Plains") shouldBe true
                }
            }
        }
    }
}
