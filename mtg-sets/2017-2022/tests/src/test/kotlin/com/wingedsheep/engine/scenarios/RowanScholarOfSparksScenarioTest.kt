package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Rowan, Scholar of Sparks // Will, Scholar of Frost (STX #156).
 *
 * Pins down:
 *  1. Rowan's +1 deals 1 damage to each opponent, or 3 once you've drawn three or more cards
 *     this turn.
 *  2. Will's −7 exiles the targets together, then each exiled permanent's *last-known
 *     controller* gets a 4/4 Elemental — a stolen creature hands its Elemental to the player who
 *     controlled it when it was exiled, not to its owner.
 */
class RowanScholarOfSparksScenarioTest : ScenarioTestBase() {

    init {
        context("Rowan's +1") {
            fun rowanGame(cardsDrawn: Int) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rowan, Scholar of Sparks")
                .withCardsDrawnThisTurn(1, cardsDrawn)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("deals 1 damage to each opponent with fewer than three cards drawn") {
                val game = rowanGame(cardsDrawn = 2)
                val rowan = game.findPermanent("Rowan, Scholar of Sparks")!!
                val plusOne = cardRegistry.getCard("Rowan, Scholar of Sparks")!!.script.activatedAbilities[0].id

                game.execute(ActivateAbility(game.player1Id, rowan, plusOne)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
            }

            test("deals 3 damage instead once three cards were drawn this turn") {
                val game = rowanGame(cardsDrawn = 3)
                val rowan = game.findPermanent("Rowan, Scholar of Sparks")!!
                val plusOne = cardRegistry.getCard("Rowan, Scholar of Sparks")!!.script.activatedAbilities[0].id

                game.execute(ActivateAbility(game.player1Id, rowan, plusOne)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }
        }

        context("Will's −7") {
            test("each exiled permanent's last-known controller creates a 4/4 Elemental") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Will, Scholar of Frost")
                    .withCardInHand(1, "Caught Red-Handed")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val will = game.findPermanent("Will, Scholar of Frost")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.state = game.state.updateEntity(will) {
                    it.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
                }

                game.castSpell(1, "Caught Red-Handed", bears).error shouldBe null
                game.resolveStack()
                withClue("Player1 has stolen the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                }

                val minusSeven = cardRegistry.getCard("Will, Scholar of Frost")!!.script.activatedAbilities[2].id
                game.execute(
                    ActivateAbility(
                        game.player1Id,
                        will,
                        minusSeven,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true

                val elementals = game.findPermanents("Elemental Token")
                elementals shouldHaveSize 2
                val controllers = elementals.map { game.state.projectedState.getController(it) }
                withClue("the stolen Bears' Elemental goes to Player1, who controlled it when it was exiled") {
                    controllers.count { it == game.player1Id } shouldBe 1
                }
                withClue("the Hill Giant's Elemental goes to Player2") {
                    controllers.count { it == game.player2Id } shouldBe 1
                }
            }
        }
    }
}
