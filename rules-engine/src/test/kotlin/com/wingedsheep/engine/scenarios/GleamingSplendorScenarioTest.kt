package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.GleamingSplendor
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gleaming Splendor (HOB #15) —
 *  "Whenever an opponent draws their second card each turn, you create a Treasure token."
 *  "{2}{W}: Two target players each draw a card."
 *
 * The trigger is `NthCardDrawn(2, Player.EachOpponent)`: it reads the per-turn draw counter and
 * fires only on the crossing draw (CR 121.2 — a three-card draw still crosses the second exactly
 * once), and only for players who are not the controller. Covered: an opponent's second draw makes
 * exactly one Treasure, the controller's own second draw makes none, and the activated ability
 * draws one card for each of its two distinct player targets.
 */
class GleamingSplendorScenarioTest : ScenarioTestBase() {

    // A free instant drawing three cards, so one cast spans the turn's first, second and third
    // draws — the second is the only one that crosses the threshold.
    private val drawThree = card("Draw Three Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw three cards."
        spell { effect = Effects.DrawCards(3) }
    }

    init {
        cardRegistry.register(GleamingSplendor)
        cardRegistry.register(drawThree)

        val activateId = GleamingSplendor.script.activatedAbilities.first().id

        context("Gleaming Splendor") {

            test("an opponent's second draw each turn makes exactly one Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gleaming Splendor")
                    .withCardInHand(2, "Draw Three Test")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(2, 0)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.findPermanents("Treasure").size shouldBe 0

                game.castSpell(2, "Draw Three Test").error shouldBe null
                game.resolveStack()

                withClue("only the crossing (second) draw fires it, so three draws make one Treasure") {
                    game.findPermanents("Treasure").size shouldBe 1
                }
            }

            test("the controller's own second draw makes no Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gleaming Splendor")
                    .withCardInHand(1, "Draw Three Test")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Draw Three Test").error shouldBe null
                game.resolveStack()

                withClue("'an opponent draws' excludes the Splendor's own controller") {
                    game.findPermanents("Treasure").size shouldBe 0
                }
            }

            test("{2}{W}: two target players each draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gleaming Splendor")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val splendor = game.findPermanent("Gleaming Splendor")!!
                val handsBefore = game.handSize(1) to game.handSize(2)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = splendor,
                        abilityId = activateId,
                        targets = listOf(
                            ChosenTarget.Player(game.player1Id),
                            ChosenTarget.Player(game.player2Id)
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("each of the two targeted players drew exactly one card") {
                    game.handSize(1) shouldBe handsBefore.first + 1
                    game.handSize(2) shouldBe handsBefore.second + 1
                }
            }
        }
    }
}
