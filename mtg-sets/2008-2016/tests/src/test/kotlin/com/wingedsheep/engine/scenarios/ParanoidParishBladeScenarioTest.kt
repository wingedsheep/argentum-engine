package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Paranoid Parish-Blade (SOI #33) — {2}{W} 3/2 Human Soldier.
 *
 *   Delirium — This creature gets +1/+0 and has first strike as long as there are four or more
 *   card types among cards in your graveyard.
 */
class ParanoidParishBladeScenarioTest : ScenarioTestBase() {

    init {
        context("Paranoid Parish-Blade delirium") {

            test("without delirium it is a plain 3/2 without first strike") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Paranoid Parish-Blade")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Paranoid Parish-Blade")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 3
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe false
            }

            test("with four card types in the graveyard it is a 4/2 with first strike") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Paranoid Parish-Blade")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Paranoid Parish-Blade")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 4
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe true
            }
        }
    }
}
