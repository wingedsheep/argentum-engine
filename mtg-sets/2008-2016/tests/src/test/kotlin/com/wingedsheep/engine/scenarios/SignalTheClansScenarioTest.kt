package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

/**
 * Signal the Clans (GTC #194) — {R}{G} instant.
 *
 *   Search your library for three creature cards and reveal them. If you reveal three cards with
 *   different names, choose one of them at random and put that card into your hand. Shuffle the
 *   rest into your library.
 */
class SignalTheClansScenarioTest : ScenarioTestBase() {

    private fun board(vararg library: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Signal the Clans")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castAndSearch(vararg picks: String) {
        execute(
            CastSpell(player1Id, state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Signal the Clans"
            })
        ).error shouldBe null
        resolveStack()
        val chosen = picks.groupBy { it }.flatMap { (name, copies) ->
            findCardsInLibrary(1, name).take(copies.size)
        }
        selectCards(chosen).error shouldBe null
        resolveStack()
    }

    private fun TestGame.handNames() =
        state.getHand(player1Id).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("three differently named creatures: one of them, at random, goes to hand") {
            val game = board("Grizzly Bears", "Hill Giant", "Craw Wurm", "Forest")

            game.castAndSearch("Grizzly Bears", "Hill Giant", "Craw Wurm")

            val hand = game.handNames()
            hand.size shouldBe 1
            hand.single() shouldBeIn listOf("Grizzly Bears", "Hill Giant", "Craw Wurm")
            game.librarySize(1) shouldBe 3
            game.isInGraveyard(1, "Signal the Clans") shouldBe true
        }

        test("two of the three share a name: nothing goes to hand") {
            val game = board("Grizzly Bears", "Grizzly Bears", "Hill Giant", "Craw Wurm")

            game.castAndSearch("Grizzly Bears", "Grizzly Bears", "Hill Giant")

            game.handNames() shouldBe emptyList()
            game.librarySize(1) shouldBe 4
        }

        test("fewer than three creatures found: nothing goes to hand") {
            val game = board("Grizzly Bears", "Hill Giant", "Forest")

            game.castAndSearch("Grizzly Bears", "Hill Giant")

            game.handNames() shouldBe emptyList()
            game.librarySize(1) shouldBe 3
        }
    }
}
