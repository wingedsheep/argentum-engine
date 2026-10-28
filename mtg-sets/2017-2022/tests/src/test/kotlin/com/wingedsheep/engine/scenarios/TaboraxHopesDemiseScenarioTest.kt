package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.TaboraxHopesDemise
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Taborax, Hope's Demise (ZNR) — {2}{B} Legendary Creature — Demon Cleric 2/2, flying.
 *
 * The counter always lands; the draw-and-lose-1 is a resolution-time rider gated on the dying
 * creature's last-known type line being a Cleric, and is optional. Lifelink switches on at five
 * +1/+1 counters. Deaths are staged with Lightning Bolt so the dies trigger actually fires.
 */
class TaboraxHopesDemiseScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TaboraxHopesDemise))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Bolt [creature] and pass until the stack is empty or a decision is pending. */
    fun bolt(driver: GameTestDriver, caster: EntityId, creature: EntityId) {
        driver.giveMana(caster, Color.RED, 1)
        val boltCard = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, boltCard, targets = listOf(creature)).error shouldBe null
        var safety = 0
        while (driver.stackSize > 0 && driver.pendingDecision == null && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    test("a Cleric dying gives a counter and an optional draw that costs 1 life") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val taborax = driver.putCreatureOnBattlefield(you, "Taborax, Hope's Demise")
        val cleric = driver.putCreatureOnBattlefield(you, "Test Cleric")
        val handBefore = driver.getHandSize(you)

        bolt(driver, you, cleric)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(you, true)

        plusOneCounters(driver, taborax) shouldBe 1
        driver.getHandSize(you) shouldBe handBefore + 1
        driver.getLifeTotal(you) shouldBe 19
    }

    test("declining the draw keeps the counter and costs no life") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val taborax = driver.putCreatureOnBattlefield(you, "Taborax, Hope's Demise")
        val cleric = driver.putCreatureOnBattlefield(you, "Test Cleric")
        val handBefore = driver.getHandSize(you)

        bolt(driver, you, cleric)
        driver.submitYesNo(you, false)

        plusOneCounters(driver, taborax) shouldBe 1
        driver.getHandSize(you) shouldBe handBefore
        driver.getLifeTotal(you) shouldBe 20
    }

    test("a non-Cleric dying gives only the counter, with no draw prompt") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val taborax = driver.putCreatureOnBattlefield(you, "Taborax, Hope's Demise")
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val handBefore = driver.getHandSize(you)

        bolt(driver, you, courser)

        driver.pendingDecision shouldBe null
        plusOneCounters(driver, taborax) shouldBe 1
        driver.getHandSize(you) shouldBe handBefore
        driver.getLifeTotal(you) shouldBe 20
    }

    test("an opponent's creature dying does not trigger") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val taborax = driver.putCreatureOnBattlefield(you, "Taborax, Hope's Demise")
        val theirCleric = driver.putCreatureOnBattlefield(opponent, "Test Cleric")

        bolt(driver, you, theirCleric)

        driver.pendingDecision shouldBe null
        plusOneCounters(driver, taborax) shouldBe 0
    }

    test("has lifelink only with five or more +1/+1 counters") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val taborax = driver.putCreatureOnBattlefield(you, "Taborax, Hope's Demise")

        driver.addComponent(taborax, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
        projector.project(driver.state).hasKeyword(taborax, Keyword.LIFELINK) shouldBe false

        driver.addComponent(taborax, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 5)))
        projector.project(driver.state).hasKeyword(taborax, Keyword.LIFELINK) shouldBe true
    }
})
