package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.LandsEdge
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Land's Edge — "Discard a card: If the discarded card was a land card, this enchantment deals
 * 2 damage to target player or planeswalker. Any player may activate this ability."
 *
 * Covers the opponent activating it: they discard from their own hand, they pick the target, and
 * the land check reads the card they discarded.
 */
class LandsEdgeScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LandsEdge))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.activate(player: EntityId, source: EntityId, discard: EntityId, victim: EntityId) {
        if (state.priorityPlayerId != player) passPriority(state.priorityPlayerId!!).error shouldBe null
        val option = legalActions(player).single { (it.action as? ActivateAbility)?.sourceId == source }
        (discard in option.additionalCostInfo!!.validDiscardTargets) shouldBe true
        submit(
            (option.action as ActivateAbility).copy(
                targets = listOf(ChosenTarget.Player(victim)),
                costPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
            )
        ).error shouldBe null
    }

    test("an opponent discarding a land deals 2 damage to the player they target") {
        val d = driver()
        val edge = d.putPermanentOnBattlefield(d.player1, LandsEdge.name)
        val forest = d.putCardInHand(d.player2, "Forest")
        val controllersHand = d.getHand(d.player1).toList()
        val life = d.getLifeTotal(d.player1)

        d.activate(d.player2, edge, forest, d.player1)
        (forest in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
        d.getHand(d.player1) shouldBe controllersHand
        d.bothPass().error shouldBe null

        d.getLifeTotal(d.player1) shouldBe life - 2
    }

    test("discarding a nonland card deals no damage") {
        val d = driver()
        val edge = d.putPermanentOnBattlefield(d.player1, LandsEdge.name)
        val lions = d.putCardInHand(d.player2, "Savannah Lions")
        val life = d.getLifeTotal(d.player1)

        d.activate(d.player2, edge, lions, d.player1)
        d.bothPass().error shouldBe null

        (lions in d.state.getZone(d.player2, Zone.GRAVEYARD)) shouldBe true
        d.getLifeTotal(d.player1) shouldBe life
    }

    test("the controller can activate it too") {
        val d = driver()
        val edge = d.putPermanentOnBattlefield(d.player1, LandsEdge.name)
        val forest = d.putCardInHand(d.player1, "Forest")
        val life = d.getLifeTotal(d.player2)

        d.activate(d.player1, edge, forest, d.player2)
        d.bothPass().error shouldBe null

        d.getLifeTotal(d.player2) shouldBe life - 2
    }
})
