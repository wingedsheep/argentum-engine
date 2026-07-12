package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Lantern of the Lost (VOW #259) — {1} Artifact.
 *
 *   When this artifact enters, exile target card from a graveyard.
 *   {1}, {T}, Exile this artifact: Exile all cards from all graveyards, then draw a card.
 *
 * Exercises the ETB exile-target-from-graveyard trigger and the activated ability that clears
 * every graveyard and draws a card.
 */
class LanternOfTheLostScenarioTest : ScenarioTestBase() {

    init {
        context("Lantern of the Lost") {

            test("ETB exiles a targeted card from a graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lantern of the Lost")
                    .withLandsOnBattlefield(1, "Plains", 1) // Lantern costs {1}
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

                game.castSpell(1, "Lantern of the Lost").error shouldBe null
                game.resolveStack() // Lantern enters -> ETB trigger asks for a target

                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears is exiled from the graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("Lantern of the Lost resolved onto the battlefield") {
                    game.isOnBattlefield("Lantern of the Lost") shouldBe true
                }
            }

            test("{1}, {T}, exile this: exiles all cards from all graveyards, then draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lantern of the Lost", tapped = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lantern = game.findPermanent("Lantern of the Lost")!!
                val abilityId = cardRegistry.getCard("Lantern of the Lost")!!.activatedAbilities.first().id
                val handSizeBefore = game.handSize(1)

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lantern,
                        abilityId = abilityId
                    )
                )
                withClue("activation should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("both graveyards are emptied") {
                    game.graveyardSize(1) shouldBe 0
                    game.graveyardSize(2) shouldBe 0
                }
                withClue("Lantern of the Lost was exiled as a cost") {
                    game.isOnBattlefield("Lantern of the Lost") shouldBe false
                    game.isInExile(1, "Lantern of the Lost") shouldBe true
                }
                withClue("a card was drawn") {
                    game.handSize(1) shouldBe handSizeBefore + 1
                }
            }
        }
    }
}
