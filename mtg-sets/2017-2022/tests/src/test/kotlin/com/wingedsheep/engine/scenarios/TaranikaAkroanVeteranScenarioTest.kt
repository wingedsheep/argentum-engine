package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.thb.cards.TaranikaAkroanVeteran
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Taranika, Akroan Veteran (THB #39) — {1}{W}{W} Legendary Creature — Human Soldier, 3/3.
 *
 * "Vigilance. Whenever Taranika attacks, untap another target creature you control. Until end of
 *  turn, that creature has base power and toughness 4/4 and gains indestructible."
 */
class TaranikaAkroanVeteranScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TaranikaAkroanVeteran))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("attack trigger untaps another attacker, makes it a 4/4 and gives it indestructible") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val taranika = driver.putCreatureOnBattlefield(me, "Taranika, Akroan Veteran")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(taranika)
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(taranika, bears), opponent)
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(bears))

        // Vigilance: Taranika stays untapped; the Bears tapped to attack.
        driver.isTapped(taranika) shouldBe false
        driver.isTapped(bears) shouldBe true

        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.isTapped(bears) shouldBe false
        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 4
        driver.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
        // Taranika itself is untouched.
        driver.state.projectedState.getPower(taranika) shouldBe 3
        driver.state.projectedState.hasKeyword(taranika, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("the effect ends at end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val taranika = driver.putCreatureOnBattlefield(me, "Taranika, Akroan Veteran")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(taranika)
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(taranika), opponent)
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(bears))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 4

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
        driver.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
