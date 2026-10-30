package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.GarrisonGriffin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Garrison Griffin (ELD #305) — "Flying. Whenever this creature attacks, target Knight you control
 * gains flying until end of turn."
 */
class GarrisonGriffinScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    test("attacking gives a Knight you control flying until end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GarrisonGriffin, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val griffin = driver.putCreatureOnBattlefield(me, "Garrison Griffin")
        driver.removeSummoningSickness(griffin)
        val knight = driver.putCreatureOnBattlefield(me, "Test Knight")
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.state.projectedState.hasKeyword(knight, Keyword.FLYING) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(griffin), foe).error shouldBe null
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(knight))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.state.projectedState.hasKeyword(knight, Keyword.FLYING) shouldBe true
    }
})
