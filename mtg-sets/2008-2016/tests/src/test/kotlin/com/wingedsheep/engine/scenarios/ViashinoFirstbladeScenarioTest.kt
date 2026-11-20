package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Viashino Firstblade (DGM #113) — {1}{R}{W} 2/2 Creature — Lizard Soldier.
 *
 *   Haste
 *   When this creature enters, it gets +2/+2 until end of turn.
 */
class ViashinoFirstbladeScenarioTest : ScenarioTestBase() {
    init {
        test("enters as a 4/4 haste attacker for the turn") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Viashino Firstblade")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Viashino Firstblade").error shouldBe null
            game.resolveStack()

            val blade = game.findPermanent("Viashino Firstblade")!!
            withClue("the enters trigger pumped it +2/+2") {
                game.state.projectedState.getPower(blade) shouldBe 4
                game.state.projectedState.getToughness(blade) shouldBe 4
            }
            game.state.projectedState.hasKeyword(blade, Keyword.HASTE) shouldBe true

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets it attack the turn it enters") {
                game.declareAttackers(mapOf("Viashino Firstblade" to 2)).error shouldBe null
            }
        }
    }
}
