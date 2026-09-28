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

class ZodiacDragonScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(ZodiacDragon)
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

    fun kill(yes: Boolean): Pair<Boolean, Boolean> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val dragon = driver.putCreatureOnBattlefield(me, "Zodiac Dragon")
        val kill = driver.putCardInHand(me, "Doom Blade")
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, kill, listOf(dragon)).outcome shouldBe Outcome.Done
        driver.bothPass()
        settle(driver, yes)
        return (dragon in driver.getHand(me)) to (dragon in driver.getGraveyard(me))
    }

    test("returns to hand when it dies and the owner accepts") {
        kill(true) shouldBe (true to false)
    }

    test("stays in the graveyard when declined") {
        kill(false) shouldBe (false to true)
    }
})
