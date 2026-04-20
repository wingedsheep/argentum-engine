package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.Moonshadow
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Moonshadow {B} — Elemental 7/7.
 *   Menace
 *   This creature enters with six -1/-1 counters on it.
 *   Whenever one or more permanent cards are put into your graveyard from anywhere
 *   while this creature has a -1/-1 counter on it, remove a -1/-1 counter from this creature.
 *
 * Exercises the new batching trigger event CardsPutIntoYourGraveyardEvent,
 * its IsPermanent filter, and the SourceHasCounter intervening-if condition.
 */
class MoonshadowTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Moonshadow))
        return driver
    }

    fun addMinusOneMinusOne(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.MINUS_ONE_MINUS_ONE, count))
        }
        driver.replaceState(newState)
    }

    fun getMinusOneMinusOne(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    test("creature dying to your graveyard removes a -1/-1 counter from Moonshadow") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val moon = driver.putCreatureOnBattlefield(activePlayer, "Moonshadow")
        addMinusOneMinusOne(driver, moon, 6)
        getMinusOneMinusOne(driver, moon) shouldBe 6

        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")

        driver.passPriority(activePlayer)
        driver.castSpellWithTargets(
            opponent, bolt, listOf(ChosenTarget.Permanent(fodder))
        ).error shouldBe null

        var safety = 0
        while (driver.stackSize > 0 && !driver.isPaused && safety < 20) {
            driver.bothPass()
            safety++
        }

        getMinusOneMinusOne(driver, moon) shouldBe 5
    }

    test("two creatures dying at once only remove one -1/-1 counter (batching)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!

        val moon = driver.putCreatureOnBattlefield(activePlayer, "Moonshadow")
        addMinusOneMinusOne(driver, moon, 6)

        val a = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")
        val b = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Send both creatures directly to graveyard in the same batch.
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        val battlefieldZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)

        // Build two simultaneous zone change events and detect triggers.
        val events = listOf(
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = a,
                entityName = "Savannah Lions",
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = activePlayer
            ),
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = b,
                entityName = "Grizzly Bears",
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = activePlayer
            ),
        )

        // Move them atomically to the graveyard.
        var state = driver.state
        state = state.removeFromZone(battlefieldZone, a).removeFromZone(battlefieldZone, b)
        state = state.addToZone(graveyardZone, a).addToZone(graveyardZone, b)
        driver.replaceState(state)

        val detector = com.wingedsheep.engine.event.TriggerDetector(driver.cardRegistry)
        val triggers = detector.detectTriggers(driver.state, events)

        // Only one Moonshadow trigger should fire despite two permanents hitting the graveyard.
        triggers.count { it.sourceId == moon } shouldBe 1
    }

    test("Moonshadow with no -1/-1 counters does not trigger (intervening-if)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!

        val moon = driver.putCreatureOnBattlefield(activePlayer, "Moonshadow")
        // No counters on Moonshadow — it should be a plain 7/7.
        getMinusOneMinusOne(driver, moon) shouldBe 0

        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        val events = listOf(
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = fodder,
                entityName = "Savannah Lions",
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = activePlayer
            )
        )

        val battlefieldZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        driver.replaceState(
            driver.state.removeFromZone(battlefieldZone, fodder).addToZone(graveyardZone, fodder)
        )

        val detector = com.wingedsheep.engine.event.TriggerDetector(driver.cardRegistry)
        val triggers = detector.detectTriggers(driver.state, events)

        triggers.none { it.sourceId == moon } shouldBe true
    }

    test("instant going to your graveyard does not trigger Moonshadow (non-permanent)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!

        val moon = driver.putCreatureOnBattlefield(activePlayer, "Moonshadow")
        addMinusOneMinusOne(driver, moon, 6)

        // Put an instant card directly into the graveyard and simulate the zone change.
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val handZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.HAND)
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        driver.replaceState(
            driver.state.removeFromZone(handZone, bolt).addToZone(graveyardZone, bolt)
        )

        val events = listOf(
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = bolt,
                entityName = "Lightning Bolt",
                fromZone = com.wingedsheep.sdk.core.Zone.HAND,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = activePlayer
            )
        )

        val detector = com.wingedsheep.engine.event.TriggerDetector(driver.cardRegistry)
        val triggers = detector.detectTriggers(driver.state, events)

        triggers.none { it.sourceId == moon } shouldBe true
        getMinusOneMinusOne(driver, moon) shouldBe 6
    }

    test("opponent's creature dying to their graveyard does not trigger your Moonshadow") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        val moon = driver.putCreatureOnBattlefield(activePlayer, "Moonshadow")
        addMinusOneMinusOne(driver, moon, 6)

        val opposingCreature = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val battlefieldZone = com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
        val graveyardZone = com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.GRAVEYARD)
        driver.replaceState(
            driver.state.removeFromZone(battlefieldZone, opposingCreature).addToZone(graveyardZone, opposingCreature)
        )

        val events = listOf(
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = opposingCreature,
                entityName = "Savannah Lions",
                fromZone = com.wingedsheep.sdk.core.Zone.BATTLEFIELD,
                toZone = com.wingedsheep.sdk.core.Zone.GRAVEYARD,
                ownerId = opponent
            )
        )

        val detector = com.wingedsheep.engine.event.TriggerDetector(driver.cardRegistry)
        val triggers = detector.detectTriggers(driver.state, events)

        triggers.none { it.sourceId == moon } shouldBe true
    }
})
