package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.RighteousValkyrie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Righteous Valkyrie (KHM #24) — "Flying. Whenever another Angel or Cleric you control enters, you
 * gain life equal to that creature's toughness. As long as you have at least 7 life more than your
 * starting life total, creatures you control get +2/+2."
 */
class RighteousValkyrieScenarioTest : FunSpec({

    fun newDriver(startingLife: Int = 20): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RighteousValkyrie)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = startingLife)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("another Cleric entering gains life equal to its toughness; a non-Angel/Cleric gains nothing") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Righteous Valkyrie")

        val cleric = driver.putCardInHand(me, "Test Cleric")
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, cleric).error shouldBe null
        driver.bothPass() // resolve the Cleric
        driver.bothPass() // resolve the lifegain trigger
        withClue("Test Cleric is a 2/2, so the Valkyrie gains 2") {
            driver.getLifeTotal(me) shouldBe 22
        }

        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lions).error shouldBe null
        driver.bothPass() // resolve the Lions — a Cat, so no trigger
        withClue("a non-Angel, non-Cleric creature entering gains nothing") {
            driver.getLifeTotal(me) shouldBe 22
        }
    }

    test("another Angel entering triggers too, but the Valkyrie entering does not trigger itself") {
        val driver = newDriver()
        val me = driver.activePlayer!!

        val first = driver.putCardInHand(me, "Righteous Valkyrie")
        driver.giveMana(me, Color.WHITE, 3)
        driver.castSpell(me, first).error shouldBe null
        driver.bothPass() // resolve the first Valkyrie
        withClue("it says *another* Angel or Cleric") {
            driver.getLifeTotal(me) shouldBe 20
        }

        val second = driver.putCardInHand(me, "Righteous Valkyrie")
        driver.giveMana(me, Color.WHITE, 3)
        driver.castSpell(me, second).error shouldBe null
        driver.bothPass() // resolve the second Valkyrie
        driver.bothPass() // resolve the first Valkyrie's trigger
        withClue("the second Valkyrie is a 2/4 Angel, so the first gains 4") {
            driver.getLifeTotal(me) shouldBe 24
        }
    }

    test("the anthem turns on at starting life + 7 and covers only your creatures") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val valkyrie = driver.putCreatureOnBattlefield(me, "Righteous Valkyrie")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val theirLions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.setLifeTotal(me, 26)
        withClue("26 is only 6 above the starting 20") {
            driver.state.projectedState.getPower(valkyrie) shouldBe 2
            driver.state.projectedState.getToughness(valkyrie) shouldBe 4
            driver.state.projectedState.getPower(lions) shouldBe 1
            driver.state.projectedState.getToughness(lions) shouldBe 1
        }

        driver.setLifeTotal(me, 27)
        withClue("27 is exactly 7 above — every creature you control, the Valkyrie included, gets +2/+2") {
            driver.state.projectedState.getPower(valkyrie) shouldBe 4
            driver.state.projectedState.getToughness(valkyrie) shouldBe 6
            driver.state.projectedState.getPower(lions) shouldBe 3
            driver.state.projectedState.getToughness(lions) shouldBe 3
        }
        withClue("the opponent's creatures are untouched") {
            driver.state.projectedState.getPower(theirLions) shouldBe 1
            driver.state.projectedState.getToughness(theirLions) shouldBe 1
        }

        driver.setLifeTotal(opponent, 40)
        driver.setLifeTotal(me, 20)
        withClue("dropping back below the threshold turns it off; the opponent's life is irrelevant") {
            driver.state.projectedState.getPower(lions) shouldBe 1
        }
    }

    test("the threshold is measured from the real starting life total, not a hardcoded 20") {
        val driver = newDriver(startingLife = 30)
        val me = driver.activePlayer!!
        val valkyrie = driver.putCreatureOnBattlefield(me, "Righteous Valkyrie")

        driver.setLifeTotal(me, 36)
        driver.state.projectedState.getPower(valkyrie) shouldBe 2

        driver.setLifeTotal(me, 37)
        driver.state.projectedState.getPower(valkyrie) shouldBe 4
        driver.state.projectedState.getToughness(valkyrie) shouldBe 6
    }
})
