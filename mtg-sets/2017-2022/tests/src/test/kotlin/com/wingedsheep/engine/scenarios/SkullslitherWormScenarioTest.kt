package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Skullslither Worm — "When this creature enters, each opponent discards a card. For each opponent
 * who can't, put two +1/+1 counters on this creature."
 *
 * "Who can't" is decided before the discard: an opponent with one card discards it and does not
 * count; only an opponent who started with an empty hand grows the Worm.
 */
class SkullslitherWormScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.setOpponentHandSize(size: Int) {
        getHand(player2).drop(size).forEach { moveToGraveyard(it) }
    }

    fun GameTestDriver.castWormAndResolve() {
        val worm = putCardInHand(player1, "Skullslither Worm")
        giveMana(player1, Color.BLACK, 4)
        castSpell(player1, worm)
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard++ < 20) {
            if (pendingDecision != null) {
                val choice = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                submitCardSelection(choice.playerId, choice.options.take(choice.minSelections.coerceAtLeast(1)))
            } else {
                passPriority(state.priorityPlayerId!!)
            }
        }
    }

    fun GameTestDriver.wormCounters(): Int {
        val worm = findPermanent(player1, "Skullslither Worm")
        worm shouldNotBe null
        return state.getEntity(worm!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    test("an opponent with cards discards one and the Worm gets no counters") {
        val driver = newGame()
        val opponentHand = driver.getHandSize(driver.player2)

        driver.castWormAndResolve()

        driver.getHandSize(driver.player2) shouldBe opponentHand - 1
        driver.wormCounters() shouldBe 0
    }

    test("an opponent with exactly one card discards it and still doesn't count as unable") {
        val driver = newGame()
        driver.setOpponentHandSize(1)

        driver.castWormAndResolve()

        driver.getHandSize(driver.player2) shouldBe 0
        withClue("the opponent could discard, so no counters") {
            driver.wormCounters() shouldBe 0
        }
    }

    test("an opponent with an empty hand can't discard, so the Worm gets two +1/+1 counters") {
        val driver = newGame()
        driver.setOpponentHandSize(0)

        driver.castWormAndResolve()

        driver.wormCounters() shouldBe 2
        val worm = driver.findPermanent(driver.player1, "Skullslither Worm")!!
        driver.state.projectedState.getPower(worm) shouldBe 5
    }
})
