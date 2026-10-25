package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rune of Flight (KHM #75) — "Enchant permanent. When this Aura enters, draw a card. As long as
 * enchanted permanent is a creature, it has flying. As long as enchanted permanent is an Equipment,
 * it has 'Equipped creature has flying.'"
 */
class RuneOfFlightScenarioTest : ScenarioTestBase() {

    init {
        context("Rune of Flight") {

            test("on a creature: draws a card and the creature has flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Rune of Flight")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Rune of Flight", bears).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }

            test("on an Equipment: only the equipped creature gains flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withCardInHand(1, "Rune of Flight")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                game.castSpell(1, "Rune of Flight", bonesplitter).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe false
                game.state.projectedState.hasKeyword(bonesplitter, Keyword.FLYING) shouldBe false
            }
        }
    }
}
