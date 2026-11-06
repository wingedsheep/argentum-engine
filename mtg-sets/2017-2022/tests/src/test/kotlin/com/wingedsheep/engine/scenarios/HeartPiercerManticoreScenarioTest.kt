package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.HeartPiercerManticore
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Heart-Piercer Manticore (AKH #138) — {2}{R}{R} 4/3 Manticore.
 *
 * "When this creature enters, you may sacrifice another creature. When you do, this creature deals
 *  damage equal to that creature's power to any target."
 *
 * The sacrificed creature's power is snapshotted while it is still on the battlefield and read back
 * by the reflexive ability after a stack round-trip. These tests pin the amount, a creature target,
 * and the declined "may".
 */
class HeartPiercerManticoreScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + HeartPiercerManticore)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Manticore with [fodderName] already in play and resolve it; its enters trigger then resolves. */
    fun castManticore(driver: GameTestDriver, me: EntityId, fodderName: String): EntityId {
        val fodder = driver.putCreatureOnBattlefield(me, fodderName)
        val manticore = driver.putCardInHand(me, "Heart-Piercer Manticore")
        driver.giveMana(me, Color.RED, 2)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, manticore).error shouldBe null
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger
        return fodder
    }

    test("sacrificing a creature deals damage equal to its power to a player") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = castManticore(driver, me, "Grizzly Bears")

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))

        // The "When you do" reflexive trigger goes on the stack; aim it at the opponent.
        driver.submitTargetSelection(me, listOf(opp))
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        withClue("Grizzly Bears has power 2") { driver.getLifeTotal(opp) shouldBe 18 }
    }

    test("the damage can be aimed at a creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val victim = driver.putCreatureOnBattlefield(opp, "Hill Giant")

        val fodder = castManticore(driver, me, "Hill Giant")

        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))
        driver.submitTargetSelection(me, listOf(victim))
        driver.bothPass()

        withClue("3 damage from a 3-power sacrifice kills the 3/3") {
            driver.getGraveyard(opp).contains(victim) shouldBe true
        }
        driver.getLifeTotal(opp) shouldBe 20
    }

    test("declining the sacrifice deals no damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = castManticore(driver, me, "Grizzly Bears")

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.getPermanents(me).contains(fodder) shouldBe true
        driver.getLifeTotal(opp) shouldBe 20
    }
})
