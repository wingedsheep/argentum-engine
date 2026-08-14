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
 * Tests for `StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn`, the graveyard filter
 * behind LTR's Samwise the Stouthearted ("target permanent card in your graveyard that
 * was put there from the battlefield this turn") and Lobelia Sackville-Baggins
 * (analogous against an opponent's graveyard).
 *
 * The marker is set on the card entity by `ZoneTransitionService` on every graveyard
 * arrival and records whether that arrival came from the battlefield; this predicate
 * requires that flag. It is stripped when the card leaves the graveyard, so a later
 * arrival via mill or exile → graveyard doesn't falsely match. It carries no turn number —
 * `BeginningPhaseManager` wipes it from every entity during the untap step of each turn,
 * which is what gives the predicate MTG-correct per-turn semantics.
 */
class PutIntoGraveyardFromBattlefieldThisTurnTest : FunSpec({

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

    fun GameTestDriver.matches(entityId: EntityId): Boolean {
        val evaluator = PredicateEvaluator()
        return evaluator.matchesStatePredicate(
            state = state,
            entityId = entityId,
            predicate = StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn,
            context = null
        )
    }

    test("a creature destroyed this turn matches in the graveyard") {
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

        driver.matches(victim) shouldBe true
    }

    test("a card directly placed into the graveyard from hand/library does NOT match") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // putCardInGraveyard places the card into the graveyard without crossing the
        // battlefield, so the marker should never be set.
        val deadBear = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.matches(deadBear) shouldBe false
        // The marker may be present (the card did arrive in a graveyard this turn), but it
        // must not claim a battlefield origin.
        (driver.state.getEntity(deadBear)
            ?.get<PutIntoGraveyardThisTurnComponent>()?.fromBattlefield ?: false) shouldBe false
    }

    test("marker is stripped when the card leaves the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val moveToGraveyard = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = bear,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(moveToGraveyard.state)
        driver.matches(bear) shouldBe true

        // Exile it — leaving the graveyard.
        val moveToExile = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = bear,
            destinationZone = Zone.EXILE
        )
        driver.replaceState(moveToExile.state)
        driver.state.getEntity(bear)
            ?.get<PutIntoGraveyardThisTurnComponent>() shouldBe null
        driver.matches(bear) shouldBe false
    }

    test("a card milled from library into the graveyard does NOT match") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Put a fresh card into the library, then move it directly library → graveyard
        // (the milling path) and confirm the predicate is false.
        val card = driver.putCardInHand(player, "Grizzly Bears")
        val moveToLibrary = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = card,
            destinationZone = Zone.LIBRARY
        )
        driver.replaceState(moveToLibrary.state)
        val moveToGraveyard = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state = driver.state,
            entityId = card,
            destinationZone = Zone.GRAVEYARD
        )
        driver.replaceState(moveToGraveyard.state)

        driver.matches(card) shouldBe false
    }

    test("a marker from a previous turn does not match on a later turn") {
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

        driver.matches(victim) shouldBe true

        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent
        // New turn → BeginningPhaseManager wiped the marker during the opponent's
        // untap step, so the predicate now evaluates false.
        driver.matches(victim) shouldBe false
    }
})
