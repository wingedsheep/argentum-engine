package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SilumgarSorcerer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silumgar Sorcerer (DTK #76) — {1}{U}{U} 2/1 Flash, Flying Human Wizard Sorcerer.
 *
 * "Exploit. When this creature exploits a creature, counter target creature spell."
 *
 * The opponent casts a creature on their own turn and the Sorcerer is flashed in on top of it: the
 * exploit trigger and its reflexive counter both resolve above the creature spell. Pins that
 * exploiting counters the creature spell and that declining lets it resolve.
 */
class SilumgarSorcererScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilumgarSorcerer))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** The active player casts Grizzly Bears and releases priority; returns the spell on the stack. */
    fun opponentCastsBears(driver: GameTestDriver, opp: EntityId): EntityId {
        val bears = driver.putCardInHand(opp, "Grizzly Bears")
        driver.giveMana(opp, Color.GREEN, 1)
        driver.giveColorlessMana(opp, 1)
        driver.castSpell(opp, bears).outcome shouldBe Outcome.Done
        val spell = driver.getTopOfStack().shouldNotBeNull()
        driver.passPriority(opp)
        return spell
    }

    /** Flash the Sorcerer in response and resolve it and its exploit trigger. */
    fun flashSorcerer(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Silumgar Sorcerer")
        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the Sorcerer -> exploit trigger goes on the stack above the Bears
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting a creature counters the creature spell") {
        val driver = createDriver()
        val opp = driver.activePlayer!!
        val me = driver.getOpponent(opp)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bearsSpell = opponentCastsBears(driver, opp)
        val sorcerer = flashSorcerer(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        // The Bears spell is the only creature spell left on the stack; answer if the prompt is asked.
        if (driver.pendingDecision is ChooseTargetsDecision) driver.submitTargetSelection(me, listOf(bearsSpell))
        driver.bothPass() // resolve the reflexive counter

        driver.getGraveyard(me) shouldContain fodder
        driver.getGraveyard(opp) shouldContain bearsSpell
        driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        driver.getPermanents(me) shouldContain sorcerer
    }

    test("declining the exploit sacrifice lets the creature spell resolve") {
        val driver = createDriver()
        val opp = driver.activePlayer!!
        val me = driver.getOpponent(opp)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        opponentCastsBears(driver, opp)
        flashSorcerer(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.bothPass() // resolve the Bears

        driver.getPermanents(me) shouldContain fodder
        driver.findPermanent(opp, "Grizzly Bears").shouldNotBeNull()
    }
})
