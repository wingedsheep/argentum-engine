package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.identity.PutIntoGraveyardThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for `StatePredicate.PutIntoGraveyardThisTurn`, the zone-agnostic graveyard filter
 * behind FDN's Abyssal Harvester ("target creature card from a graveyard that was put there
 * this turn").
 *
 * It is the sibling of `PutIntoGraveyardFromBattlefieldThisTurn` (Samwise / Lobelia, LTR) and
 * reads the same `PutIntoGraveyardThisTurnComponent`, ignoring its `fromBattlefield` flag —
 * so a milled, discarded, or countered card qualifies where the LTR predicate does not. The
 * marker is stripped when the card leaves the graveyard and wiped by `BeginningPhaseManager`
 * at the untap step of every turn, which is what gives the "this turn" window MTG-correct
 * per-turn semantics rather than the engine's per-round turn counter.
 */
class PutIntoGraveyardThisTurnTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Swamp" to 10,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.matchesAnyOrigin(entityId: EntityId): Boolean =
        PredicateEvaluator().matchesStatePredicate(
            state = state,
            entityId = entityId,
            predicate = StatePredicate.PutIntoGraveyardThisTurn,
            context = null
        )

    fun GameTestDriver.matchesFromBattlefield(entityId: EntityId): Boolean =
        PredicateEvaluator().matchesStatePredicate(
            state = state,
            entityId = entityId,
            predicate = StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn,
            context = null
        )

    test("a creature destroyed this turn matches — as it does for the battlefield-only sibling") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.castSpellWithTargets(
            player,
            doomBlade,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(victim))
        )
        driver.bothPass()

        driver.matchesAnyOrigin(victim) shouldBe true
        driver.matchesFromBattlefield(victim) shouldBe true
        driver.state.getEntity(victim)
            ?.get<PutIntoGraveyardThisTurnComponent>()?.fromBattlefield shouldBe true
    }

    test("a card milled from library into the graveyard matches, but not as 'from battlefield'") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val card = driver.putCardInHand(player, "Grizzly Bears")
        val toLibrary = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = card,
            destinationZone = Zone.LIBRARY
        )
        driver.replaceState(toLibrary.state)
        val toGraveyard = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = card,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(toGraveyard.state)

        // This is the whole point of the new predicate: the LTR one says no, this one says yes.
        driver.matchesAnyOrigin(card) shouldBe true
        driver.matchesFromBattlefield(card) shouldBe false
        driver.state.getEntity(card)
            ?.get<PutIntoGraveyardThisTurnComponent>()?.fromBattlefield shouldBe false
    }

    test("a card discarded from hand into the graveyard matches") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val card = driver.putCardInHand(player, "Grizzly Bears")
        val toGraveyard = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = card,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(toGraveyard.state)

        driver.matchesAnyOrigin(card) shouldBe true
        driver.matchesFromBattlefield(card) shouldBe false
    }

    test("a card that was already in a graveyard when play began does not match") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // putCardInGraveyard writes the zone directly rather than going through
        // ZoneTransitionService — nothing "put it there", so there is no arrival to report.
        val planted = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.matchesAnyOrigin(planted) shouldBe false
        driver.matchesFromBattlefield(planted) shouldBe false
    }

    test("marker is stripped when the card leaves the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val toGraveyard = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = bear,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(toGraveyard.state)
        driver.matchesAnyOrigin(bear) shouldBe true

        val toExile = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = bear,
            destinationZone = Zone.EXILE
        )
        driver.replaceState(toExile.state)
        driver.state.getEntity(bear)
            ?.get<PutIntoGraveyardThisTurnComponent>() shouldBe null
        driver.matchesAnyOrigin(bear) shouldBe false
    }

    test("a battlefield arrival overwrites an earlier non-battlefield one for the same card") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Hand → graveyard (not from the battlefield), then graveyard → battlefield → graveyard.
        val bear = driver.putCardInHand(player, "Grizzly Bears")
        fun move(dest: Zone) {
            driver.replaceState(
                com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    state = driver.state, entityId = bear, destinationZone = dest
                ).state
            )
        }
        move(Zone.GRAVEYARD)
        driver.matchesFromBattlefield(bear) shouldBe false

        move(Zone.BATTLEFIELD)
        move(Zone.GRAVEYARD)
        driver.matchesAnyOrigin(bear) shouldBe true
        driver.matchesFromBattlefield(bear) shouldBe true
    }

    test("a card put into a graveyard last turn does not match on a later turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.castSpellWithTargets(
            player,
            doomBlade,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(victim))
        )
        driver.bothPass()
        driver.matchesAnyOrigin(victim) shouldBe true

        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent
        // New turn → BeginningPhaseManager wiped the marker during the untap step.
        driver.matchesAnyOrigin(victim) shouldBe false
    }
})
