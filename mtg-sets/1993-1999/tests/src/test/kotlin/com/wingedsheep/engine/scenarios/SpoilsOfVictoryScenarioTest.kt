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

class SpoilsOfVictoryScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(SpoilsOfVictory)
    }

    fun settle(driver: GameTestDriver, yes: Boolean = true) {
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val d = driver.pendingDecision) {
                null -> driver.bothPass()
                is YesNoDecision -> driver.submitYesNo(d.playerId, yes)
                is SelectCardsDecision -> driver.submitCardSelection(d.playerId, d.options.take(d.minSelections.coerceAtLeast(1)))
                else -> error("unexpected decision $d")
            }
        }
    }

    test("fetches any basic-typed land straight onto the battlefield, untapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val spell = driver.putCardInHand(me, "Spoils of Victory")
        driver.giveMana(me, Color.GREEN, 3)
        val landsBefore = driver.state.getBattlefield().count { driver.getCardName(it) == "Swamp" }
        driver.castSpell(me, spell).outcome shouldBe Outcome.Done
        driver.bothPass()
        val d = driver.pendingDecision as SelectCardsDecision
        val swamp = d.options.first { driver.getCardName(it) == "Swamp" }
        driver.submitCardSelection(me, listOf(swamp))
        settle(driver)
        driver.state.getBattlefield().count { driver.getCardName(it) == "Swamp" } shouldBe landsBefore + 1
        driver.state.getEntity(swamp)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe false
    }
})
