package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gollum, Silent Slinker // Meager Meal (HOB #71).
 *
 * The Adventure takes two targets of different kinds — a required "target player" and "up to one
 * target creature" — so the risk is the two getting crossed, or the optional one swallowing the
 * required one when it is declined. These tests pin that both land where they belong, and that
 * declining the creature still gains the life.
 */
class GollumSilentSlinkerScenarioTest : ScenarioTestBase() {

    init {
        context("Meager Meal (the Adventure)") {

            test("puts the counter on the chosen creature and gains the chosen player 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gollum, Silent Slinker")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val gollum = game.findCardsInHand(1, "Gollum, Silent Slinker").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = gollum,
                        faceIndex = 0, // the Adventure face
                        // Required player target first, optional creature second — see the card's
                        // KDoc for why the slot order is the reverse of the oracle sentence.
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Permanent(bears)
                        )
                    )
                )
                withClue("Casting Meager Meal should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the creature target got the +1/+1 counter") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
                withClue("the player target gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("the Adventure exiles itself rather than going to the graveyard") {
                    game.isInExile(1, "Gollum, Silent Slinker") shouldBe true
                }
            }

            test("the creature is 'up to one' — declining it still gains the life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gollum, Silent Slinker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gollum = game.findCardsInHand(1, "Gollum, Silent Slinker").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = gollum,
                        faceIndex = 0,
                        // No creature on the battlefield at all — only the required player target.
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Meager Meal is castable with no creature to counter: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the chosen player still gains 2 life") {
                    game.getLifeTotal(2) shouldBe 22
                }
            }
        }
    }
}
