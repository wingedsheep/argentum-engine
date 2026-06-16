package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ShackleSlinger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shackle Slinger (OTJ #65) — {2}{U} Creature — Human Soldier 3/2.
 *
 *   "Whenever you cast your second spell each turn, choose target creature an opponent controls.
 *    If it's tapped, put a stun counter on it. Otherwise, tap it."
 *
 * Verifies: no trigger on the first spell; on the second spell, an untapped opposing creature is
 * tapped (no stun counter); a tapped opposing creature gets a stun counter (stays tapped).
 */
class ShackleSlingerScenarioTest : FunSpec({

    fun GameTestDriver.stunCount(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShackleSlinger))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Lightning Bolt" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("second spell taps an untapped opposing creature; no stun counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Shackle Slinger")
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        // First spell: no trigger.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass()
        driver.isTapped(bears) shouldBe false

        // Second spell: trigger fires, targets the untapped Bears.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        // Trigger asks for its target.
        (driver.pendingDecision as ChooseTargetsDecision)
        driver.submitTargetSelection(me, listOf(bears))
        driver.bothPass()

        // Untapped target -> tapped, no stun counter.
        driver.isTapped(bears) shouldBe true
        driver.stunCount(bears) shouldBe 0
    }

    test("second spell puts a stun counter on a tapped opposing creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Shackle Slinger")
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.tapPermanent(bears)

        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass()

        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        (driver.pendingDecision as ChooseTargetsDecision)
        driver.submitTargetSelection(me, listOf(bears))
        driver.bothPass()

        // Already tapped -> stun counter; still tapped.
        driver.stunCount(bears) shouldBe 1
        driver.isTapped(bears) shouldBe true
    }
})
