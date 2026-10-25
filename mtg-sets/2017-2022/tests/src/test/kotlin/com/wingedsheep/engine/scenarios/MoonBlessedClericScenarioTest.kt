package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Moon-Blessed Cleric (Adventures in the Forgotten Realms #26).
 *
 * Divine Intervention — When this creature enters, you may search your library for an
 * enchantment card, reveal it, then shuffle and put that card on top.
 *
 * Proves only enchantments are offered and the chosen one ends up on top of the library
 * (not in hand), and that declining the "may" leaves the library alone.
 */
class MoonBlessedClericScenarioTest : ScenarioTestBase() {

    init {
        test("Divine Intervention puts the searched enchantment on top of the library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Moon-Blessed Cleric")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Moon-Blessed Cleric").error shouldBe null
            game.resolveStack()

            withClue("the optional trigger asks whether to search") {
                game.hasPendingDecision() shouldBe true
            }
            game.answerYesNo(true)

            val pacifism = game.findCardsInLibrary(1, "Pacifism").single()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the enchantment is offered") {
                decision.options shouldContainExactly listOf(pacifism)
            }
            game.selectCards(listOf(pacifism))
            game.resolveStack()

            withClue("Pacifism is on top of the library") {
                game.state.getLibrary(game.player1Id).first() shouldBe pacifism
            }
            withClue("the library still holds all four cards") {
                game.state.getLibrary(game.player1Id).size shouldBe 4
            }
            withClue("the Cleric is on the battlefield") {
                game.isOnBattlefield("Moon-Blessed Cleric") shouldBe true
            }
        }

        test("declining the search leaves the library untouched") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Moon-Blessed Cleric")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Pacifism")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.state.getLibrary(game.player1Id)
            game.castSpell(1, "Moon-Blessed Cleric").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe true
            game.answerYesNo(false)
            game.resolveStack()

            withClue("no further decision after declining") {
                game.hasPendingDecision() shouldBe false
            }
            withClue("library order unchanged") {
                game.state.getLibrary(game.player1Id) shouldBe before
            }
        }
    }
}
