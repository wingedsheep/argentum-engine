package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.InnocentTraveler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Innocent Traveler (VOW) — "At the beginning of your upkeep, any opponent may sacrifice a creature
 * of their choice. If no one does, transform this creature."
 *
 * Pins the opponent's prompt: it names the payment ("a creature", not "1 creatures") and the real
 * consequence of declining (the Traveler transforms), not a generic "cause X to be sacrificed".
 */
class InnocentTravelerScenarioTest : FunSpec({

    fun setUp(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(InnocentTraveler)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val traveler = driver.putCreatureOnBattlefield(me, "Innocent Traveler")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        // Walk through the opponent's turn to my next upkeep; the trigger goes on the stack there.
        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.passPriorityUntil(Step.UPKEEP, me)
        driver.bothPass()
        return Triple(driver, opponent, traveler)
    }

    test("the opponent's prompt names the sacrifice and the transform that follows a decline") {
        val (driver, opponent, traveler) = setUp()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponent
        decision.prompt shouldBe "You may sacrifice a creature. If no one does, transform Innocent Traveler."

        driver.submitCardSelection(opponent, emptyList())
        driver.getCardName(traveler) shouldBe "Malicious Invader"
    }

    test("an opponent who sacrifices keeps the Traveler face up") {
        val (driver, opponent, traveler) = setUp()

        val courser = driver.findPermanent(opponent, "Centaur Courser")!!
        driver.submitCardSelection(opponent, listOf(courser))

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getCardName(traveler) shouldBe "Innocent Traveler"
    }
})
