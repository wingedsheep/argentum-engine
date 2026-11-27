package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NiblisOfTheMistScenarioTest : FunSpec({

    fun castNiblisTargeting(accept: Boolean): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Grizzly Bears")

        val niblis = driver.putCardInHand(you, "Niblis of the Mist")
        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, niblis).outcome shouldBe Outcome.Done

        var guard = 0
        while (guard++ < 20) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(you, listOf(bears))
                is YesNoDecision -> driver.submitYesNo(you, accept)
                else -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
            }
        }
        return driver to bears
    }

    test("when it enters you may tap target creature") {
        val (driver, bears) = castNiblisTargeting(accept = true)
        driver.isTapped(bears) shouldBe true
    }

    test("declining the may leaves the target untapped") {
        val (driver, bears) = castNiblisTargeting(accept = false)
        driver.isTapped(bears) shouldBe false
    }
})
