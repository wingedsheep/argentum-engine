package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Find a card by name in a player's hand — Eriette's Whisper needs a manual multi-target cast. */
private fun ScenarioTestBase.TestGame.handCardId(playerNumber: Int, name: String): EntityId {
    val playerId = if (playerNumber == 1) player1Id else player2Id
    return state.getHand(playerId).first {
        state.getEntity(it)?.get<CardComponent>()?.name == name
    }
}

/**
 * Resolve the spell, answering the opponent's "discard two cards" selection (the discarding player
 * picks the cards, so resolution pauses on a SelectCardsDecision). Player 2 holds exactly two cards,
 * so we discard the whole hand.
 */
private fun ScenarioTestBase.TestGame.resolveWithDiscard(opponentNumber: Int) {
    val opponentId = if (opponentNumber == 1) player1Id else player2Id
    resolveStack()
    if (state.pendingDecision != null) {
        selectCards(state.getHand(opponentId))
        resolveStack()
    }
}

/**
 * Scenario tests for Eriette's Whisper (WOE #88) — {3}{B} Sorcery.
 *
 * "Target opponent discards two cards. Create a Wicked Role token attached to up to one target
 *  creature you control."
 *
 * Exercises the new Wicked Role token: the +1/+1 buff and its "when this Role is put into a
 * graveyard, each opponent loses 1 life" drain trigger.
 */
class EriettesWhisperScenarioTest : ScenarioTestBase() {

    init {
        context("Eriette's Whisper") {

            test("opponent discards two cards and your creature gains the Wicked Role (+1/+1)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eriette's Whisper")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    CastSpell(
                        game.player1Id,
                        game.handCardId(1, "Eriette's Whisper"),
                        listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                game.resolveWithDiscard(2)

                val projector = StateProjector()
                withClue("Player 2 discards its whole two-card hand") {
                    game.state.getHand(game.player2Id).size shouldBe 0
                }
                withClue("Wicked Role enters attached to Grizzly Bears, buffing it to 3/3") {
                    game.findPermanent("Wicked Role") shouldNotBe null
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 3
                }
            }

            test("each opponent loses 1 life when the Wicked Role is put into the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eriette's Whisper")
                    .withCardInHand(1, "Frantic Firebolt")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Savannah Lions") // 1/1 → 2/2 with the Role
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                game.execute(
                    CastSpell(
                        game.player1Id,
                        game.handCardId(1, "Eriette's Whisper"),
                        listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(lions))
                    )
                ).error shouldBe null
                game.resolveWithDiscard(2)

                val lifeBefore = game.state.getEntity(game.player2Id)!!.get<LifeTotalComponent>()!!.life

                // Kill the 2/2 (Lions + Wicked Role) with our own Frantic Firebolt; the Role falls off
                // into the graveyard, firing its drain.
                game.castSpell(1, "Frantic Firebolt", lions).error shouldBe null
                game.resolveStack()

                withClue("Savannah Lions died, so the Wicked Role went to the graveyard and drained the opponent") {
                    game.findPermanent("Savannah Lions") shouldBe null
                    game.findPermanent("Wicked Role") shouldBe null
                    game.state.getEntity(game.player2Id)!!.get<LifeTotalComponent>()!!.life shouldBe lifeBefore - 1
                }
            }
        }
    }
}
