package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.InfernalCaptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Infernal Captor — exploit; when it exploits a creature, gain control of target artifact or
 * creature until end of turn, untap it, and it gains haste until end of turn.
 */
class InfernalCaptorScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(InfernalCaptor))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("exploiting steals, untaps, and hastes the target until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.tapPermanent(theirBears)

        val captor = driver.putCardInHand(me, "Infernal Captor")
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, captor).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the exploit trigger

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.submitTargetSelection(me, listOf(theirBears))
        driver.bothPass() // resolve the reflexive payoff

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.state.projectedState.getController(theirBears) shouldBe me
        driver.isTapped(theirBears) shouldBe false
        driver.state.projectedState.hasKeyword(theirBears, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getController(theirBears) shouldBe opp
        driver.state.projectedState.hasKeyword(theirBears, Keyword.HASTE) shouldBe false
    }

    test("declining the sacrifice steals nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val captor = driver.putCardInHand(me, "Infernal Captor")
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, captor).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.state.projectedState.getController(theirBears) shouldBe opp
    }
})
