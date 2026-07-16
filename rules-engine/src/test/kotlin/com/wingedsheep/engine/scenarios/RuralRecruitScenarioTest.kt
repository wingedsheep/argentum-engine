package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rural Recruit (VOW #216) — {3}{G} 1/1 Creature — Human Peasant, Training + an ETB Boar maker.
 *
 * "When this creature enters, create a 3/1 green Boar creature token." The Boar's power (3) exceeds
 * the Recruit's own (1), so the token is exactly the greater-power partner the Recruit needs to
 * train (CR 702.149a). This proves the two pieces compose: the ETB token is what enables Training.
 *
 * Rural Recruit is cast as a real spell (not placed directly), because ETB triggers only fire on an
 * actual zone change into the battlefield — direct placement would skip the Boar.
 */
class RuralRecruitScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.tokenNamed(playerId: EntityId, name: String): EntityId? =
        getPermanents(playerId).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    test("Rural Recruit's ETB makes a 3/1 Boar token that then enables its Training") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val recruitCard = driver.putCardInHand(me, "Rural Recruit")               // {3}{G}
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, recruitCard).isSuccess shouldBe true
        driver.drainStack()                                                       // resolve Rural Recruit + its ETB

        val recruit = driver.getPermanents(me).first { driver.getCardName(it) == "Rural Recruit" }
        val boar = driver.tokenNamed(me, "Boar Token") ?: driver.tokenNamed(me, "Boar")
            ?: error("Rural Recruit should have created a Boar token")

        // The Boar is a 3/1: base power 3 beats the Recruit's 1.
        driver.plusOneCounters(recruit) shouldBe 0

        // Freshly on the battlefield this turn — clear sickness so both can attack.
        listOf(recruit, boar).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(recruit, boar), opp)                    // Recruit power 1 alongside Boar power 3
        driver.drainStack()

        // The Boar's greater power let the Recruit train.
        driver.plusOneCounters(recruit) shouldBe 1
    }
})
