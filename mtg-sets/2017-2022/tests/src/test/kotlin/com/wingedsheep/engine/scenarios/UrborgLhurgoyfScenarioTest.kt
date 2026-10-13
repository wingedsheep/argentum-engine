package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Urborg Lhurgoyf — "Kicker {U} and/or {B}" (two kicker abilities, CR 702.33b) feeding an as-enters
 * mill of three cards per kicker paid, read off the linked kicker slots on the entering permanent.
 * Its power counts creature cards in *your* graveyard only (toughness is that plus 1), so it
 * includes the creatures it just milled and ignores the opponent's.
 */
class UrborgLhurgoyfScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Urborg Lhurgoyf")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
        return builder.build()
    }

    /** Cast through the server-offered action for [variant] (null = unkicked) and resolve it. */
    private fun TestGame.castAndResolve(variant: String?) {
        val cardId = findCardsInHand(1, "Urborg Lhurgoyf").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority().error shouldBe null
        state.stack.isEmpty() shouldBe true
    }

    private fun TestGame.assertLhurgoyf(power: Int) {
        val id = findPermanent("Urborg Lhurgoyf")!!
        state.projectedState.getPower(id) shouldBe power
        state.projectedState.getToughness(id) shouldBe power + 1
    }

    init {
        test("unkicked: mills nothing, counts only your graveyard") {
            val game = board()
            game.castAndResolve(null)
            game.librarySize(1) shouldBe 10
            game.graveyardSize(1) shouldBe 1
            game.assertLhurgoyf(1)
        }

        test("kicked with {U} only: mills three") {
            val game = board()
            game.castAndResolve("Kicked {U}")
            game.librarySize(1) shouldBe 7
            game.graveyardSize(1) shouldBe 4
            game.assertLhurgoyf(4)
        }

        test("kicked with {B} only: mills three") {
            val game = board()
            game.castAndResolve("Kicked {B}")
            game.librarySize(1) shouldBe 7
            game.assertLhurgoyf(4)
        }

        test("kicked twice: mills six") {
            val game = board()
            game.castAndResolve("Kicked {U} + {B}")
            game.librarySize(1) shouldBe 4
            game.graveyardSize(1) shouldBe 7
            game.assertLhurgoyf(7)
        }
    }
}
