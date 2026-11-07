package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Thraben Foulbloods (EMN #108) — {2}{B} 3/2 Zombie Dog.
 *
 *   Delirium — This creature gets +1/+1 and has menace as long as there are four or more card
 *   types among cards in your graveyard.
 */
class ThrabenFoulbloodsScenarioTest : ScenarioTestBase() {

    init {
        context("Thraben Foulbloods delirium") {

            test("without delirium it is a plain 3/2") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Thraben Foulbloods")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val dog = game.findPermanent("Thraben Foulbloods")!!
                val projected = game.state.projectedState
                projected.getPower(dog) shouldBe 3
                projected.getToughness(dog) shouldBe 2
                projected.hasKeyword(dog, Keyword.MENACE) shouldBe false
            }

            test("with four card types in the graveyard it is a 4/3 with menace") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Thraben Foulbloods")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val dog = game.findPermanent("Thraben Foulbloods")!!
                val projected = game.state.projectedState
                projected.getPower(dog) shouldBe 4
                projected.getToughness(dog) shouldBe 3
                projected.hasKeyword(dog, Keyword.MENACE) shouldBe true
            }
        }
    }
}
