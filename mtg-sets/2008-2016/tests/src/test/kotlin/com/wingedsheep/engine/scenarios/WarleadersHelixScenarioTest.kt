package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Warleader's Helix (DGM #116) — {2}{R}{W} Instant.
 *
 *   Warleader's Helix deals 4 damage to any target and you gain 4 life.
 */
class WarleadersHelixScenarioTest : ScenarioTestBase() {
    init {
        test("deals 4 to a player and gains its caster 4") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Warleader's Helix")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Warleader's Helix", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 24
        }

        test("kills a creature and still gains 4") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Warleader's Helix")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Warleader's Helix", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 24
        }
    }
}
