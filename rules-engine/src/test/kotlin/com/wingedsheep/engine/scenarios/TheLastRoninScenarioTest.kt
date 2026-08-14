package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.TheLastRonin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for The Last Ronin (TMT) chapter III — the turn-scoped, per-attacker delayed trigger.
 *
 *   III — Whenever a creature you control attacks alone this turn, put three +1/+1 counters on it.
 *         It gains trample, lifelink, and indestructible until end of turn.
 *
 * Chapter III installs `CreateDelayedTriggerEffect(trigger = attacks(youControl, Alone), binding =
 * ANY, expiry = EndOfTurn, …)`; the per-attacker fan-out in detectEventBasedDelayedTriggers binds the
 * lone attacker to TriggeringEntity so the counters + keywords land on it.
 */
class TheLastRoninScenarioTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheLastRonin, bear))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /**
     * Advance to the precombat main phase of the starting player's [nth] turn — the clock a Saga's
     * lore counters run on. `GameState.turnNumber` counts player turns, and these are duels where
     * the two seats alternate, so the starting player's nth turn is turn `2n - 1`.
     */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    fun GameTestDriver.castSaga(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.BLACK, 5)
        giveMana(controller, Color.GREEN, 1)
        repeat(4) { putCardOnTopOfLibrary(controller, "Mountain") }
        val saga = putCardInHand(controller, "The Last Ronin")
        castSpell(controller, saga)
        resolveStack() // saga enters (lore 1 → chapter I: destroy all creatures) and resolves
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Chapter III pumps a lone attacker with counters, trample, lifelink, and indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = if (controller == driver.player1) driver.player2 else driver.player1

        driver.castSaga(controller)
        driver.advanceToMain(2) // chapter II — mill four, reflexive return
        driver.resolveStack()
        driver.advanceToMain(3) // chapter III installs the delayed trigger
        driver.resolveStack()

        driver.state.delayedTriggers.size shouldBe 1

        val attacker = driver.putCreatureOnBattlefield(controller, "Test Bear")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(controller, listOf(attacker), opponent)
        var guard = 0
        while (driver.plusOneCounters(attacker) == 0 && guard < 40) {
            if (driver.state.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            guard++
        }

        driver.plusOneCounters(attacker) shouldBe 3
        driver.state.projectedState.hasKeyword(attacker, Keyword.TRAMPLE) shouldBe true
        driver.state.projectedState.hasKeyword(attacker, Keyword.LIFELINK) shouldBe true
        driver.state.projectedState.hasKeyword(attacker, Keyword.INDESTRUCTIBLE) shouldBe true
    }
})
