package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for `StatePredicate.PutIntoGraveyardThisCombat` (Storrev, Devkarin Lich's "wasn't put
 * there this combat"). It reads the `duringCombat` flag of the graveyard-arrival stamp: set on an
 * arrival during the combat phase, cleared by `CombatManager.endCombat` when that phase ends —
 * after which the card is still "put there this turn".
 */
class PutIntoGraveyardThisCombatTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 10, "Swamp" to 10, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.matches(entityId: EntityId, predicate: StatePredicate): Boolean =
        PredicateEvaluator(cardRegistry = null).matchesStatePredicate(
            state = state,
            entityId = entityId,
            predicate = predicate,
            context = null
        )

    fun GameTestDriver.discard(cardId: EntityId) {
        replaceState(zones.moveToZone(state = state, entityId = cardId, destinationZone = Zone.GRAVEYARD).state)
    }

    test("a card put into a graveyard before combat isn't put there this combat") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Grizzly Bears")
        driver.discard(card)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        driver.matches(card, StatePredicate.PutIntoGraveyardThisTurn) shouldBe true
        driver.matches(card, StatePredicate.PutIntoGraveyardThisCombat) shouldBe false
    }

    test("a card put into a graveyard during combat matches until that combat phase ends") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val card = driver.putCardInHand(player, "Grizzly Bears")
        driver.discard(card)
        driver.matches(card, StatePredicate.PutIntoGraveyardThisCombat) shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.matches(card, StatePredicate.PutIntoGraveyardThisCombat) shouldBe false
        driver.matches(card, StatePredicate.PutIntoGraveyardThisTurn) shouldBe true
    }

    test("a card that leaves the graveyard during combat loses the mark") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val card = driver.putCardInHand(player, "Grizzly Bears")
        driver.discard(card)
        driver.replaceState(
            driver.zones.moveToZone(state = driver.state, entityId = card, destinationZone = Zone.EXILE).state
        )

        driver.matches(card, StatePredicate.PutIntoGraveyardThisCombat) shouldBe false
    }
})
