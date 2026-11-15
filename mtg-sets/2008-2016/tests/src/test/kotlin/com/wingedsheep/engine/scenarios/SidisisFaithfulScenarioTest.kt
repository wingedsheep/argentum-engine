package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SidisisFaithful
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sidisi's Faithful (DTK #74) — {U} 0/4 Snake Wizard.
 *
 * "Exploit. When this creature exploits a creature, return target creature to its owner's hand."
 *
 * Pins that exploiting another creature bounces the chosen creature to its *owner's* hand, that the
 * bounce still happens when the Faithful exploits itself, and that declining bounces nothing.
 */
class SidisisFaithfulScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SidisisFaithful))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castFaithful(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Sidisi's Faithful")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting another creature returns target creature to its owner's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        castFaithful(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass() // resolve the reflexive bounce

        driver.getGraveyard(me) shouldContain fodder
        driver.getHand(opp) shouldContain victim
        driver.getPermanents(opp) shouldNotContain victim
    }

    test("exploiting itself still bounces the target") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val faithful = castFaithful(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true) // the Faithful is my only creature -> sacrifices itself
        if (driver.pendingDecision is ChooseTargetsDecision) driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        driver.getGraveyard(me) shouldContain faithful
        driver.getHand(opp) shouldContain victim
    }

    test("declining the exploit sacrifice bounces nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        castFaithful(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getPermanents(opp) shouldContain victim
    }
})
