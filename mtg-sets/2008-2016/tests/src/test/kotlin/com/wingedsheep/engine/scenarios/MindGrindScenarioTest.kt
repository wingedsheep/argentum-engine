package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Mind Grind (GTC #178) — {X}{U}{B} sorcery.
 *
 *   Each opponent reveals cards from the top of their library until they reveal X land cards, then
 *   puts all cards revealed this way into their graveyard. X can't be 0.
 */
class MindGrindScenarioTest : ScenarioTestBase() {

    private fun TestGame.castMindGrind(x: Int) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Mind Grind" },
            xValue = x
        )
    )

    private fun board(vararg library: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Mind Grind")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .apply { library.forEach { withCardInLibrary(2, it) } }
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the opponent bins their library down to the Xth land; the caster's library is untouched") {
            // Top first: two lands among two spells, then a card that stays.
            val game = board("Grizzly Bears", "Forest", "Hill Giant", "Forest", "Craw Wurm")

            game.castMindGrind(2).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 4
            game.findCardsInGraveyard(2, "Forest").size shouldBe 2
            game.librarySize(2) shouldBe 1
            game.findCardsInLibrary(2, "Craw Wurm").size shouldBe 1
            game.librarySize(1) shouldBe 1
        }

        test("fewer than X lands reveals and mills the whole library") {
            val game = board("Forest", "Grizzly Bears")

            game.castMindGrind(2).error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 2
        }

        test("X can't be 0") {
            val game = board("Forest", "Grizzly Bears")

            game.castMindGrind(0).error.shouldNotBeNull() shouldContain "X can't be 0"
            game.isInHand(1, "Mind Grind") shouldBe true
            game.librarySize(2) shouldBe 2
        }
    }
}
