package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.SurtlandFlinger
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Surtland Flinger (KHM #377) — {3}{R}{R} 4/6 Giant Berserker.
 *
 * "Whenever this creature attacks, you may sacrifice another creature. When you do, this creature
 *  deals damage equal to the sacrificed creature's power to any target. If the sacrificed creature
 *  was a Giant, this creature deals twice that much damage instead."
 *
 * The amount — power, doubled for a Giant — is snapshotted while the creature is still on the
 * battlefield and read back by the reflexive ability after a stack round-trip. These tests pin both
 * halves of that amount and the declined "may".
 */
class SurtlandFlingerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SurtlandFlinger)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Put the Flinger and [fodderName] onto the battlefield, attack with the Flinger, resolve the trigger. */
    fun attackWithFlinger(driver: GameTestDriver, me: EntityId, opp: EntityId, fodderName: String): EntityId {
        val flinger = driver.putCreatureOnBattlefield(me, "Surtland Flinger")
        driver.removeSummoningSickness(flinger)
        val fodder = driver.putCreatureOnBattlefield(me, fodderName)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(flinger), opp).error shouldBe null
        driver.bothPass() // resolve the attack trigger
        return fodder
    }

    test("sacrificing a non-Giant deals damage equal to its power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = attackWithFlinger(driver, me, opp, "Grizzly Bears")

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))

        // The "When you do" reflexive trigger goes on the stack; aim it at the opponent.
        driver.submitTargetSelection(me, listOf(opp))
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        withClue("Grizzly Bears has power 2") { driver.getLifeTotal(opp) shouldBe 18 }
    }

    test("sacrificing a Giant deals twice its power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = attackWithFlinger(driver, me, opp, "Hill Giant")

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitTargetSelection(me, listOf(fodder))
        driver.submitTargetSelection(me, listOf(opp))
        driver.bothPass()

        driver.getGraveyard(me).contains(fodder) shouldBe true
        withClue("Hill Giant is a 3/3 Giant: twice 3 is 6") { driver.getLifeTotal(opp) shouldBe 14 }
    }

    test("declining the sacrifice deals no damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val fodder = attackWithFlinger(driver, me, opp, "Grizzly Bears")

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.getPermanents(me).contains(fodder) shouldBe true
        driver.getLifeTotal(opp) shouldBe 20
    }
})
