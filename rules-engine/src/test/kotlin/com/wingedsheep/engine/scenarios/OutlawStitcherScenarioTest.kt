package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.OutlawStitcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Outlaw Stitcher — {3}{U} 1/4 Creature — Human Warlock
 *
 * "When this creature enters, create a 2/2 blue and black Zombie Rogue creature token, then put two
 * +1/+1 counters on that token for each spell you've cast this turn other than the first."
 *
 * Verifies the ETB pipeline: a fresh Zombie Rogue token is created and receives
 * `2 * max(spellsCastThisTurn - 1, 0)` +1/+1 counters, addressing the just-created token via the
 * CREATED_TOKENS pipeline collection.
 */
class OutlawStitcherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(OutlawStitcher)
        return driver
    }

    fun tokenCounters(driver: GameTestDriver, me: EntityId): Int {
        val token = driver.getCreatures(me).single { driver.getCardName(it) == "Zombie Rogue Token" }
        return driver.state.getEntity(token)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    test("as the only spell this turn, the token gets no counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val stitcher = driver.putCardInHand(me, "Outlaw Stitcher")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, stitcher)
        driver.bothPass() // resolve Outlaw Stitcher -> ETB trigger on stack
        driver.bothPass() // resolve ETB trigger

        // First (and only) spell -> "other than the first" = 0 -> 0 counters, a vanilla 2/2.
        tokenCounters(driver, me) shouldBe 0
        val token = driver.getCreatures(me).single { driver.getCardName(it) == "Zombie Rogue Token" }
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
    }

    test("with two prior spells, the token gets four +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Cast two Lightning Bolts this turn before the Stitcher.
        repeat(2) {
            val bolt = driver.putCardInHand(me, "Lightning Bolt")
            driver.giveMana(me, Color.RED, 1)
            driver.castSpell(me, bolt, targets = listOf(opp))
            driver.bothPass()
        }

        // Outlaw Stitcher is the third spell: other-than-first = 2 -> 2*2 = 4 counters.
        val stitcher = driver.putCardInHand(me, "Outlaw Stitcher")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, stitcher)
        driver.bothPass() // resolve Outlaw Stitcher -> ETB trigger
        driver.bothPass() // resolve ETB trigger

        tokenCounters(driver, me) shouldBe 4
        val token = driver.getCreatures(me).single { driver.getCardName(it) == "Zombie Rogue Token" }
        driver.state.projectedState.getPower(token) shouldBe 6
        driver.state.projectedState.getToughness(token) shouldBe 6
    }
})
