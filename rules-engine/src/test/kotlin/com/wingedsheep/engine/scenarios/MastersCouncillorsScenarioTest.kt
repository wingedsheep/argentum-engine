package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.MastersCouncillors
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Master's Councillors {1}{U} — Creature — Human Advisor 1/3
 *   Vigilance
 *   This creature gets +2/+0 for each graveyard with seven or more cards in it.
 *   Whenever you draw your second card each turn, target player mills three cards.
 *
 * The pump is a [com.wingedsheep.sdk.scripting.values.DynamicAmount.CountPlayersWith] read from
 * inside a continuous effect — the count has to be recomputed through the layer projection as
 * graveyards fill, and it has to include the controller's own graveyard, not just opponents'.
 */
class MastersCouncillorsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MastersCouncillors))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with no full graveyard it is a printed 1/3") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val councillors = driver.putCreatureOnBattlefield(you, "Master's Councillors")

        driver.state.projectedState.getPower(councillors) shouldBe 1
        driver.state.projectedState.getToughness(councillors) shouldBe 3
    }

    test("your own graveyard counts: seven cards makes it a 3/3") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val councillors = driver.putCreatureOnBattlefield(you, "Master's Councillors")

        repeat(6) { driver.putCardInGraveyard(you, "Island") }
        driver.state.projectedState.getPower(councillors) shouldBe 1 // six is not enough

        driver.putCardInGraveyard(you, "Island")
        driver.state.projectedState.getPower(councillors) shouldBe 3
        driver.state.projectedState.getToughness(councillors) shouldBe 3
    }

    test("both graveyards over seven makes it a 5/3") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val councillors = driver.putCreatureOnBattlefield(you, "Master's Councillors")

        repeat(7) { driver.putCardInGraveyard(you, "Island") }
        repeat(9) { driver.putCardInGraveyard(opponent, "Island") }

        driver.state.projectedState.getPower(councillors) shouldBe 5
        driver.state.projectedState.getToughness(councillors) shouldBe 3
    }
})
