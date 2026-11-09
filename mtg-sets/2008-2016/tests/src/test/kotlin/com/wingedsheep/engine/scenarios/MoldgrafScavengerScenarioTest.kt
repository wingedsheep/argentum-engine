package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe

/**
 * Moldgraf Scavenger (SOI #218) — {1}{G} 0/4 Fungus.
 *
 *   Delirium — This creature gets +3/+0 as long as there are four or more card types among cards
 *   in your graveyard.
 */
class MoldgrafScavengerScenarioTest : ScenarioTestBase() {

    init {
        context("Moldgraf Scavenger delirium") {

            test("without delirium it is a plain 0/4") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Moldgraf Scavenger")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Moldgraf Scavenger")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 0
                projected.getToughness(creature) shouldBe 4
            }

            test("with four card types in the graveyard it is a 3/4") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Moldgraf Scavenger")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Moldgraf Scavenger")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 3
                projected.getToughness(creature) shouldBe 4
            }
        }
    }
}
