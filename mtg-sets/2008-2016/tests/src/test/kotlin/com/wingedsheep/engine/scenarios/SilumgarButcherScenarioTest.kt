package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SilumgarButcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silumgar Butcher (DTK #122) — {4}{B} 3/3 Zombie Djinn.
 *
 * "Exploit. When this creature exploits a creature, target creature gets -3/-3 until end of turn."
 *
 * Pins that exploiting a creature gives the chosen creature -3/-3 (killing a 3/3), and that
 * declining the sacrifice shrinks nothing.
 */
class SilumgarButcherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilumgarButcher))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castButcher(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Silumgar Butcher")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting a creature gives target creature -3/-3, killing a 3/3") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val butcher = castButcher(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass() // resolve the reflexive -3/-3

        driver.getGraveyard(me) shouldContain fodder
        driver.getGraveyard(opp) shouldContain victim
        driver.getPermanents(me) shouldContain butcher
    }

    test("declining the exploit sacrifice shrinks nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        castButcher(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getPermanents(me) shouldContain fodder
        driver.getPermanents(opp) shouldContain victim
    }
})
