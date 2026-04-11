package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Direct tests for [CostHandler.payAbilityCost] with [AbilityCost.DiscardSelf].
 *
 * No card in the current set list uses DiscardSelf yet, so we exercise the code path
 * by paying the cost directly against a hand card. This pins down the contract so that
 * the first card to use "discard this card" as a cost doesn't silently misbehave.
 */
class CostHandlerDiscardSelfTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("DiscardSelf can be paid — moves card from hand to graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val cardId = driver.putCardInHand(player, "Grizzly Bears")
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(player, Zone.HAND))
            .shouldContain(cardId)

        val costHandler = CostHandler()
        costHandler.canPayAbilityCost(
            state = driver.state,
            cost = AbilityCost.DiscardSelf,
            sourceId = cardId,
            controllerId = player,
            manaPool = ManaPool()
        ) shouldBe true

        val result = costHandler.payAbilityCost(
            state = driver.state,
            cost = AbilityCost.DiscardSelf,
            sourceId = cardId,
            controllerId = player,
            manaPool = ManaPool()
        )

        result.success shouldBe true
        val newState = result.newState!!
        newState.getZone(com.wingedsheep.engine.state.ZoneKey(player, Zone.HAND))
            .contains(cardId) shouldBe false
        newState.getZone(com.wingedsheep.engine.state.ZoneKey(player, Zone.GRAVEYARD))
            .contains(cardId) shouldBe true

        // Events: both a CardsDiscardedEvent and a ZoneChangeEvent from HAND→GRAVEYARD.
        result.events.any { it is CardsDiscardedEvent && it.cardIds.contains(cardId) } shouldBe true
        val zoneChange = result.events.filterIsInstance<ZoneChangeEvent>().firstOrNull { it.entityId == cardId }
        zoneChange shouldNotBe null
        zoneChange!!.fromZone shouldBe Zone.HAND
        zoneChange.toZone shouldBe Zone.GRAVEYARD
        zoneChange.ownerId shouldBe player
    }

    test("DiscardSelf fails cleanly when the source card is not in its owner's hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Card exists but is in the graveyard, not the hand.
        val cardId = driver.putCardInGraveyard(player, "Grizzly Bears")

        val costHandler = CostHandler()
        costHandler.canPayAbilityCost(
            state = driver.state,
            cost = AbilityCost.DiscardSelf,
            sourceId = cardId,
            controllerId = player,
            manaPool = ManaPool()
        ) shouldBe false

        val result = costHandler.payAbilityCost(
            state = driver.state,
            cost = AbilityCost.DiscardSelf,
            sourceId = cardId,
            controllerId = player,
            manaPool = ManaPool()
        )
        result.success shouldBe false
    }
})
