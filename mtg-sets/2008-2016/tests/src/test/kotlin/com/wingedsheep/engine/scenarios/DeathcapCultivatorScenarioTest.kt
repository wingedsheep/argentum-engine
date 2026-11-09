package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Deathcap Cultivator (SOI #202) — {1}{G} 2/1 Human Druid.
 *
 *   {T}: Add {B} or {G}.
 *   Delirium — This creature has deathtouch as long as there are four or more card types among
 *   cards in your graveyard.
 */
class DeathcapCultivatorScenarioTest : ScenarioTestBase() {

    init {
        context("Deathcap Cultivator delirium") {

            test("without delirium it lacks deathtouch") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Deathcap Cultivator")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .build()

                val creature = game.findPermanent("Deathcap Cultivator")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 1
                projected.hasKeyword(creature, Keyword.DEATHTOUCH) shouldBe false
            }

            test("with four card types in the graveyard it has deathtouch") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Deathcap Cultivator")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .build()

                val creature = game.findPermanent("Deathcap Cultivator")!!
                val projected = game.state.projectedState
                projected.getPower(creature) shouldBe 2
                projected.getToughness(creature) shouldBe 1
                projected.hasKeyword(creature, Keyword.DEATHTOUCH) shouldBe true
            }
        }
    }
}
