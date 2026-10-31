package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Of One Mind (IKO #60).
 *
 * "This spell costs {2} less to cast if you control a Human creature and a non-Human creature."
 * The discount is a self-cast ModifySpellCost gated on `Conditions.All` of two YouControl checks,
 * so it needs both halves: Humans alone or non-Humans alone leave the full {2}{U}.
 */
class OfOneMindScenarioTest : ScenarioTestBase() {

    private fun game(islands: Int, creatures: List<String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Of One Mind")
        .withLandsOnBattlefield(1, "Island", islands)
        .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Of One Mind") {

            test("with a Human and a non-Human creature it costs only {U} and draws two") {
                val game = game(islands = 1, creatures = listOf("Elite Vanguard", "Grizzly Bears"))
                withClue("cast should succeed for {U}") { game.castSpell(1, "Of One Mind").error shouldBe null }
                game.resolveStack()
                game.handSize(1) shouldBe 2
            }

            test("with only Human creatures the full {2}{U} is due") {
                val onOneLand = game(islands = 1, creatures = listOf("Elite Vanguard", "Elite Vanguard"))
                onOneLand.castSpell(1, "Of One Mind").error shouldNotBe null
                onOneLand.handSize(1) shouldBe 1

                val onThreeLands = game(islands = 3, creatures = listOf("Elite Vanguard", "Elite Vanguard"))
                onThreeLands.castSpell(1, "Of One Mind").error shouldBe null
                onThreeLands.resolveStack()
                onThreeLands.handSize(1) shouldBe 2
            }

            test("with only non-Human creatures the full {2}{U} is due") {
                val onOneLand = game(islands = 1, creatures = listOf("Grizzly Bears", "Grizzly Bears"))
                onOneLand.castSpell(1, "Of One Mind").error shouldNotBe null
                onOneLand.handSize(1) shouldBe 1

                val onThreeLands = game(islands = 3, creatures = listOf("Grizzly Bears", "Grizzly Bears"))
                onThreeLands.castSpell(1, "Of One Mind").error shouldBe null
                onThreeLands.resolveStack()
                onThreeLands.handSize(1) shouldBe 2
            }
        }
    }
}
