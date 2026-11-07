package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gnarlwood Dryad (EMN #159) — {G} Creature — Dryad Horror, 1/1.
 *
 *   Deathtouch
 *   Delirium — This creature gets +2/+2 as long as there are four or more card types among cards
 *   in your graveyard.
 */
class GnarlwoodDryadScenarioTest : ScenarioTestBase() {

    init {
        test("without delirium it is a 1/1 with deathtouch") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gnarlwood Dryad")
                .withCardInGraveyard(1, "Grizzly Bears")   // creature
                .withCardInGraveyard(1, "Lightning Bolt")  // instant
                .withCardInGraveyard(1, "Lava Spike")      // sorcery
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dryad = game.findPermanent("Gnarlwood Dryad")!!
            game.state.projectedState.getPower(dryad) shouldBe 1
            game.state.projectedState.getToughness(dryad) shouldBe 1
            game.state.projectedState.hasKeyword(dryad, Keyword.DEATHTOUCH) shouldBe true
        }

        test("with four card types in your graveyard it is a 3/3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gnarlwood Dryad")
                .withCardInGraveyard(1, "Grizzly Bears")   // creature
                .withCardInGraveyard(1, "Lightning Bolt")  // instant
                .withCardInGraveyard(1, "Lava Spike")      // sorcery
                .withCardInGraveyard(1, "Ornithopter")     // artifact
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dryad = game.findPermanent("Gnarlwood Dryad")!!
            game.state.projectedState.getPower(dryad) shouldBe 3
            game.state.projectedState.getToughness(dryad) shouldBe 3
        }

        test("an opponent's graveyard does not grant delirium") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gnarlwood Dryad")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withCardInGraveyard(2, "Lava Spike")
                .withCardInGraveyard(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dryad = game.findPermanent("Gnarlwood Dryad")!!
            game.state.projectedState.getPower(dryad) shouldBe 1
        }
    }
}
