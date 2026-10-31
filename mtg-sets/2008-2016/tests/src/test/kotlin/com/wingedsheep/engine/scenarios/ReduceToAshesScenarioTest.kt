package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Reduce to Ashes (SOI #176) — {4}{R} Sorcery.
 *
 *   Reduce to Ashes deals 5 damage to target creature. If that creature would die this turn, exile it instead.
 */
class ReduceToAshesScenarioTest : ScenarioTestBase() {

    init {
        context("Reduce to Ashes") {
            test("deals 5 damage and exiles the creature instead of letting it die") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reduce to Ashes")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Reduce to Ashes", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
        }
    }
}
