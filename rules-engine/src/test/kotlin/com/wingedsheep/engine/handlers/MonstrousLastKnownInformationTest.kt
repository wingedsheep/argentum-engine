package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.state.components.stack.captureLastKnown
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
 * The monstrous designation (CR 701.37b) answered from last-known information once the permanent
 * has left the battlefield (CR 608.2h): [StatePredicate.IsMonstrous] reads
 * [EntitySnapshot.wasMonstrous] — off the card's [LastKnownPermanentComponent] for a live predicate
 * match, and off the frozen snapshot for a recipient match — and both snapshot captures freeze it.
 * Polis Crusher's resolution recheck of "if this creature is monstrous" (ruling 2013-09-15) is
 * the card case.
 */
class MonstrousLastKnownInformationTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)
    val monstrous = GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsMonstrous)

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver to driver.putCardInGraveyard(driver.player1, "Grizzly Bears")
    }

    fun snapshotOf(entity: EntityId, wasMonstrous: Boolean) = EntitySnapshot(
        entityId = entity,
        typeLine = TypeLine.parse("Creature - Bear"),
        wasMonstrous = wasMonstrous,
    )

    fun GameTestDriver.liveMatches(entity: EntityId): Boolean =
        evaluator.matches(state, state.projectedState, entity, monstrous, PredicateContext(controllerId = player1))

    test("a card that left the battlefield monstrous still matches IsMonstrous") {
        val (driver, dead) = setup()
        driver.replaceState(
            driver.state.updateEntity(dead) { it.with(LastKnownPermanentComponent(snapshotOf(dead, wasMonstrous = true))) }
        )
        driver.liveMatches(dead) shouldBe true
    }

    test("a card that left not monstrous, or never was a permanent, doesn't match") {
        val (driver, dead) = setup()
        driver.liveMatches(dead) shouldBe false
        driver.replaceState(
            driver.state.updateEntity(dead) { it.with(LastKnownPermanentComponent(snapshotOf(dead, wasMonstrous = false))) }
        )
        driver.liveMatches(dead) shouldBe false
    }

    test("a recipient match answers IsMonstrous from the snapshot, negation included") {
        val (driver, dead) = setup()
        fun recipient(filter: GameObjectFilter, snapshot: EntitySnapshot) = evaluator.matchesRecipient(
            driver.state, driver.state.projectedState, dead, Recipient.Object(filter),
            PredicateContext(controllerId = driver.player1),
            lastKnown = snapshot,
        )
        val notMonstrous = GameObjectFilter.Creature.withStatePredicate(StatePredicate.Not(StatePredicate.IsMonstrous))
        recipient(monstrous, snapshotOf(dead, wasMonstrous = true)) shouldBe true
        recipient(monstrous, snapshotOf(dead, wasMonstrous = false)) shouldBe false
        recipient(notMonstrous, snapshotOf(dead, wasMonstrous = false)) shouldBe true
    }

    test("the last-known capture freezes the monstrous designation") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        captureLastKnown(driver.state, bear).wasMonstrous shouldBe false
        driver.replaceState(driver.state.updateEntity(bear) { it.with(MonstrousComponent) })
        captureLastKnown(driver.state, bear).wasMonstrous shouldBe true
    }
})
