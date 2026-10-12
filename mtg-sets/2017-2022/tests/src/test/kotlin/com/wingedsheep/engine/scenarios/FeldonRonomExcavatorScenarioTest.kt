package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Feldon, Ronom Excavator — "Whenever Feldon is dealt damage, exile that many cards from the top
 * of your library. Choose one of them. Until the end of your next turn, you may play that card."
 *
 * Covers: the exile count follows the damage dealt, only the chosen card gets the play
 * permission, and that permission survives the opponent's turn and lapses after Feldon's
 * controller's next turn.
 */
class FeldonRonomExcavatorScenarioTest : ScenarioTestBase() {

    private fun board(spell: String) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Feldon, Ronom Excavator")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, spell)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.exileSize(): Int = state.getExile(player1Id).size

    private fun TestGame.playable(): Set<EntityId> =
        state.mayPlayPermissions.flatMap { it.cardIds }.toSet()

    init {
        test("2 damage exiles two cards and only the chosen one is playable") {
            val game = board("Shock")
            val feldon = game.findPermanent("Feldon, Ronom Excavator")!!
            game.castSpell(1, "Shock", feldon).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options shouldHaveSize 2
            game.exileSize() shouldBe 2
            val chosen = decision.options.first()
            game.selectCards(listOf(chosen))
            game.resolveStack()

            game.playable() shouldBe setOf(chosen)
        }

        test("1 damage exiles one card, playable through the next turn and not after") {
            val game = board("Scorching Spear")
            val feldon = game.findPermanent("Feldon, Ronom Excavator")!!
            game.castSpell(1, "Scorching Spear", feldon).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(listOf((game.getPendingDecision() as SelectCardsDecision).options.single()))
                game.resolveStack()
            }

            game.isOnBattlefield("Feldon, Ronom Excavator") shouldBe true
            game.exileSize() shouldBe 1
            val exiled = game.state.getExile(game.player1Id).single()
            game.playable() shouldBe setOf(exiled)

            // Opponent's turn: still playable.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.playable() shouldBe setOf(exiled)

            // Feldon's controller's next turn: still playable.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.playable() shouldBe setOf(exiled)

            // After that turn ends, the permission is gone.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            (exiled in game.playable()) shouldBe false
        }
    }
}
