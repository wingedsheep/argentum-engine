package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Darkblade Agent (GRN #164) — {1}{U}{B} Creature — Human Assassin 2/3.
 *
 *   As long as you've surveilled this turn, this creature has deathtouch and "Whenever this
 *   creature deals combat damage to a player, you draw a card."
 */
class DarkbladeAgentScenarioTest : ScenarioTestBase() {

    init {
        val surveilOne = card("Test Surveil One") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            oracleText = "Surveil 1."
            spell { effect = Patterns.Library.surveil(1) }
        }
        val scryOne = card("Test Scry One") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            oracleText = "Scry 1."
            spell { effect = Patterns.Library.scry(1) }
        }
        cardRegistry.register(listOf(surveilOne, scryOne))

        fun baseScenario() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Darkblade Agent", summoningSickness = false)
            .withCardInHand(1, "Test Surveil One")
            .withCardInHand(1, "Test Scry One")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun ScenarioTestBase.TestGame.look(name: String) {
            castSpell(1, name).error shouldBe null
            resolveStack()
            if (getPendingDecision() is SelectCardsDecision) skipSelection()
            if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
            resolveStack()
        }

        fun ScenarioTestBase.TestGame.attackUnblocked() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Darkblade Agent" to 2)).error shouldBe null
            passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        }

        test("without a surveil it has no deathtouch and draws nothing on combat damage") {
            val game = baseScenario().build()
            val agent = game.findPermanent("Darkblade Agent")!!
            game.state.projectedState.hasKeyword(agent, Keyword.DEATHTOUCH) shouldBe false

            val hand = game.handSize(1)
            game.attackUnblocked()

            game.getLifeTotal(2) shouldBe 18
            game.handSize(1) shouldBe hand
        }

        test("a scry does not count as surveilling") {
            val game = baseScenario().build()
            val agent = game.findPermanent("Darkblade Agent")!!
            game.look("Test Scry One")

            game.state.projectedState.hasKeyword(agent, Keyword.DEATHTOUCH) shouldBe false
            val hand = game.handSize(1)
            game.attackUnblocked()
            game.handSize(1) shouldBe hand
        }

        test("after surveilling it has deathtouch and draws a card on combat damage to a player") {
            val game = baseScenario().build()
            val agent = game.findPermanent("Darkblade Agent")!!
            game.look("Test Surveil One")

            game.state.projectedState.hasKeyword(agent, Keyword.DEATHTOUCH) shouldBe true
            val hand = game.handSize(1)
            game.attackUnblocked()

            game.getLifeTotal(2) shouldBe 18
            withClue("the granted combat-damage trigger draws one card") {
                game.handSize(1) shouldBe hand + 1
            }
        }
    }
}
