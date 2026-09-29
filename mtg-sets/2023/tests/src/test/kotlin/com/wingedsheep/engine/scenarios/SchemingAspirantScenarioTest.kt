package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Scheming Aspirant (ONE #107) — {1}{B} 1/3 Creature — Phyrexian Advisor.
 *
 * "Whenever you proliferate, each opponent loses 2 life and you gain 2 life."
 *
 * Proof card for "whenever you proliferate": the drain happens even when the proliferate chose
 * nothing, and not when an opponent proliferates.
 */
class SchemingAspirantScenarioTest : ScenarioTestBase() {

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private fun resolveAll(game: TestGame) {
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) game.skipSelection() else game.resolveStack()
        }
    }

    init {
        cardRegistry.register(spreading)

        test("your proliferate drains each opponent for 2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scheming Aspirant")
                .withCardInHand(1, "Test Spreading")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            resolveAll(game)

            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 18
        }

        test("an opponent's proliferate does not trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scheming Aspirant")
                .withCardInHand(2, "Test Spreading")
                .withLandsOnBattlefield(2, "Swamp", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Test Spreading").error shouldBe null
            resolveAll(game)

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
