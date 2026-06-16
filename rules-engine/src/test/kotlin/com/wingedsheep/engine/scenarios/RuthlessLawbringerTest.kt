package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RuthlessLawbringer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ruthless Lawbringer ({1}{W}{B}, 3/2 Vampire Assassin):
 * "When this creature enters, you may sacrifice another creature. When you do,
 *  destroy target nonland permanent."
 *
 * Composed as a [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect]: the
 * "sacrifice another creature" action is a resolution-time choice (SelectTargetEffect +
 * SacrificeTarget), and the "When you do" reflexive destroy targets a nonland permanent,
 * chosen as that second ability goes on the stack.
 */
class RuthlessLawbringerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RuthlessLawbringer)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castLawbringer(driver: GameTestDriver, me: EntityId): EntityId {
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        val card = driver.putCardInHand(me, "Ruthless Lawbringer")
        driver.castSpell(me, card).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger off the stack
        return card
    }

    test("sacrificing another creature destroys a target nonland permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        castLawbringer(driver, me)

        // "You may sacrifice another creature." — accept.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        // Choose the fodder to sacrifice (SelectTargetEffect -> ChooseTargetsDecision).
        driver.submitTargetSelection(me, listOf(fodder))

        // The "When you do" reflexive trigger goes on the stack; pick the destroy target.
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass() // resolve the reflexive destroy

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.getGraveyard(opp).contains(victim) shouldBe true
    }

    test("declining the optional sacrifice destroys nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        castLawbringer(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        // Nothing destroyed: the victim is still on the battlefield.
        driver.getPermanents(opp).contains(victim) shouldBe true
    }

    test("the Lawbringer itself can't be the sacrificed creature (\"another\")") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // No other creatures: only the Lawbringer is on the battlefield, so the optional
        // sacrifice has no legal target and the reflexive payoff must not fire.
        castLawbringer(driver, me)

        // With no "another creature" available, the may-sacrifice action is infeasible and is
        // skipped entirely — no decision is presented.
        driver.pendingDecision shouldBe null
    }
})
