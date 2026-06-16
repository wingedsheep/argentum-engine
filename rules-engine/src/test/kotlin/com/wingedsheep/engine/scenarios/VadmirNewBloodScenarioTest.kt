package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.VadmirNewBlood
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vadmir, New Blood — {1}{B} 2/2 Legendary Creature — Vampire Rogue
 *
 * "Whenever you commit a crime, put a +1/+1 counter on Vadmir. This ability triggers only once each turn."
 * "As long as Vadmir has four or more +1/+1 counters on it, it has menace and lifelink."
 *
 * Verifies: the crime trigger adds a counter once per turn; menace+lifelink turn on only at 4+ counters
 * and turn off again below the threshold (static ability re-evaluated from projected state).
 */
class VadmirNewBloodScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VadmirNewBlood)
        return driver
    }

    fun setCounters(driver: GameTestDriver, entityId: EntityId, count: Int) {
        val newState = driver.state.updateEntity(entityId) { container ->
            container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
        }
        driver.replaceState(newState)
    }

    test("committing a crime adds a +1/+1 counter, but only once per turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val vadmir = driver.putCreatureOnBattlefield(me, "Vadmir, New Blood")

        driver.state.projectedState.getPower(vadmir) shouldBe 2

        // First crime this turn -> one counter.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> crime -> counter trigger on stack
        driver.bothPass() // resolve counter trigger
        driver.state.projectedState.getPower(vadmir) shouldBe 3

        // Second crime same turn -> once-per-turn gate, no extra counter.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()
        driver.state.projectedState.getPower(vadmir) shouldBe 3
    }

    test("menace and lifelink switch on at 4 counters and off below it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val vadmir = driver.putCreatureOnBattlefield(me, "Vadmir, New Blood")

        // Three counters: still below threshold, no menace/lifelink.
        setCounters(driver, vadmir, 3)
        var projected = projector.project(driver.state)
        projected.hasKeyword(vadmir, Keyword.MENACE) shouldBe false
        projected.hasKeyword(vadmir, Keyword.LIFELINK) shouldBe false

        // Fourth counter: menace + lifelink turn on.
        setCounters(driver, vadmir, 4)
        projected = projector.project(driver.state)
        projected.hasKeyword(vadmir, Keyword.MENACE) shouldBe true
        projected.hasKeyword(vadmir, Keyword.LIFELINK) shouldBe true

        // Drop back to 3: keywords turn off again.
        setCounters(driver, vadmir, 3)
        projected = projector.project(driver.state)
        projected.hasKeyword(vadmir, Keyword.MENACE) shouldBe false
        projected.hasKeyword(vadmir, Keyword.LIFELINK) shouldBe false
    }
})
