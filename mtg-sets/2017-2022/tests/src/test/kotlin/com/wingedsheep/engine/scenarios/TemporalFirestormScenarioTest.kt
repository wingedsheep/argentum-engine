package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Temporal Firestorm (DMU #147) — "Kicker {1}{W} and/or {1}{U}. Choose up to X creatures and/or
 * planeswalkers you control, where X is the number of times this spell was kicked. Those permanents
 * phase out. Temporal Firestorm deals 5 damage to each creature and each planeswalker."
 *
 * X is summed from the two linked kicker slots on the *resolving spell*, so it's 0, 1 or 2; the
 * chosen permanents phase out before the sweep and the sweep skips them (CR 702.26b).
 */
class TemporalFirestormScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Temporal Firestorm")
        .withLandsOnBattlefield(1, "Mountain", 7)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardOnBattlefield(1, "Force of Nature")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast through the server-offered action for [variant] (null = unkicked) and pass until it resolves or pauses. */
    private fun TestGame.cast(variant: String?) {
        val cardId = findCardsInHand(1, "Temporal Firestorm").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    private fun TestGame.finish() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) {
            passPriority().error shouldBe null
        }
        checkStateBasedActions()
    }

    private fun TestGame.isPhasedOut(id: EntityId): Boolean =
        state.getEntity(id)?.get<PhasedOutComponent>() != null

    init {
        test("unkicked: nothing is chosen and every creature takes 5") {
            val game = board()
            game.cast(null)
            withClue("X = 0 asks nothing") { game.hasPendingDecision() shouldBe false }
            game.finish()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Force of Nature") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("kicked once: one chosen creature phases out and survives the sweep") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser")!!
            game.cast("Kicked {1}{W}")

            withClue("X = 1 prompts the choice") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(courser)).error shouldBe null
            game.finish()

            withClue("the chosen Courser is phased out, not dead") {
                game.isInGraveyard(1, "Centaur Courser") shouldBe false
                game.isPhasedOut(courser) shouldBe true
            }
            game.isInGraveyard(1, "Force of Nature") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("kicked once: choosing two is rejected") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser")!!
            val force = game.findPermanent("Force of Nature")!!
            game.cast("Kicked {1}{U}")

            game.hasPendingDecision() shouldBe true
            (game.selectCards(listOf(courser, force)).error != null) shouldBe true
        }

        test("kicked twice: both of your creatures phase out; the opponent's still dies") {
            val game = board()
            val courser = game.findPermanent("Centaur Courser")!!
            val force = game.findPermanent("Force of Nature")!!
            game.cast("Kicked {1}{W} + {1}{U}")

            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(courser, force)).error shouldBe null
            game.finish()

            game.isPhasedOut(courser) shouldBe true
            game.isPhasedOut(force) shouldBe true
            game.isInGraveyard(1, "Centaur Courser") shouldBe false
            game.isInGraveyard(1, "Force of Nature") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
