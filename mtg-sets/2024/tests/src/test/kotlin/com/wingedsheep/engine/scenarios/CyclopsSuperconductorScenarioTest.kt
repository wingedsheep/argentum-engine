package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cyclops Superconductor (MH3 #182) — {1}{U}{R} 2/2 Creature — Cyclops Wizard.
 *
 * "Prowess. When this creature enters, you get {E}{E}{E}. When this creature dies, you may pay
 *  {E}{E}{E}. When you do, this creature deals damage equal to its power to any target."
 */
class CyclopsSuperconductorScenarioTest : ScenarioTestBase() {

    private fun TestGame.energy(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.seedEnergy(playerId: EntityId, amount: Int) {
        state = state.updateEntity(playerId) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun boltBoard() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Cyclops Superconductor")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Resolve everything, answering the pay prompt with [pay] and aiming the damage at [damageTarget]. */
    private fun TestGame.drain(pay: Boolean, damageTarget: EntityId): List<String> {
        val seen = mutableListOf<String>()
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 30) {
            when (val d = state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> { seen += "yesno"; answerYesNo(pay).error shouldBe null }
                is ChooseTargetsDecision -> {
                    seen += "target"
                    submitDecision(TargetsResponse(d.id, mapOf(0 to listOf(damageTarget)))).error shouldBe null
                }
                else -> error("unexpected decision $d")
            }
        }
        return seen
    }

    init {
        test("entering gives three energy") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cyclops Superconductor")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cyclops Superconductor").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Cyclops Superconductor") shouldBe true
            game.energy(game.player1Id) shouldBe 3
        }

        test("paying three energy when it dies deals damage equal to its last-known power, prowess included") {
            val game = boltBoard()
            game.seedEnergy(game.player1Id, 4)
            val cyclops = game.findPermanent("Cyclops Superconductor")!!

            // Casting Bolt triggers prowess (resolves first, making it 3/3); Bolt's 3 damage then kills it.
            game.castSpell(1, "Lightning Bolt", cyclops).error shouldBe null
            val seen = game.drain(pay = true, damageTarget = game.player2Id)

            withClue("decisions: $seen") { seen shouldBe listOf("yesno", "target") }
            game.isInGraveyard(1, "Cyclops Superconductor") shouldBe true
            withClue("three energy paid") { game.energy(game.player1Id) shouldBe 1 }
            withClue("dealt 3 (last-known power after prowess) to the opponent") {
                game.getLifeTotal(2) shouldBe 17
            }
        }

        test("declining the payment deals no damage and keeps the energy") {
            val game = boltBoard()
            game.seedEnergy(game.player1Id, 3)
            val cyclops = game.findPermanent("Cyclops Superconductor")!!

            game.castSpell(1, "Lightning Bolt", cyclops).error shouldBe null
            val seen = game.drain(pay = false, damageTarget = game.player2Id)

            withClue("decisions: $seen") { seen shouldBe listOf("yesno") }
            game.isInGraveyard(1, "Cyclops Superconductor") shouldBe true
            game.energy(game.player1Id) shouldBe 3
            game.getLifeTotal(2) shouldBe 20
        }

        test("with fewer than three energy there is no prompt and no damage") {
            val game = boltBoard()
            game.seedEnergy(game.player1Id, 2)
            val cyclops = game.findPermanent("Cyclops Superconductor")!!

            game.castSpell(1, "Lightning Bolt", cyclops).error shouldBe null
            val seen = game.drain(pay = true, damageTarget = game.player2Id)

            withClue("decisions: $seen") { seen shouldBe emptyList() }
            game.isInGraveyard(1, "Cyclops Superconductor") shouldBe true
            game.energy(game.player1Id) shouldBe 2
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
