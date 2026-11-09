package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ad Nauseam {3}{B}{B} — Instant.
 *
 * "Reveal the top card of your library and put that card into your hand. You lose life equal to its
 *  mana value. You may repeat this process any number of times."
 *
 * The first pass is mandatory; after each pass the caster chooses whether to go again, and each
 * pass costs life equal to that pass's card.
 */
class AdNauseamScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame {
        var builder = scenario()
            .withPlayers("You", "Opponent")
            .withCardInHand(1, "Ad Nauseam")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Plains")
            .withLifeTotal(1, 20)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return builder.build()
    }

    /** Mana values of the library from the top down. */
    private fun TestGame.libraryManaValues(): List<Int> =
        state.getLibrary(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.manaValue }

    init {
        context("Ad Nauseam") {
            test("reveals one card and may stop: you lose life equal to just that card") {
                val game = setup()
                val values = game.libraryManaValues()
                withClue("fixture: the first card must cost life") { (values[0] > 0) shouldBe true }

                game.castSpell(1, "Ad Nauseam").error shouldBe null
                game.resolveStack()
                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(false)

                game.getLifeTotal(1) shouldBe 20 - values[0]
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe values.size - 1
            }

            test("repeating draws the next card and costs its mana value too; stops when told to") {
                val game = setup()
                val values = game.libraryManaValues()

                game.castSpell(1, "Ad Nauseam").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true)
                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(true)
                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(false)

                game.getLifeTotal(1) shouldBe 20 - values.take(3).sum()
                game.handSize(1) shouldBe 3
                game.librarySize(1) shouldBe values.size - 3
            }
        }
    }
}
