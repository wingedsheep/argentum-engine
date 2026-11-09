package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Hound of the Farbogs (SOI #117) — {4}{B} 5/3 Zombie Dog.
 *
 *   Delirium — This creature has menace as long as there are four or more card types among cards
 *   in your graveyard. (A creature with menace can't be blocked except by two or more creatures.)
 */
class HoundOfTheFarbogsScenarioTest : ScenarioTestBase() {

    init {
        context("Hound of the Farbogs delirium") {

            test("without delirium it lacks menace") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hound of the Farbogs")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Hound of the Farbogs")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 5
                projected.getToughness(creature) shouldBe 3
                projected.hasKeyword(creature, Keyword.MENACE) shouldBe false
            }

            test("with four card types in the graveyard it has menace") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hound of the Farbogs")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Hound of the Farbogs")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 5
                projected.getToughness(creature) shouldBe 3
                projected.hasKeyword(creature, Keyword.MENACE) shouldBe true
            }
        }
    }
}
