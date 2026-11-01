package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Whispering Snitch (GRN #90) — {1}{B} Creature — Vampire Rogue 1/3.
 *
 *   Whenever you surveil for the first time each turn, this creature deals 1 damage to each
 *   opponent and you gain 1 life.
 */
class WhisperingSnitchScenarioTest : ScenarioTestBase() {

    init {
        val surveilOne = card("Test Snitch Surveil") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            oracleText = "Surveil 1."
            spell { effect = Patterns.Library.surveil(1) }
        }
        cardRegistry.register(listOf(surveilOne))

        fun ScenarioTestBase.TestGame.surveil() {
            castSpell(1, "Test Snitch Surveil").error shouldBe null
            resolveStack()
            if (getPendingDecision() is SelectCardsDecision) skipSelection()
            if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
            resolveStack()
        }

        test("only the first surveil each turn drains 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Whispering Snitch")
                .withCardInHand(1, "Test Snitch Surveil")
                .withCardInHand(1, "Test Snitch Surveil")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.surveil()
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21

            game.surveil()
            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
