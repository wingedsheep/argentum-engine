package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Scourge Wolf (SOI #179) — {R}{R} 2/2 Wolf Horror.
 *
 *   First strike
 *   Delirium — This creature has double strike as long as there are four or more card types among
 *   cards in your graveyard.
 */
class ScourgeWolfScenarioTest : ScenarioTestBase() {

    init {
        context("Scourge Wolf delirium") {

            test("without delirium it lacks double strike") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Scourge Wolf")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Scourge Wolf")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(creature, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            test("with four card types in the graveyard it has double strike") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Scourge Wolf")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Scourge Wolf")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 2
                projected.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(creature, Keyword.DOUBLE_STRIKE) shouldBe true
            }
        }
    }
}
