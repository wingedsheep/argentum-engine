package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.UnscrupulousContractor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unscrupulous Contractor ({2}{B}, 3/2 Human Assassin):
 * "When this creature enters, you may sacrifice a creature. When you do, target player
 *  draws two cards and loses 2 life."
 *
 * Composed as a [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect]: the
 * "sacrifice a creature" action is a resolution-time choice (SelectTargetEffect +
 * SacrificeTarget), and the "When you do" reflexive payoff targets a player chosen as the
 * reflexive ability goes on the stack, then makes that player draw two and lose 2 life.
 */
class UnscrupulousContractorTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + UnscrupulousContractor)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castContractor(driver: GameTestDriver, me: EntityId): EntityId {
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2)
        val card = driver.putCardInHand(me, "Unscrupulous Contractor")
        driver.castSpell(me, card).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger off the stack
        return card
    }

    test("sacrificing a creature makes the chosen player draw two and lose two life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castContractor(driver, me)

        // "You may sacrifice a creature." — accept.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        // Choose the fodder to sacrifice.
        driver.submitTargetSelection(me, listOf(fodder))

        val oppHandBefore = driver.getHand(opp).size
        // The reflexive trigger goes on the stack; choose the target player (the opponent).
        driver.submitTargetSelection(me, listOf(opp))
        driver.bothPass() // resolve the reflexive draw + lose life

        driver.getGraveyard(me).contains(fodder) shouldBe true
        driver.getHand(opp).size shouldBe oppHandBefore + 2
        driver.getLifeTotal(opp) shouldBe 18
    }

    test("the controller may target themselves to draw and lose life") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castContractor(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))

        val myHandBefore = driver.getHand(me).size
        driver.submitTargetSelection(me, listOf(me))
        driver.bothPass()

        driver.getHand(me).size shouldBe myHandBefore + 2
        driver.getLifeTotal(me) shouldBe 18
    }

    test("declining the optional sacrifice does nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        castContractor(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        // No reflexive payoff: opponent's life unchanged, hand not refilled.
        driver.getLifeTotal(opp) shouldBe 20
        driver.pendingDecision shouldBe null
    }
})
