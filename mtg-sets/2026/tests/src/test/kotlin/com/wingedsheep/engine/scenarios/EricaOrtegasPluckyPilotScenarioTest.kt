package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.EricaOrtegasPluckyPilot
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Erica Ortegas, Plucky Pilot — "Whenever Erica Ortegas becomes tapped, you may put a land card
 * from your hand onto the battlefield. If you do, return a land you control to its owner's hand at
 * the beginning of the next end step."
 *
 * Attacking taps her (she has haste, so she can attack the turn she arrives). The returned land is
 * chosen when the delayed trigger resolves and may be any land you control — here an untapped land
 * that was already in play, not the one she put down.
 */
class EricaOrtegasPluckyPilotScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(EricaOrtegasPluckyPilot))
        it.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Attack with Erica and let her becomes-tapped trigger resolve up to its hand choice. */
    fun GameTestDriver.attackWithErica(me: EntityId, opp: EntityId, erica: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(erica), opp).error shouldBe null
        isTapped(erica) shouldBe true
        var guard = 0
        while (pendingDecision == null && state.stack.isNotEmpty() && guard < 10) { bothPass(); guard++ }
    }

    test("she puts a land from hand onto the battlefield; at the end step a chosen land returns to hand") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val erica = d.putCreatureOnBattlefield(me, "Erica Ortegas, Plucky Pilot")
        val oldForest = d.putLandOnBattlefield(me, "Forest")
        val mountain = d.putCardInHand(me, "Mountain")

        d.attackWithErica(me, opp, erica)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(mountain)).error shouldBe null

        withClue("the Mountain went from hand to the battlefield") {
            d.state.getBattlefield() shouldContain mountain
            d.getHand(me) shouldNotContain mountain
        }

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.END)
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard < 10) { d.bothPass(); guard++ }

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, listOf(oldForest)).error shouldBe null
        while (d.state.stack.isNotEmpty() && guard < 20) { d.bothPass(); guard++ }

        withClue("any land you control may be returned — not necessarily the one she put down") {
            d.getHand(me) shouldContain oldForest
            d.state.getBattlefield() shouldContain mountain
        }
    }

    test("declining to put a land creates no delayed trigger") {
        val d = newDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val erica = d.putCreatureOnBattlefield(me, "Erica Ortegas, Plucky Pilot")
        val oldForest = d.putLandOnBattlefield(me, "Forest")
        d.putCardInHand(me, "Mountain")

        d.attackWithErica(me, opp, erica)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(me, emptyList()).error shouldBe null

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.END)

        withClue("no land was put down, so nothing returns at the end step") {
            d.pendingDecision shouldBe null
            d.state.stack.isEmpty() shouldBe true
            d.state.getBattlefield() shouldContain oldForest
        }
    }
})
