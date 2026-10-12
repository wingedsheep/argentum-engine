package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Skitterbeam Battalion (BRO #165) — {9} Artifact Creature — Construct 4/4, Prototype {3}{R}{R} — 2/2,
 * trample, haste. "When this creature enters, if you cast it, create two tokens that are copies of it."
 *
 * Proves the copies inherit the prototyped characteristics (ruling: same mana cost, color and size as
 * the prototyped permanent), that the tokens don't re-trigger (they weren't cast), and that putting the
 * card onto the battlefield without casting it creates nothing.
 */
class SkitterbeamBattalionScenarioTest : ScenarioTestBase() {
    init {
        context("Skitterbeam Battalion") {
            test("cast prototyped, it makes two red 2/2 token copies and they don't trigger again") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Skitterbeam Battalion")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellPrototyped(1, "Skitterbeam Battalion").error shouldBe null
                game.resolveStack()

                val battalions = game.findPermanents("Skitterbeam Battalion")
                battalions shouldHaveSize 3
                val tokens = battalions.filter { game.state.getEntity(it)!!.has<TokenComponent>() }
                tokens shouldHaveSize 2

                val projected = game.state.projectedState
                for (token in tokens) {
                    projected.getPower(token) shouldBe 2
                    projected.getToughness(token) shouldBe 2
                    projected.getColors(token) shouldBe setOf(Color.RED.name)
                    projected.hasType(token, "ARTIFACT") shouldBe true
                }
                game.state.stack.isEmpty() shouldBe true
            }

            test("cast for {9}, the copies are colorless 4/4s") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Skitterbeam Battalion")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Skitterbeam Battalion").error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Skitterbeam Battalion")
                    .filter { game.state.getEntity(it)!!.has<TokenComponent>() }
                tokens shouldHaveSize 2
                val projected = game.state.projectedState
                for (token in tokens) {
                    projected.getPower(token) shouldBe 4
                    projected.getToughness(token) shouldBe 4
                    projected.getColors(token) shouldBe emptySet()
                }
            }

            test("put onto the battlefield without being cast, it creates no copies") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInGraveyard(1, "Skitterbeam Battalion")
                    .withCardInHand(1, "Zombify")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val battalion = game.findCardsInGraveyard(1, "Skitterbeam Battalion").single()
                game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(battalion)).error shouldBe null
                game.resolveStack()

                game.findPermanents("Skitterbeam Battalion") shouldHaveSize 1
            }
        }
    }
}
