package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Inquisitor's Ox (SOI #24) — {3}{W} 2/5 Ox.
 *
 *   Delirium — This creature gets +1/+0 and has vigilance as long as there are four or more card
 *   types among cards in your graveyard.
 */
class InquisitorsOxScenarioTest : ScenarioTestBase() {

    init {
        context("Inquisitor's Ox delirium") {

            test("without delirium it is a plain 2/5 without vigilance") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Inquisitor's Ox")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Inquisitor's Ox")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 5
                projected.hasKeyword(creature, Keyword.VIGILANCE) shouldBe false
            }

            test("with four card types in the graveyard it is a 3/5 with vigilance") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Inquisitor's Ox")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Inquisitor's Ox")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 3
                projected.getToughness(creature) shouldBe 5
                projected.hasKeyword(creature, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
