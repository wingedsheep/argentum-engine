package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BarTheDoorScenarioTest : FunSpec({

    test("creatures you control get +0/+4; an opponent's creature is unaffected") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val mine = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(you, "Bar the Door")
        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(mine) shouldBe 2
        projected.getToughness(mine) shouldBe 6
        projected.getToughness(theirs) shouldBe 2
    }
})
