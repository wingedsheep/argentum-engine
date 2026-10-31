package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sleep Paralysis (SOI #87) — {3}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, tap enchanted creature.
 *   Enchanted creature doesn't untap during its controller's untap step.
 */
class SleepParalysisScenarioTest : ScenarioTestBase() {

    init {
        context("Sleep Paralysis") {
            test("taps the enchanted creature and stops it untapping") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sleep Paralysis")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Sleep Paralysis", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Sleep Paralysis") shouldBe true
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
            }
        }
    }
}
