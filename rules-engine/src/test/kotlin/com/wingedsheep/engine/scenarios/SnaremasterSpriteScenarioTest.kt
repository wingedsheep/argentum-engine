package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Snaremaster Sprite — {U} Creature — Faerie Wizard 1/1 (WOE).
 *
 * Flying
 * When this creature enters, you may pay {2}. When you do, tap target creature an opponent
 * controls and put a stun counter on it.
 *
 * The enters trigger is an optional mana payment whose follow-up is a *reflexive* triggered
 * ability (CR 603.12), so the decision order at resolution is the load-bearing part:
 * [YesNoDecision] ("Pay {2}?") → [SelectManaSourcesDecision] → and only once the {2} is actually
 * paid, [ChooseTargetsDecision] for the reflexive ability. Declining short-circuits before any
 * target is ever asked for. These tests pin both branches plus the stun counter's effect on the
 * next untap step.
 */
class SnaremasterSpriteScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers()
        .withCardInHand(1, "Snaremaster Sprite")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("paying {2} taps the target and puts a stun counter on it") {
            val g = game()
            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Snaremaster Sprite").error shouldBe null
            g.resolveStack()

            (g.getPendingDecision() is YesNoDecision) shouldBe true
            g.answerYesNo(true).error shouldBe null

            (g.getPendingDecision() is SelectManaSourcesDecision) shouldBe true
            g.submitManaSourcesAutoPay().error shouldBe null

            // The target is only asked for *after* the {2} is paid — the reflexive trigger.
            (g.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            g.selectTargets(listOf(bears)).error shouldBe null
            g.resolveStack()

            g.isTapped(bears) shouldBe true
            g.stunCounters(bears) shouldBe 1
        }

        test("declining the payment leaves the opponent's creature untouched") {
            val g = game()
            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Snaremaster Sprite").error shouldBe null
            g.resolveStack()

            (g.getPendingDecision() is YesNoDecision) shouldBe true
            g.answerYesNo(false).error shouldBe null
            g.resolveStack()

            // No reflexive trigger, so no target was ever asked for.
            g.hasPendingDecision() shouldBe false
            g.isTapped(bears) shouldBe false
            g.stunCounters(bears) shouldBe 0
        }

        test("the stun counter is consumed instead of untapping the creature") {
            val g = game()
            val bears = g.findPermanent("Grizzly Bears")!!

            g.castSpell(1, "Snaremaster Sprite").error shouldBe null
            g.resolveStack()
            g.answerYesNo(true).error shouldBe null
            g.submitManaSourcesAutoPay().error shouldBe null
            g.selectTargets(listOf(bears)).error shouldBe null
            g.resolveStack()
            g.isTapped(bears) shouldBe true

            // Hand the turn over: the Bears' controller's untap step removes the stun counter
            // rather than untapping it.
            g.passUntilPhase(Phase.ENDING, Step.END)
            g.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            g.state.activePlayerId shouldBe g.player2Id

            g.stunCounters(bears) shouldBe 0
            g.isTapped(bears) shouldBe true
        }
    }

    private fun TestGame.stunCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null
}
