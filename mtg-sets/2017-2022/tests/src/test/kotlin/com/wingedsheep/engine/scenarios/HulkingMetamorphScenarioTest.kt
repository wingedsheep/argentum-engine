package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hulking Metamorph (BRO #79) — {9} Artifact Creature — Shapeshifter 7/7, Prototype {2}{U}{U} — 3/3.
 *
 * Enters as a copy of an artifact or creature you control, except it's an artifact creature in
 * addition to its other types and its P/T are its own — 3/3 prototyped, 7/7 otherwise (ruling).
 */
class HulkingMetamorphScenarioTest : ScenarioTestBase() {
    init {
        context("Hulking Metamorph") {
            test("prototyped, it copies a creature you control as a 3/3 artifact creature and reverts on dying") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Hulking Metamorph")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellPrototyped(1, "Hulking Metamorph").error shouldBe null
                game.resolveStack()

                // Only permanents you control are offered — the opponent's Bears are not.
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val giant = game.findPermanent("Hill Giant")!!
                decision.options shouldBe listOf(giant)
                game.selectCards(listOf(giant)).error shouldBe null
                game.resolveStack()

                val copy = game.findPermanents("Hill Giant").single { it != giant }
                val projected = game.state.projectedState
                projected.getPower(copy) shouldBe 3
                projected.getToughness(copy) shouldBe 3
                projected.hasType(copy, "ARTIFACT") shouldBe true
                projected.hasSubtype(copy, "Giant") shouldBe true
                // It copies the Giant's color, not the prototype's blue.
                projected.getColors(copy) shouldBe setOf(Color.RED.name)

                game.passPriority()
                game.castSpell(2, "Lightning Bolt", copy).error shouldBe null
                game.resolveStack()
                game.findPermanents("Hill Giant").single() shouldBe giant
                val card = game.findCardsInGraveyard(1, "Hulking Metamorph").single()
                val printed = game.state.getEntity(card)!!.get<CardComponent>()!!
                printed.manaValue shouldBe 9
                printed.baseStats!!.basePower shouldBe 7
            }

            test("cast for {9}, it copies an artifact as a 7/7 artifact creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Hulking Metamorph")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Island", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hulking Metamorph").error shouldBe null
                game.resolveStack()
                val thopter = game.findPermanent("Ornithopter")!!
                game.selectCards(listOf(thopter)).error shouldBe null
                game.resolveStack()

                val copy = game.findPermanents("Ornithopter").single { it != thopter }
                val projected = game.state.projectedState
                projected.getPower(copy) shouldBe 7
                projected.getToughness(copy) shouldBe 7
                projected.hasType(copy, "ARTIFACT") shouldBe true
                projected.isCreature(copy) shouldBe true
            }

            test("declining the copy leaves it a 3/3 prototyped Shapeshifter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Hulking Metamorph")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellPrototyped(1, "Hulking Metamorph").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                val metamorph = game.findPermanent("Hulking Metamorph")!!
                game.state.projectedState.getPower(metamorph) shouldBe 3
                game.state.projectedState.getToughness(metamorph) shouldBe 3
            }
        }
    }
}
