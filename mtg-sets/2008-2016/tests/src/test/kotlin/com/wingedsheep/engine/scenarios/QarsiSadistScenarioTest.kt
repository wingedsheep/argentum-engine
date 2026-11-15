package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.QarsiSadist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Qarsi Sadist (DTK #113) — {1}{B} 1/3 Human Cleric.
 *
 * "Exploit. When this creature exploits a creature, target opponent loses 2 life and you gain
 * 2 life."
 *
 * Pins the drain when exploiting another creature and when the Sadist exploits itself (the payoff
 * rides the exploit reflexive, so it survives the self-sacrifice).
 */
class QarsiSadistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(QarsiSadist))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castSadist(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Qarsi Sadist")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    // The only legal "target opponent" may be auto-chosen; answer the prompt if it is asked.
    fun GameTestDriver.chooseOpponentIfAsked(me: EntityId, opp: EntityId) {
        if (pendingDecision is ChooseTargetsDecision) submitTargetSelection(me, listOf(opp))
    }

    test("exploiting another creature drains the opponent for 2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castSadist(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.chooseOpponentIfAsked(me, opp)
        driver.bothPass() // resolve the reflexive drain

        driver.getGraveyard(me) shouldContain fodder
        driver.getLifeTotal(opp) shouldBe 18
        driver.getLifeTotal(me) shouldBe 22
    }

    test("exploiting itself still drains the opponent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val sadist = castSadist(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true) // the Sadist is the only creature -> sacrifices itself
        driver.chooseOpponentIfAsked(me, opp)
        driver.bothPass()

        driver.getGraveyard(me) shouldContain sadist
        driver.getLifeTotal(opp) shouldBe 18
        driver.getLifeTotal(me) shouldBe 22
    }
})
