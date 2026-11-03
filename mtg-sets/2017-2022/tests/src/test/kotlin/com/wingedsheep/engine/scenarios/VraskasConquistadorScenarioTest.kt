package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Vraska's Conquistador (RIX #199) — "Whenever this creature attacks or blocks, if you control a
 * Vraska planeswalker, target opponent loses 2 life and you gain 2 life."
 */
class VraskasConquistadorScenarioTest : ScenarioTestBase() {

    private fun attackWith(vraska: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Vraska's Conquistador")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        if (vraska) builder.withCardOnBattlefield(1, "Vraska, Golgari Queen")
        val game = builder.build()
        game.declareAttackers(mapOf("Vraska's Conquistador" to 2)).error shouldBe null
        return game
    }

    init {
        test("attacking while you control a Vraska drains the target opponent for 2") {
            val game = attackWith(vraska = true)
            game.resolveStack() // the only opponent is auto-targeted

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("without a Vraska planeswalker nothing triggers") {
            val game = attackWith(vraska = false)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 20
            game.getLifeTotal(1) shouldBe 20
        }

        test("blocking while you control a Vraska also drains") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vraska's Conquistador")
                .withCardOnBattlefield(1, "Vraska, Golgari Queen")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Vraska's Conquistador" to listOf("Grizzly Bears"))).error shouldBe null
            game.resolveStack() // the only opponent is auto-targeted

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }
    }
}
