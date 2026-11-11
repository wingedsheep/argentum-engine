package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Smothering Abomination — its controller sacrifices a creature each upkeep, and every creature they
 * sacrifice draws a card, including the Abomination itself.
 */
class SmotheringAbominationScenarioTest : ScenarioTestBase() {

    private fun board(withBears: Boolean) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Smothering Abomination")
        .apply { if (withBears) withCardOnBattlefield(1, "Grizzly Bears") }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("upkeep: sacrificing another creature draws a card and keeps the Abomination") {
            val game = board(withBears = true)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            val handBefore = game.handSize(1)

            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            }
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Smothering Abomination") shouldBe true
            withClue("the sacrifice drew a card") { game.handSize(1) shouldBe handBefore + 1 }
        }

        test("upkeep: sacrificing itself still draws a card") {
            val game = board(withBears = false)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val handBefore = game.handSize(1)

            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(game.findPermanent("Smothering Abomination")!!)).error shouldBe null
            }
            game.resolveStack()

            game.isInGraveyard(1, "Smothering Abomination") shouldBe true
            withClue("the leaves-the-battlefield trigger saw its own sacrifice") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
