package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Printed riot (CR 702.136a): "You may have this permanent enter with an additional +1/+1 counter
 * on it. If you don't, it gains haste." Choosing the counter when it can't be placed means the
 * permanent didn't enter with it, so it gains haste (Rhythm of the Wild's ruling: "If a creature
 * entering the battlefield has riot but can't have a +1/+1 counter put onto it, it gains haste").
 */
class PrintedRiotCounterFallbackTest : FunSpec({

    val rioter = card("Test Rioter") {
        manaCost = "{1}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        riot()
    }

    // Cuts every +1/+1 counter placement on your creatures by one — a lone riot counter becomes zero.
    val damper = card("Test Counter Damper") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Enchantment"
        replacementEffect(ModifyCounterPlacement(modifier = -1))
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(rioter, damper))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun castRioterChoosing(driver: GameTestDriver, you: EntityId, needle: String): EntityId {
        driver.giveMana(you, Color.RED, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, driver.putCardInHand(you, "Test Rioter"))
        driver.bothPass()
        val pick = driver.pendingDecision as ChooseOptionDecision
        val idx = pick.options.indexOfFirst { it.contains(needle, ignoreCase = true) }
        driver.submitDecision(you, OptionChosenResponse(pick.id, idx))
        return driver.findPermanent(you, "Test Rioter")!!
    }

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("choosing the counter when it can't be placed gives haste instead") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Test Counter Damper")

        val rioter = castRioterChoosing(driver, you, "counter")

        plusOne(driver, rioter) shouldBe 0
        driver.state.projectedState.hasKeyword(rioter, Keyword.HASTE) shouldBe true
    }

    test("choosing the counter when it can be placed gives the counter and no haste") {
        val (driver, you) = newGame()

        val rioter = castRioterChoosing(driver, you, "counter")

        plusOne(driver, rioter) shouldBe 1
        driver.state.projectedState.hasKeyword(rioter, Keyword.HASTE) shouldBe false
    }
})
