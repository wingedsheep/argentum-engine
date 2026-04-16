package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.HeirloomAuntie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Heirloom Auntie {2}{B} — Goblin Warlock 4/4.
 *   This creature enters with two -1/-1 counters on it.
 *   Whenever another creature you control dies, surveil 1, then remove a -1/-1
 *   counter from this creature.
 *
 * Regression: RemoveCountersExecutor's inline `CounterType.valueOf(...)` mapping
 * does not handle "-1/-1" (produces "M1_M1" which is not an enum name), so it
 * silently fell back to PLUS_ONE_PLUS_ONE and no -1/-1 counter was ever removed.
 * The fix routes every counter executor through a shared `resolveCounterType`
 * helper with explicit mappings for "+1/+1" and "-1/-1".
 */
class HeirloomAuntieRemoveCounterTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HeirloomAuntie))
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

    test("dies trigger surveils 1 and removes a -1/-1 counter from Heirloom Auntie") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Active player's Heirloom Auntie is already on the battlefield with
        // two -1/-1 counters (simulating the ETB replacement having already run).
        val auntie = driver.putCreatureOnBattlefield(activePlayer, "Heirloom Auntie")
        addMinusOneMinusOne(driver, auntie, 2)
        getMinusOneMinusOne(driver, auntie) shouldBe 2

        // A friendly creature the opponent can kill to trigger the ability.
        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Known card on top of active player's library for surveil to see.
        val surveilTarget = driver.putCardOnTopOfLibrary(activePlayer, "Grizzly Bears")

        // Opponent bolts the friendly creature.
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")

        driver.passPriority(activePlayer)
        driver.castSpellWithTargets(
            opponent, bolt, listOf(ChosenTarget.Permanent(fodder))
        ).error shouldBe null

        // Drain priority until the dies trigger pauses for the surveil decision.
        var safety = 0
        while (!driver.isPaused && driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }

        // Surveil 1 asks which of the top 1 cards go to the graveyard.
        driver.isPaused shouldBe true
        val surveilDecision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        surveilDecision.options shouldBe listOf(surveilTarget)

        // Put the card in the graveyard (no reorder decision follows).
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(
                decisionId = surveilDecision.id,
                selectedCards = listOf(surveilTarget)
            )
        )

        // Drain anything left on the stack (no further decisions expected).
        safety = 0
        while (driver.stackSize > 0 && !driver.isPaused && safety < 20) {
            driver.bothPass()
            safety++
        }

        // The -1/-1 counter must actually come off — this is the regression.
        getMinusOneMinusOne(driver, auntie) shouldBe 1
    }

    test("second dies trigger removes the remaining -1/-1 counter, leaving Heirloom Auntie at 4/4") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Start Heirloom Auntie with a single -1/-1 counter.
        val auntie = driver.putCreatureOnBattlefield(activePlayer, "Heirloom Auntie")
        addMinusOneMinusOne(driver, auntie, 1)
        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Empty the surveil lookahead — no decision to handle.
        val libraryZone = com.wingedsheep.engine.state.ZoneKey(activePlayer, com.wingedsheep.sdk.core.Zone.LIBRARY)
        driver.replaceState(
            driver.state.copy(zones = driver.state.zones + (libraryZone to emptyList()))
        )

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

        driver.isPaused shouldBe false
        getMinusOneMinusOne(driver, auntie) shouldBe 0
    }
})
