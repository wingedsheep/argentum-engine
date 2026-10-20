package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerStepScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [DelayedTriggerStepScope.ANY_MAIN_PHASE] — "at the beginning of your next main phase".
 *
 * The precombat and postcombat main phases are individually and collectively "the main phase"
 * (CR 505.1), and every main phase after the first is a postcombat main phase, additional ones
 * included (CR 505.1a). So a trigger scheduled during a main phase or combat waits for the *next*
 * main phase of the turn rather than skipping to the next turn's precombat one, and fires once.
 */
class DelayedTriggerNextMainPhaseTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, skipMulligans = true, startingPlayer = 0)
        return driver
    }

    fun GameTestDriver.schedule(player: EntityId, step: Step, scope: DelayedTriggerStepScope) = replaceState(
        state.copy(
            delayedTriggers = state.delayedTriggers + DelayedTriggeredAbility(
                id = "next-main-${state.delayedTriggers.size}", effect = Effects.GainLife(3), fireAtStep = step,
                stepScope = scope, sourceId = player, sourceName = "Scheduled Gain", controllerId = player,
                fireOnPlayerId = player,
            )
        )
    )

    /** Pass until the scheduled trigger has left the delayed-trigger list, returning the step it fired in. */
    fun GameTestDriver.passUntilFired(maxRounds: Int = 200): Pair<Step, Int> {
        var guard = 0
        while (state.delayedTriggers.isNotEmpty() && guard < maxRounds) {
            bothPass()
            guard++
        }
        state.delayedTriggers shouldBe emptyList()
        return currentStep to state.turnNumber
    }

    test("scheduled in the precombat main phase, it fires at that turn's postcombat main phase") {
        val d = createDriver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val turn = d.state.turnNumber
        d.schedule(d.player1, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.ANY_MAIN_PHASE)

        d.passUntilFired() shouldBe (Step.POSTCOMBAT_MAIN to turn)
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("scheduled during combat, it fires at the postcombat main phase") {
        val d = createDriver()
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val turn = d.state.turnNumber
        d.schedule(d.player1, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.ANY_MAIN_PHASE)

        d.passUntilFired() shouldBe (Step.POSTCOMBAT_MAIN to turn)
    }

    test("scheduled before its controller's turn, it fires at their precombat main phase") {
        val d = createDriver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("player 2's trigger ignores player 1's postcombat main phase") {
            d.schedule(d.player2, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.ANY_MAIN_PHASE)
        }

        val (step, _) = d.passUntilFired()
        step shouldBe Step.PRECOMBAT_MAIN
        d.state.activePlayerId shouldBe d.player2
    }

    test("an additional main phase is a main phase: scheduled in the postcombat main, it fires there") {
        val d = createDriver()
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val turn = d.state.turnNumber
        d.replaceState(d.state.updateEntity(d.player1) { it.with(AdditionalPhasesComponent(listOf(QueuedPhase(ExtraPhaseKind.MAIN)))) })
        d.schedule(d.player1, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.ANY_MAIN_PHASE)

        d.passUntilFired() shouldBe (Step.POSTCOMBAT_MAIN to turn)
    }

    test("it is a one-shot: after firing, the next main phase adds nothing") {
        val d = createDriver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.schedule(d.player1, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.ANY_MAIN_PHASE)
        d.passUntilFired()
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("the default STEP scope still waits for the named step exactly") {
        val d = createDriver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.schedule(d.player1, Step.PRECOMBAT_MAIN, DelayedTriggerStepScope.STEP)

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("this turn's postcombat main phase does not count for a precombat-only trigger") {
            d.state.delayedTriggers.size shouldBe 1
        }
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.state.delayedTriggers shouldBe emptyList()
    }

    test("ANY_MAIN_PHASE must be paired with a main-phase step") {
        shouldThrow<IllegalArgumentException> {
            CreateDelayedTriggerEffect(step = Step.END, effect = Effects.GainLife(1), stepScope = DelayedTriggerStepScope.ANY_MAIN_PHASE)
        }
    }
})
