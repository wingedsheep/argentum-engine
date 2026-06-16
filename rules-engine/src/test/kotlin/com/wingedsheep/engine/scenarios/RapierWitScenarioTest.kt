package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.RapierWit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rapier Wit {1}{W} Instant (SOS canonical).
 *
 * Tap target creature. If it's your turn, put a stun counter on it. Draw a card.
 */
class RapierWitScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RapierWit))
        return driver
    }

    fun stunCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    test("on your turn: taps the creature, adds a stun counter, and draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3, untapped

        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.WHITE, 1)
        val spell = driver.putCardInHand(me, "Rapier Wit")
        val handWithSpell = driver.getHandSize(me)
        driver.castSpell(me, spell, targets = listOf(bear)).error shouldBe null
        driver.bothPass()

        driver.isTapped(bear) shouldBe true
        stunCounters(driver, bear) shouldBe 1
        driver.getHandSize(me) shouldBe handWithSpell // -1 spell, +1 draw
    }

    test("on an opponent's turn: taps the creature and draws, but adds no stun counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val starting = driver.activePlayer!!
        val other = driver.getOpponent(starting)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Advance into the opponent's turn (their upkeep is the next UPKEEP step reached).
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe other
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        driver.activePlayer shouldBe other

        // `starting` casts Rapier Wit during `other`'s turn — it is NOT the caster's turn.
        val bear = driver.putCreatureOnBattlefield(other, "Centaur Courser")

        driver.giveMana(starting, Color.WHITE, 1)
        driver.giveMana(starting, Color.WHITE, 1)
        val spell = driver.putCardInHand(starting, "Rapier Wit")
        val handWithSpell = driver.getHandSize(starting)
        // Active player passes priority so the non-active caster receives it.
        driver.passPriority(other)
        driver.castSpell(starting, spell, targets = listOf(bear)).error shouldBe null
        driver.bothPass()

        driver.isTapped(bear) shouldBe true
        stunCounters(driver, bear) shouldBe 0
        driver.getHandSize(starting) shouldBe handWithSpell // -1 spell, +1 draw
    }
})
