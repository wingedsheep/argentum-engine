package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.*
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RidingTheDiluHorseScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(RidingTheDiluHorse)
        registerCard(ShuDefender)
        registerCard(ShuCavalry)
    }

    test("the bonus and horsemanship last past end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val guy = driver.putCreatureOnBattlefield(me, "Shu Defender")
        val base = driver.state.projectedState.getPower(guy)!!
        val spell = driver.putCardInHand(me, "Riding the Dilu Horse")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, spell, listOf(guy)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.getPower(guy)!! shouldBe (base + 2)
        driver.state.projectedState.hasKeyword(guy, "HORSEMANSHIP") shouldBe true

        // through the opponent's turn and back: nothing wore off
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.projectedState.getPower(guy)!! shouldBe (base + 2)
        driver.state.projectedState.hasKeyword(guy, "HORSEMANSHIP") shouldBe true
    }
})
