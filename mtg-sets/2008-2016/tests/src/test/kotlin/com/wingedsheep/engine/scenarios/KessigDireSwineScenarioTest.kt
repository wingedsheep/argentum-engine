package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Kessig Dire Swine (SOI #214) — {4}{G}{G} 6/6 Boar Horror.
 *
 *   Delirium — This creature has trample as long as there are four or more card types among cards
 *   in your graveyard.
 */
class KessigDireSwineScenarioTest : ScenarioTestBase() {

    init {
        context("Kessig Dire Swine delirium") {

            test("without delirium it lacks trample") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kessig Dire Swine")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Kessig Dire Swine")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 6
                projected.getToughness(creature) shouldBe 6
                projected.hasKeyword(creature, Keyword.TRAMPLE) shouldBe false
            }

            test("with four card types in the graveyard it has trample") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Kessig Dire Swine")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Kessig Dire Swine")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 6
                projected.getToughness(creature) shouldBe 6
                projected.hasKeyword(creature, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
