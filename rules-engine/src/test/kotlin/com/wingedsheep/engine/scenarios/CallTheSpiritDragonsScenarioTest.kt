package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.CallTheSpiritDragons
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Call the Spirit Dragons:
 * {W}{U}{B}{R}{G} Enchantment
 *
 * Dragons you control have indestructible.
 * At the beginning of your upkeep, for each color, put a +1/+1 counter on a Dragon you control
 * of that color. If you put +1/+1 counters on five Dragons this way, you win the game.
 */
class CallTheSpiritDragonsScenarioTest : FunSpec({

    fun dragon(name: String, cost: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = setOf(Subtype.DRAGON),
        power = 4,
        toughness = 4
    )

    val WhiteDragon = dragon("Test White Dragon", "{4}{W}")
    val BlueDragon = dragon("Test Blue Dragon", "{4}{U}")
    val BlackDragon = dragon("Test Black Dragon", "{4}{B}")
    val RedDragon = dragon("Test Red Dragon", "{4}{R}")
    val GreenDragon = dragon("Test Green Dragon", "{4}{G}")
    val FiveColorDragon = dragon("Test Wedge Dragon", "{W}{U}{B}{R}{G}")
    val PlainBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                CallTheSpiritDragons, WhiteDragon, BlueDragon, BlackDragon,
                RedDragon, GreenDragon, FiveColorDragon, PlainBear
            )
        )
        return driver
    }

    fun advanceToControllerUpkeep(driver: GameTestDriver, controller: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != controller) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe controller
    }

    fun counters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Dragons you control have indestructible; other creatures and opponents' Dragons do not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Call the Spirit Dragons")
        val myDragon = driver.putCreatureOnBattlefield(controller, "Test Red Dragon")
        val myBear = driver.putCreatureOnBattlefield(controller, "Test Bear")
        val theirDragon = driver.putCreatureOnBattlefield(opponent, "Test Blue Dragon")

        val projected = StateProjector().project(driver.state)
        projected.hasKeyword(myDragon, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(myBear, Keyword.INDESTRUCTIBLE) shouldBe false
        projected.hasKeyword(theirDragon, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("five different-colored Dragons each get a counter and you win the game") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Call the Spirit Dragons")
        val w = driver.putCreatureOnBattlefield(controller, "Test White Dragon")
        val u = driver.putCreatureOnBattlefield(controller, "Test Blue Dragon")
        val b = driver.putCreatureOnBattlefield(controller, "Test Black Dragon")
        val r = driver.putCreatureOnBattlefield(controller, "Test Red Dragon")
        val g = driver.putCreatureOnBattlefield(controller, "Test Green Dragon")

        advanceToControllerUpkeep(driver, controller)
        driver.stackSize shouldBe 1
        // Each color has exactly one legal Dragon, so every pick auto-resolves (no decision).
        driver.bothPass()

        // One +1/+1 counter on each of the five Dragons.
        counters(driver, w) shouldBe 1
        counters(driver, u) shouldBe 1
        counters(driver, b) shouldBe 1
        counters(driver, r) shouldBe 1
        counters(driver, g) shouldBe 1

        // Five different Dragons received counters → controller wins.
        driver.assertGameOver(expectedWinner = controller)
    }

    test("a single five-color Dragon gets five counters but does NOT win the game") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Call the Spirit Dragons")
        val wedge = driver.putCreatureOnBattlefield(controller, "Test Wedge Dragon")

        advanceToControllerUpkeep(driver, controller)
        driver.stackSize shouldBe 1
        // The five-color Dragon is the only legal pick for every color, so all picks auto-resolve.
        driver.bothPass()

        // It received one counter per color = five counters, but it is only one Dragon.
        counters(driver, wedge) shouldBe 5
        driver.state.gameOver shouldBe false
    }

    test("only four colors covered: counters placed, but you do not win") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val controller = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(controller, "Call the Spirit Dragons")
        val w = driver.putCreatureOnBattlefield(controller, "Test White Dragon")
        val u = driver.putCreatureOnBattlefield(controller, "Test Blue Dragon")
        val b = driver.putCreatureOnBattlefield(controller, "Test Black Dragon")
        val r = driver.putCreatureOnBattlefield(controller, "Test Red Dragon")
        // No green Dragon.

        advanceToControllerUpkeep(driver, controller)
        driver.stackSize shouldBe 1
        driver.bothPass()

        counters(driver, w) shouldBe 1
        counters(driver, u) shouldBe 1
        counters(driver, b) shouldBe 1
        counters(driver, r) shouldBe 1
        // Only four Dragons received counters → no win.
        driver.state.gameOver shouldBe false
    }
})
