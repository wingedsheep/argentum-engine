package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.SnakeOfTheGoldenGrove
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Snake of the Golden Grove (Born of the Gods #141) — {4}{G} 4/4, Tribute 3.
 * "When this creature enters, if tribute wasn't paid, you gain 4 life."
 */
class SnakeOfTheGoldenGroveScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SnakeOfTheGoldenGrove)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castSnake(me: EntityId, answer: String): EntityId {
        giveMana(me, Color.GREEN, 1)
        giveColorlessMana(me, 4)
        castSpell(me, putCardInHand(me, "Snake of the Golden Grove")).outcome shouldBe Outcome.Done
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe getOpponent(me)
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, decision.options.indexOfFirst { it.startsWith(answer) }))
        return findPermanent(me, "Snake of the Golden Grove")!!
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("tribute paid: it enters with three +1/+1 counters and you gain no life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val snake = driver.castSnake(me, "Pay tribute")

        driver.plusOne(snake) shouldBe 3
        driver.state.projectedState.getPower(snake) shouldBe 7
        driver.state.stack.size shouldBe 0
        driver.getLifeTotal(me) shouldBe 20
    }

    test("tribute not paid: a 4/4 and you gain 4 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val snake = driver.castSnake(me, "Don't pay")

        driver.plusOne(snake) shouldBe 0
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 24
        driver.state.projectedState.getPower(snake) shouldBe 4
    }
})
