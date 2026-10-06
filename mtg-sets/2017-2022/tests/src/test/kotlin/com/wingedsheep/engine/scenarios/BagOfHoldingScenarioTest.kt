package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.BagOfHolding
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Bag of Holding (M20 #222) — {1} Artifact.
 *
 * "Whenever you discard a card, exile that card from your graveyard.
 *  {2}, {T}: Draw a card, then discard a card.
 *  {4}, {T}, Sacrifice this artifact: Return all cards exiled with this artifact to their owner's hand."
 *
 * Proves the linked exile: the discard trigger's exile is linked to the Bag, and the sacrifice
 * ability (whose source is already gone when it resolves) finds exactly that pile — not other
 * exiled cards.
 */
class BagOfHoldingScenarioTest : FunSpec({

    val lootAbilityId = BagOfHolding.activatedAbilities[0].id
    val returnAbilityId = BagOfHolding.activatedAbilities[1].id

    val trinket = card("Bag Test Trinket") {
        manaCost = "{3}"
        typeLine = "Artifact"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BagOfHolding)
        driver.registerCard(trinket)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle(discards: List<EntityId> = emptyList(), maxSteps: Int = 40) {
        repeat(maxSteps) {
            val decision = pendingDecision
            when {
                decision is SelectCardsDecision && discards.isNotEmpty() ->
                    submitCardSelection(decision.playerId, discards.take(decision.maxSelections))
                decision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    test("looting exiles the discarded card, and sacrificing the Bag returns only its own exiled cards") {
        val driver = newDriver()
        val me = driver.player1

        val bag = driver.putPermanentOnBattlefield(me, "Bag of Holding")
        val discarded = driver.putCardInHand(me, "Bag Test Trinket")
        val unrelatedExile = driver.putCardInExile(me, "Bag Test Trinket")

        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = bag, abilityId = lootAbilityId))
        driver.settle(discards = listOf(discarded))

        driver.state.getZone(me, Zone.GRAVEYARD).contains(discarded).shouldBeFalse()
        driver.state.getZone(me, Zone.EXILE).contains(discarded).shouldBeTrue()

        // Untap the Bag for the return ability.
        driver.untapPermanent(bag)

        driver.giveColorlessMana(me, 4)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = bag, abilityId = returnAbilityId))
        driver.settle()

        driver.state.getBattlefield().contains(bag).shouldBeFalse()
        driver.state.getZone(me, Zone.HAND).contains(discarded).shouldBeTrue()
        driver.state.getZone(me, Zone.EXILE).contains(unrelatedExile).shouldBeTrue()
        driver.state.getZone(me, Zone.HAND).contains(unrelatedExile).shouldBeFalse()
    }

    test("with two Bags, one discard is exiled once and the second trigger finds nothing") {
        val driver = newDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Bag of Holding")
        val bag2 = driver.putPermanentOnBattlefield(me, "Bag of Holding")
        val discarded = driver.putCardInHand(me, "Bag Test Trinket")

        // Two Bags: both trigger on one discard; whichever resolves first takes the card, the other
        // finds it gone from the graveyard and does nothing (ruling 2019-07-12).
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = bag2, abilityId = lootAbilityId))
        driver.settle(discards = listOf(discarded))

        driver.state.getZone(me, Zone.EXILE).contains(discarded).shouldBeTrue()
        driver.state.getZone(me, Zone.GRAVEYARD).contains(discarded).shouldBeFalse()
    }
})
