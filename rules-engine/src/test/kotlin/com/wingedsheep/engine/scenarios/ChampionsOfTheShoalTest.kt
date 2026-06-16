package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for Champions of the Shoal (ECL #46).
 *
 * Champions of the Shoal {3}{U}
 * Creature — Merfolk Soldier 4/6
 * Whenever this creature enters or becomes tapped, tap up to one target
 * creature and put a stun counter on it.
 *
 * "Becomes tapped" only fires on an untapped -> tapped transition (CR 603.2f):
 * tapping an already-tapped permanent does nothing (CR 701.21a) and must not
 * re-fire the trigger. This regression test pins both directions.
 */
class ChampionsOfTheShoalTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun stunCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    test("becomes-tapped trigger fires when Champions of the Shoal is tapped while untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val champions = driver.putCreatureOnBattlefield(active, "Champions of the Shoal")
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Tap Champions with a real tap effect: an untapped -> tapped transition.
        val chill = driver.putCardInHand(active, "Crippling Chill")
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 2)
        driver.castSpell(active, chill, targets = listOf(champions))
        driver.bothPass() // resolve Crippling Chill -> taps Champions -> queues becomes-tapped trigger

        driver.isTapped(champions) shouldBe true

        // The becomes-tapped trigger is on the stack; choose its "up to one target creature".
        val chooseTargets = driver.pendingDecision
        (chooseTargets is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(active, listOf(victim))
        driver.bothPass() // resolve the trigger

        driver.isTapped(victim) shouldBe true
        stunCounters(driver, victim) shouldBe 1
    }

    test("becomes-tapped trigger does NOT fire when Champions of the Shoal is already tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val champions = driver.putCreatureOnBattlefield(active, "Champions of the Shoal")
        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Put Champions into a tapped state directly (no TappedEvent, so no trigger yet).
        driver.tapPermanent(champions)
        driver.isTapped(champions) shouldBe true

        // Tap the already-tapped Champions again with a real tap effect.
        val chill = driver.putCardInHand(active, "Crippling Chill")
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 2)
        driver.castSpell(active, chill, targets = listOf(champions))
        driver.bothPass() // resolve Crippling Chill

        // No untapped -> tapped transition => no becomes-tapped trigger => no target decision,
        // and the victim is untouched.
        driver.pendingDecision.shouldBeNull()
        driver.isTapped(victim) shouldBe false
        stunCounters(driver, victim) shouldBe 0
    }
})
