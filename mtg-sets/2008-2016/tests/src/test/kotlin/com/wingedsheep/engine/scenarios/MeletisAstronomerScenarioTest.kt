package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Meletis Astronomer: "Heroic — Whenever you cast a spell that targets this creature, look at the
 * top three cards of your library. You may reveal an enchantment card from among them and put it
 * into your hand. Put the rest on the bottom of your library in any order."
 */
class MeletisAstronomerScenarioTest : ScenarioTestBase() {

    init {
        test("targeting it digs an enchantment from the top three into hand and bottoms the rest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Meletis Astronomer")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val astronomer = game.findPermanent("Meletis Astronomer")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Giant Growth").single(),
                    targets = listOf(ChosenTarget.Permanent(astronomer)),
                )
            ).error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.selectCards(listOf(game.findCardsInLibrary(1, "Pacifism").single())).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.isInHand(1, "Pacifism") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 2
            game.state.projectedState.getPower(astronomer) shouldBe 4
        }
    }
}
