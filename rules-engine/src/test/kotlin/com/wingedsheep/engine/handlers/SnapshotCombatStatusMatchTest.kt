package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A damage recipient that the damage removed from the battlefield is matched against its frozen
 * last-known information ([PredicateEvaluator.matchesRecipient] → `matchesSnapshot`). The combat
 * status the snapshot freezes — attacking, blocking — answers state predicates, so a blocker the
 * damage killed is still "a blocking creature" (CR 603.10, Kusari-Gama). Every other state
 * predicate stays *unknown*: it never matches, and negating it doesn't make it match either.
 */
class SnapshotCombatStatusMatchTest : FunSpec({

    val evaluator = PredicateEvaluator(cardRegistry = null)

    fun setup(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        // Off the battlefield, so the recipient match reads the snapshot rather than live state.
        val dead = driver.putCardInGraveyard(driver.player2, "Grizzly Bears")
        return driver to dead
    }

    fun GameTestDriver.recipientMatches(
        entity: com.wingedsheep.sdk.model.EntityId,
        filter: GameObjectFilter,
        snapshot: EntitySnapshot,
    ): Boolean = evaluator.matchesRecipient(
        state, state.projectedState, entity, Recipient.Object(filter),
        PredicateContext(controllerId = player1),
        lastKnown = snapshot,
    )

    fun snapshotOf(
        entity: com.wingedsheep.sdk.model.EntityId,
        wasBlocking: Boolean = false,
        wasAttacking: Boolean = false,
    ) = EntitySnapshot(
        entityId = entity,
        typeLine = TypeLine.parse("Creature - Bear"),
        wasBlocking = wasBlocking,
        wasAttacking = wasAttacking,
    )

    test("a blocker that has left the battlefield still matches a blocking-creature filter") {
        val (driver, dead) = setup()
        driver.recipientMatches(dead, GameObjectFilter.Creature.blocking(), snapshotOf(dead, wasBlocking = true)) shouldBe true
    }

    test("a creature that wasn't blocking doesn't match") {
        val (driver, dead) = setup()
        driver.recipientMatches(dead, GameObjectFilter.Creature.blocking(), snapshotOf(dead)) shouldBe false
    }

    test("attacking status is answered from the snapshot too") {
        val (driver, dead) = setup()
        driver.recipientMatches(dead, GameObjectFilter.Creature.attacking(), snapshotOf(dead, wasAttacking = true)) shouldBe true
        driver.recipientMatches(dead, GameObjectFilter.Creature.attacking(), snapshotOf(dead, wasBlocking = true)) shouldBe false
    }

    test("a negated combat predicate is answered: a non-blocker matches 'not blocking'") {
        val (driver, dead) = setup()
        val notBlocking = GameObjectFilter.Creature.withStatePredicate(StatePredicate.Not(StatePredicate.IsBlocking))
        driver.recipientMatches(dead, notBlocking, snapshotOf(dead)) shouldBe true
        driver.recipientMatches(dead, notBlocking, snapshotOf(dead, wasBlocking = true)) shouldBe false
    }

    test("a state predicate the snapshot doesn't freeze stays unknown, negated or not") {
        val (driver, dead) = setup()
        driver.recipientMatches(dead, GameObjectFilter.Creature.tapped(), snapshotOf(dead, wasBlocking = true)) shouldBe false
        val untappedByNegation = GameObjectFilter.Creature.withStatePredicate(StatePredicate.Not(StatePredicate.IsTapped))
        driver.recipientMatches(dead, untappedByNegation, snapshotOf(dead, wasBlocking = true)) shouldBe false
    }

    test("an unknown operand keeps an And unknown, while an Or with a known true matches") {
        val (driver, dead) = setup()
        val snapshot = snapshotOf(dead, wasBlocking = true)
        val blockingAndTapped = GameObjectFilter.Creature.withStatePredicate(
            StatePredicate.And(listOf(StatePredicate.IsBlocking, StatePredicate.IsTapped))
        )
        val blockingOrTapped = GameObjectFilter.Creature.withStatePredicate(
            StatePredicate.Or(listOf(StatePredicate.IsBlocking, StatePredicate.IsTapped))
        )
        driver.recipientMatches(dead, blockingAndTapped, snapshot) shouldBe false
        driver.recipientMatches(dead, blockingOrTapped, snapshot) shouldBe true
    }
})
