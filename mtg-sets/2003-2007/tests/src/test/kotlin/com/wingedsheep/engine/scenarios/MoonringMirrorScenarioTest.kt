package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Moonring Mirror (CHK #262) — "Whenever you draw a card, exile the top card of your library face
 * down. At the beginning of your upkeep, you may exile all cards from your hand face down. If you
 * do, put all other cards you own exiled with this artifact into your hand."
 *
 * What these pin: the draw trigger feeds the artifact's face-down pile, and the upkeep swap returns
 * the pile *as it stood before* the hand was exiled — the cards just exiled from the hand stay
 * exiled (2004-12-01 ruling) and come back only on a later swap.
 */
class MoonringMirrorScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Moonring Mirror")
        .withCardInHand(1, "Grizzly Bears")
        .apply {
            listOf("Island", "Forest", "Mountain", "Plains", "Swamp", "Island", "Forest", "Mountain")
                .forEach { withCardInLibrary(1, it) }
            repeat(8) { withCardInLibrary(2, "Island") }
        }
        .withActivePlayer(1)
        .withTurnNumber(3)
        .inPhase(Phase.BEGINNING, Step.UNTAP)
        .build()

    /** Resolve Moonring Mirror's upkeep trigger, answering its "you may" with [accept]. */
    private fun TestGame.resolveUpkeepSwap(accept: Boolean) {
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(accept)
        resolveStack()
    }

    /** From player 1's draw step, advance through player 2's turn to player 1's next upkeep. */
    private fun TestGame.toOwnNextUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 2's upkeep
        passPriority()
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe player1Id
    }

    private fun TestGame.isFaceDown(id: EntityId) = state.getEntity(id)?.has<FaceDownComponent>() == true

    init {
        test("a draw exiles the next card face down; the upkeep swap returns only the older pile") {
            val game = board()
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveUpkeepSwap(accept = false)
            withClue("declining keeps the hand") { game.state.getHand(game.player1Id) shouldBe listOf(bears) }

            val library = game.state.getLibrary(game.player1Id)
            val drawn = library[0]
            val mirrored = library[1]
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()

            withClue("the drawn card is in hand; the next one was exiled face down by the trigger") {
                game.state.getHand(game.player1Id) shouldContainExactlyInAnyOrder listOf(bears, drawn)
                game.state.getExile(game.player1Id) shouldBe listOf(mirrored)
                game.isFaceDown(mirrored) shouldBe true
            }

            game.toOwnNextUpkeep()
            val handBeforeSwap = game.state.getHand(game.player1Id)
            game.resolveUpkeepSwap(accept = true)

            withClue("the old pile comes back to hand; the hand just exiled does not") {
                game.state.getHand(game.player1Id) shouldBe listOf(mirrored)
                game.state.getExile(game.player1Id) shouldContainExactlyInAnyOrder handBeforeSwap
                handBeforeSwap.all { game.isFaceDown(it) } shouldBe true
                game.isFaceDown(mirrored) shouldBe false
            }
        }

        test("the cards exiled from the hand return on a later swap") {
            val game = board()
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveUpkeepSwap(accept = true)
            withClue("an empty pile returns nothing, but the hand is still exiled face down") {
                game.handSize(1) shouldBe 0
                game.state.getExile(game.player1Id) shouldBe listOf(bears)
                game.isFaceDown(bears) shouldBe true
            }

            val library = game.state.getLibrary(game.player1Id)
            val drawn = library[0]
            val mirrored = library[1]
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
            game.resolveStack()

            game.toOwnNextUpkeep()
            game.resolveUpkeepSwap(accept = true)

            withClue("both the hand-exiled Bears and the draw-exiled card come back; the drawn card is exiled") {
                game.state.getHand(game.player1Id) shouldContainExactlyInAnyOrder listOf(bears, mirrored)
                game.state.getExile(game.player1Id) shouldBe listOf(drawn)
            }
        }
    }
}
