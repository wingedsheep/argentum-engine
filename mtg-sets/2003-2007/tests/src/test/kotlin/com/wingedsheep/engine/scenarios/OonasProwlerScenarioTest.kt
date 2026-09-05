package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.OonasProwler
import com.wingedsheep.mtg.sets.definitions.tsp.cards.MomentaryBlink
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OonasProwlerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(OonasProwler, MomentaryBlink))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castProwler(): EntityId {
        val id = putCardInHand(player1, OonasProwler.name)
        giveMana(player1, Color.BLACK, 2)
        castSpell(player1, id).error shouldBe null
        bothPass().error shouldBe null
        return id
    }
    fun GameTestDriver.activateFromMenu(player: EntityId, source: EntityId, discard: EntityId) {
        if (state.priorityPlayerId != player) passPriority(state.priorityPlayerId!!).error shouldBe null
        val option = legalActions(player).single { (it.action as? ActivateAbility)?.sourceId == source }
        option.additionalCostInfo!!.costType shouldBe "DiscardCard"
        (discard in option.additionalCostInfo!!.validDiscardTargets) shouldBe true
        submit((option.action as ActivateAbility).copy(
            costPayment = AdditionalCostPayment(discardedCards = listOf(discard))
        )).error shouldBe null
    }

    test("opponent pays their own discard and modifies the source until cleanup") {
        val d = driver()
        val source = d.castProwler()
        val discarded = d.putCardInHand(d.player2, "Forest")
        val controllersHand = d.getHand(d.player1).toList()
        d.activateFromMenu(d.player2, source, discarded)
        (discarded in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
        d.getHand(d.player1) shouldBe controllersHand
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(source) shouldBe 1
        d.state.projectedState.getToughness(source) shouldBe 1
        d.state.projectedState.getController(source) shouldBe d.player1
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.getPower(source) shouldBe 3
    }

    test("controller can activate repeatedly without tapping") {
        val d = driver()
        val source = d.castProwler()
        repeat(2) {
            val discarded = d.putCardInHand(d.player1, "Forest")
            d.activateFromMenu(d.player1, source, discarded)
            d.bothPass().error shouldBe null
        }
        d.state.projectedState.getPower(source) shouldBe -1
        d.state.projectedState.getToughness(source) shouldBe 1
        d.isTapped(source) shouldBe false
    }

    test("opponent activation cannot modify source after blink") {
        val d = driver()
        val source = d.castProwler()
        val discarded = d.putCardInHand(d.player2, "Forest")
        d.activateFromMenu(d.player2, source, discarded)
        d.passPriority(d.player2).error shouldBe null
        val blink = d.putCardInHand(d.player1, "Momentary Blink")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, blink, listOf(source)).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(source) shouldBe 3
        d.state.projectedState.getToughness(source) shouldBe 1
        (discarded in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
    }
})
