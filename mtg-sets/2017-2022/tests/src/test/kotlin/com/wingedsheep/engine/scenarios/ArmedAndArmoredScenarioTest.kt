package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Armed and Armored (KHM #379) — {1}{W} Instant.
 *
 *   Vehicles you control become artifact creatures until end of turn. Choose a Dwarf you
 *   control. Attach any number of Equipment you control to it.
 */
class ArmedAndArmoredScenarioTest : ScenarioTestBase() {

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    init {
        context("Armed and Armored") {

            test("animates your Vehicles and moves the chosen Equipment onto your only Dwarf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Armed and Armored")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Axgard Cavalry")          // 2/2 Dwarf
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Raiders' Karve")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withCardOnBattlefield(2, "Raiders' Karve")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dwarf = game.findPermanent("Axgard Cavalry")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                val scimitar = game.findPermanent("Leonin Scimitar")!!
                val (myKarve, theirKarve) = game.findPermanents("Raiders' Karve")
                    .partition { game.state.projectedState.getController(it) == game.player1Id }
                    .let { it.first.single() to it.second.single() }

                game.castSpell(1, "Armed and Armored").error shouldBe null
                game.resolveStack()

                // Only one Dwarf: chosen automatically; then any number of Equipment.
                val select = game.getPendingDecision()
                withClue("resolution pauses to choose any number of Equipment") {
                    (select is SelectCardsDecision) shouldBe true
                }
                (select as SelectCardsDecision).minSelections shouldBe 0
                game.submitDecision(CardsSelectedResponse(select.id, listOf(bonesplitter, scimitar)))
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("your Vehicle is an artifact creature; the opponent's is not") {
                    projected.isCreature(myKarve) shouldBe true
                    projected.hasType(myKarve, "ARTIFACT") shouldBe true
                    projected.isCreature(theirKarve) shouldBe false
                }
                withClue("both Equipment — one moved off Grizzly Bears — are on the Dwarf (2 + 2 + 1)") {
                    game.attachedTo(bonesplitter) shouldBe dwarf
                    game.attachedTo(scimitar) shouldBe dwarf
                    projected.getPower(dwarf) shouldBe 5
                }
            }

            test("with two Dwarves you choose which one, and may leave Equipment where it is") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Armed and Armored")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Axgard Cavalry")
                    .withCardOnBattlefield(1, "Dwarven Soldier")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soldier = game.findPermanent("Dwarven Soldier")!!
                val bonesplitter = game.findPermanent("Bonesplitter")!!
                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.castSpell(1, "Armed and Armored").error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision()
                withClue("two Dwarves means a mid-resolution choice") {
                    (choice is ChooseTargetsDecision) shouldBe true
                }
                game.submitDecision(TargetsResponse(choice!!.id, mapOf(0 to listOf(soldier))))
                game.resolveStack()

                val select = game.getPendingDecision() as SelectCardsDecision
                game.submitDecision(CardsSelectedResponse(select.id, listOf(bonesplitter)))
                game.resolveStack()

                game.attachedTo(bonesplitter) shouldBe soldier
                game.attachedTo(scimitar) shouldBe null
                game.state.projectedState.getPower(soldier) shouldBe 4
            }
        }
    }
}
