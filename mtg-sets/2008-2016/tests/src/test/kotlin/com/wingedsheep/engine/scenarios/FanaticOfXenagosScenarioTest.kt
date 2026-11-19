package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.FanaticOfXenagos
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fanatic of Xenagos (Born of the Gods #147) — {1}{R}{G} 3/3 trample, Tribute 1.
 * "When this creature enters, if tribute wasn't paid, it gets +1/+1 and gains haste until end of turn."
 */
class FanaticOfXenagosScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FanaticOfXenagos)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Fanatic; the opponent answers the tribute question with the option starting [answer]. */
    fun GameTestDriver.castFanatic(me: EntityId, answer: String): EntityId {
        giveMana(me, Color.RED, 1)
        giveMana(me, Color.GREEN, 1)
        giveColorlessMana(me, 1)
        castSpell(me, putCardInHand(me, "Fanatic of Xenagos")).outcome shouldBe Outcome.Done
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe getOpponent(me)
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, decision.options.indexOfFirst { it.startsWith(answer) }))
        return findPermanent(me, "Fanatic of Xenagos")!!
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("tribute paid: it enters as a 4/4 with a +1/+1 counter and no haste") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fanatic = driver.castFanatic(me, "Pay tribute")

        driver.plusOne(fanatic) shouldBe 1
        driver.state.stack.size shouldBe 0
        driver.state.projectedState.getPower(fanatic) shouldBe 4
        driver.state.projectedState.getToughness(fanatic) shouldBe 4
        driver.state.projectedState.hasKeyword(fanatic, Keyword.HASTE) shouldBe false
        driver.state.projectedState.hasKeyword(fanatic, Keyword.TRAMPLE) shouldBe true
    }

    test("tribute not paid: it gets +1/+1 and haste until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fanatic = driver.castFanatic(me, "Don't pay")

        driver.plusOne(fanatic) shouldBe 0
        driver.bothPass()
        driver.state.projectedState.getPower(fanatic) shouldBe 4
        driver.state.projectedState.getToughness(fanatic) shouldBe 4
        driver.state.projectedState.hasKeyword(fanatic, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getPower(fanatic) shouldBe 3
        driver.state.projectedState.hasKeyword(fanatic, Keyword.HASTE) shouldBe false
    }
})
