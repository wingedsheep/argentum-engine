package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Fear of Death (VOW #59) — {1}{U} Enchantment — Aura.
 *
 *   Enchant creature
 *   When this Aura enters, mill two cards.
 *   Enchanted creature gets -X/-0, where X is the number of cards in your graveyard.
 *
 * Exercises the ETB self-mill and the continuous -X/-0 debuff on the enchanted creature scaling
 * with the controller's graveyard size.
 */
class FearOfDeathScenarioTest : ScenarioTestBase() {

    init {
        context("Fear of Death") {

            test("entering mills two cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fear of Death")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("starts with an empty graveyard") {
                    game.graveyardSize(1) shouldBe 0
                }

                game.castSpell(1, "Fear of Death", targetId = bears).error shouldBe null
                game.resolveStack() // Aura resolves and attaches -> ETB trigger mills two

                withClue("Fear of Death is attached to Grizzly Bears") {
                    game.isOnBattlefield("Fear of Death") shouldBe true
                }
                withClue("the ETB milled exactly two cards into the graveyard") {
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("enchanted creature's power is reduced by the controller's graveyard size") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Fear of Death", "Grizzly Bears")
                repeat(3) { builder.withCardInGraveyard(1, "Swamp") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("with three cards in the graveyard, Grizzly Bears (2/2 base) becomes -1/2") {
                    game.state.projectedState.getPower(bears) shouldBe -1
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
