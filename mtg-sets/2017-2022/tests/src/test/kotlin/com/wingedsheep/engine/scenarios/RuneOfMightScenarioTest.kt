package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rune of Might (KHM #191) — "Enchant permanent. When this Aura enters, draw a card. As long as
 * enchanted permanent is a creature, it gets +1/+1 and has trample. As long as enchanted permanent
 * is an Equipment, it has 'Equipped creature gets +1/+1 and has trample.'"
 */
class RuneOfMightScenarioTest : ScenarioTestBase() {

    init {
        context("Rune of Might") {

            test("on a creature: draws a card and the creature gets +1/+1 and trample") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Rune of Might")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Rune of Might", bears).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            test("on an Equipment: the equipped creature gets +1/+1 and trample, the Equipment is no creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withCardInHand(1, "Rune of Might")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                game.castSpell(1, "Rune of Might", bonesplitter).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                // 2/2 + Bonesplitter's +2/+0 + the granted +1/+1.
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                game.state.projectedState.hasKeyword(bonesplitter, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
