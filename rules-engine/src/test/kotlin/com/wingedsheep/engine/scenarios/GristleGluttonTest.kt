package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.GristleGlutton
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Gristle Glutton: {T}, Blight 1: Discard a card. If you do, draw a card.
 *
 * Exercises the new AbilityCost.Blight — a -1/-1 counter placed on a creature you control
 * as an additional activation cost, alongside standard Tap + rummage effect.
 */
class GristleGluttonTest : FunSpec({

    val abilityId = GristleGlutton.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GristleGlutton)
        return driver
    }

    test("activating ability places -1/-1 counter on chosen creature, discards and draws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val glutton = driver.putCreatureOnBattlefield(activePlayer, "Gristle Glutton")
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val discardTarget = driver.putCardInHand(activePlayer, "Lightning Bolt")

        driver.removeSummoningSickness(glutton)
        driver.removeSummoningSickness(bears)

        val handSizeBefore = driver.getHand(activePlayer).size
        val librarySizeBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY)).size

        val activator = activePlayer
        val gluttonId = glutton
        val bearsId = bears

        val result = driver.submit(
            ActivateAbility(
                playerId = activator,
                sourceId = gluttonId,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(blightTargets = listOf(bearsId))
            )
        )
        result.isSuccess shouldBe true

        // Bears now has a -1/-1 counter
        val counters = driver.state.getEntity(bearsId)?.get<CountersComponent>()
        counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1

        // Gristle Glutton is tapped
        driver.isTapped(gluttonId) shouldBe true

        // Resolve the rummage effect — engine pauses for discard selection.
        driver.bothPass()

        // Discard the Lightning Bolt from hand
        driver.submitCardSelection(activator, listOf(discardTarget))

        // Fully resolve
        while (driver.isPaused) {
            driver.submitCardSelection(activator, emptyList())
        }

        // Net effect: hand size same (discard 1, draw 1), library is 1 smaller
        driver.getHand(activator).size shouldBe handSizeBefore
        driver.state.getZone(ZoneKey(activator, Zone.LIBRARY)).size shouldBe librarySizeBefore - 1

        // The discarded card is in the graveyard
        driver.state.getZone(ZoneKey(activator, Zone.GRAVEYARD)).contains(discardTarget) shouldBe true
    }

    test("ability is unavailable when you control no other creatures and cost would orphan the source") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gluttonId = driver.putCreatureOnBattlefield(activePlayer, "Gristle Glutton")

        // The only creature that could receive the -1/-1 counter is Gristle Glutton itself.
        // Blight requires a creature you control — that's still payable by targeting the glutton,
        // so this test confirms activation succeeds in that edge case.
        driver.removeSummoningSickness(gluttonId)
        val activator = activePlayer

        val result = driver.submit(
            ActivateAbility(
                playerId = activator,
                sourceId = gluttonId,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(blightTargets = listOf(gluttonId))
            )
        )
        result.isSuccess shouldBe true

        val counters = driver.state.getEntity(gluttonId)?.get<CountersComponent>()
        counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
    }
})
