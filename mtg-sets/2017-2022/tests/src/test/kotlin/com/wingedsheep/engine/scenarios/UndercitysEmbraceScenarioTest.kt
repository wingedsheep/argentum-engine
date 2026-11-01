package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Undercity's Embrace (RNA #89) — target opponent sacrifices a creature; you gain 4 life if you
 * control a creature with power 4 or greater.
 */
class UndercitysEmbraceScenarioTest : ScenarioTestBase() {
    private fun cast(yourCreature: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Undercity's Embrace")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(1, yourCreature)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.castSpellTargetingPlayer(1, "Undercity's Embrace", 2).error shouldBe null
            game.resolveStack()
        }

    init {
        test("with a power-4 creature the opponent sacrifices and you gain 4") {
            val game = cast("Craw Wurm")
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 24
        }

        test("without one the opponent still sacrifices but you gain nothing") {
            val game = cast("Hill Giant")
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
