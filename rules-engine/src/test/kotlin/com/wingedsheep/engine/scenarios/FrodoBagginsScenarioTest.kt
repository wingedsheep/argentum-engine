package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Frodo Baggins — "As long as Frodo Baggins is your Ring-bearer, it must be blocked if able."
 * Exercises the new `MustBeBlocked` static ability (gated on `SourceIsRingBearer`) honored by
 * BlockPhaseManager. Frodo is designated Ring-bearer by being tempted (Birthday Escape).
 */
class FrodoBagginsScenarioTest : FunSpec({

    fun driverWithRingBearerFrodo(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val frodo = driver.putCreatureOnBattlefield(p1, "Frodo Baggins")
        driver.removeSummoningSickness(frodo)
        // The Ring's level-1 temptation makes the bearer "can't be blocked by creatures with greater
        // power", so the blocker must have power <= Frodo's (1). Savannah Lions is 1/1.
        driver.putCreatureOnBattlefield(p2, "Savannah Lions")

        // Designate Frodo as the Ring-bearer by tempting p1 (Birthday Escape).
        val spell = driver.putCardInHand(p1, "Birthday Escape")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, spell)
        driver.bothPass() // resolve: draw, then pause to choose a Ring-bearer
        val dec = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(p1, CardsSelectedResponse(dec.id, listOf(frodo)))
        driver.state.getEntity(frodo)?.get<RingBearerComponent>()?.ownerId shouldBe p1

        return Triple(driver, frodo, p2)
    }

    test("Ring-bearer Frodo must be blocked if able") {
        val (driver, frodo, p2) = driverWithRingBearerFrodo()
        val p1 = driver.activePlayer!!

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(frodo), p2).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // p2 controls a Savannah Lions that can block, so declaring no blockers is illegal.
        driver.declareBlockers(p2, emptyMap()).isSuccess shouldBe false

        // Blocking Frodo with the Lions is legal.
        val lions = driver.findPermanent(p2, "Savannah Lions")!!
        driver.declareBlockers(p2, mapOf(lions to listOf(frodo))).isSuccess shouldBe true
    }

    test("non-Ring-bearer Frodo has no must-be-blocked requirement") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val frodo = driver.putCreatureOnBattlefield(p1, "Frodo Baggins")
        driver.removeSummoningSickness(frodo)
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(frodo), p2).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Frodo isn't the Ring-bearer → the defender may decline to block.
        driver.declareBlockers(p2, emptyMap()).isSuccess shouldBe true
    }
})
