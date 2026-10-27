package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Charismatic Conqueror (LCC #70) — {1}{W} Creature — Vampire Soldier 2/2.
 *
 * "Vigilance. Whenever an artifact or creature an opponent controls enters untapped, they may tap
 * that permanent. If they don't, you create a 1/1 white Vampire creature token with lifelink."
 *
 * The choice is the opponent's; a permanent already tapped when the ability resolves can't be
 * tapped, so the token comes without asking (2023-11-10 ruling).
 */
class CharismaticConquerorScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Charismatic Conqueror")
        .withCardInHand(2, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Forest", 2)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Bob casts Grizzly Bears and lets it resolve; the Conqueror's trigger is then on the stack. */
    private fun TestGame.bobCastsBears() {
        castSpell(2, "Grizzly Bears").error shouldBe null
        var guard = 0
        while (findPermanent("Grizzly Bears") == null && guard++ < 10) passPriority()
        state.stack.isNotEmpty() shouldBe true
    }

    init {
        test("the opponent taps the entering creature: no token") {
            val game = setup()
            game.bobCastsBears()
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<YesNoDecision>()
            decision.playerId shouldBe game.player2Id
            game.answerYesNo(true)
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.findPermanents("Vampire Token").size shouldBe 0
        }

        test("the opponent declines: Alice creates a 1/1 lifelink Vampire") {
            val game = setup()
            game.bobCastsBears()
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            val vampires = game.findPermanents("Vampire Token")
            vampires.size shouldBe 1
            val vampire = vampires.single()
            game.state.projectedState.getController(vampire) shouldBe game.player1Id
            game.state.projectedState.hasKeyword(vampire, Keyword.LIFELINK) shouldBe true
        }

        test("already tapped when the ability resolves: the token comes without asking") {
            val game = setup()
            game.bobCastsBears()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { it.with(TappedComponent) }

            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Vampire Token").size shouldBe 1
        }

        test("Alice's own creature entering doesn't trigger it") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Charismatic Conqueror")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }
    }
}
