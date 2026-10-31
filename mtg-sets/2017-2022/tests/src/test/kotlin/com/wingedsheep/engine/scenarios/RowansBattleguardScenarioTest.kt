package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rowan's Battleguard (ELD #306) — {3}{R} Creature — Human Knight 3/3.
 *
 * "First strike. As long as you control a Rowan planeswalker, this creature gets +3/+0."
 */
class RowansBattleguardScenarioTest : ScenarioTestBase() {

    init {
        context("Rowan's Battleguard") {

            fun game(vararg myPermanents: String, opponentPermanents: List<String> = emptyList()) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Rowan's Battleguard")
                .apply { myPermanents.forEach { withCardOnBattlefield(1, it) } }
                .apply { opponentPermanents.forEach { withCardOnBattlefield(2, it) } }
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("is a 3/3 first striker without a Rowan planeswalker") {
                val g = game()
                val guard = g.findPermanent("Rowan's Battleguard")!!
                g.state.projectedState.hasKeyword(guard, Keyword.FIRST_STRIKE) shouldBe true
                g.state.projectedState.getPower(guard) shouldBe 3
                g.state.projectedState.getToughness(guard) shouldBe 3
            }

            test("gets +3/+0 while you control a Rowan planeswalker") {
                val g = game("Rowan, Scholar of Sparks")
                val guard = g.findPermanent("Rowan's Battleguard")!!
                g.state.projectedState.getPower(guard) shouldBe 6
                g.state.projectedState.getToughness(guard) shouldBe 3
            }

            test("a non-Rowan planeswalker, or an opponent's Rowan, doesn't count") {
                val g = game("Chandra Nalaar", opponentPermanents = listOf("Rowan, Scholar of Sparks"))
                val guard = g.findPermanent("Rowan's Battleguard")!!
                g.state.projectedState.getPower(guard) shouldBe 3
            }
        }
    }
}
