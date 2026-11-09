package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Moorland Drifter (SOI #27) — {1}{W} 2/2 Spirit.
 *
 *   Delirium — This creature has flying as long as there are four or more card types among cards
 *   in your graveyard.
 */
class MoorlandDrifterScenarioTest : ScenarioTestBase() {

    init {
        context("Moorland Drifter delirium") {

            test("without delirium it lacks flying") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Moorland Drifter")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Moorland Drifter")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FLYING) shouldBe false
            }

            test("with four card types in the graveyard it has flying") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Moorland Drifter")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Moorland Drifter")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FLYING) shouldBe true
            }
        }
    }
}
