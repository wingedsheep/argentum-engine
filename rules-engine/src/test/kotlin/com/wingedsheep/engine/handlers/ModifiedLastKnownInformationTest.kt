package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Modified" (CR 700.9) answered from last-known information once the permanent has left the
 * battlefield (CR 608.2h): [StatePredicate.IsModified] reads [EntitySnapshot.wasModified] — off the
 * card's [LastKnownPermanentComponent] for a live predicate match, and off the frozen snapshot for
 * a recipient match. Ian the Reckless's resolution recheck (ruling 2024-03-08) is the card case.
 */
class ModifiedLastKnownInformationTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val modified = GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified)

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver to driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
    }

    fun snapshotOf(entity: EntityId, wasModified: Boolean) = EntitySnapshot(
        entityId = entity,
        typeLine = TypeLine.parse("Creature - Bear"),
        wasModified = wasModified,
    )

    fun GameTestDriver.liveMatches(entity: EntityId): Boolean =
        evaluator.matches(state, state.projectedState, entity, modified, PredicateContext(controllerId = player1))

    test("a card that left the battlefield modified still matches IsModified") {
        val (driver, dead) = setup()
        driver.replaceState(
            driver.state.updateEntity(dead) { it.with(LastKnownPermanentComponent(snapshotOf(dead, wasModified = true))) }
        )
        driver.liveMatches(dead) shouldBe true
    }

    test("a card that left unmodified, or never was a permanent, doesn't match") {
        val (driver, dead) = setup()
        driver.liveMatches(dead) shouldBe false
        driver.replaceState(
            driver.state.updateEntity(dead) { it.with(LastKnownPermanentComponent(snapshotOf(dead, wasModified = false))) }
        )
        driver.liveMatches(dead) shouldBe false
    }

    test("a recipient match answers IsModified from the snapshot, negation included") {
        val (driver, dead) = setup()
        fun recipient(filter: GameObjectFilter, snapshot: EntitySnapshot) = evaluator.matchesRecipient(
            driver.state, driver.state.projectedState, dead, Recipient.Object(filter),
            PredicateContext(controllerId = driver.player1),
            lastKnown = snapshot,
        )
        val unmodified = GameObjectFilter.Creature.withStatePredicate(StatePredicate.Not(StatePredicate.IsModified))
        recipient(modified, snapshotOf(dead, wasModified = true)) shouldBe true
        recipient(modified, snapshotOf(dead, wasModified = false)) shouldBe false
        recipient(unmodified, snapshotOf(dead, wasModified = false)) shouldBe true
    }
})
