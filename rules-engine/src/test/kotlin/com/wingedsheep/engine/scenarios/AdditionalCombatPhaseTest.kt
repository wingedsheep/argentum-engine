package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for the atomic additional-phase effects (engine gap #136b):
 *
 *  - `Effects.AddCombatPhase`  → queues `[COMBAT]`        → an extra combat phase, NO extra main
 *    (Aurelia, the Warleader / Combat Celebrant / Fear of Missing Out).
 *  - `Effects.AddMainPhase`    → queues `[MAIN]`          → an extra postcombat main phase.
 *  - composing the two         → queues `[COMBAT, MAIN]`  → "an additional combat phase followed by
 *    an additional main phase" (Aggravated Assault, All-Out Assault), the shape that previously was
 *    the only thing `AddCombatPhaseEffect` could express.
 *
 * The redirect happens after the postcombat main phase (engine simplification of CR 500.8). These
 * tests pin the *turn-flow* result by recording the steps the active player visits across one turn:
 * the combat-only shape visits the postcombat main phase exactly once (the natural one), while the
 * combat+main shape visits it twice.
 */
class AdditionalCombatPhaseTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Queue the given extra phases on [player] at the start of the active player's main phase. */
    fun GameTestDriver.queuePhases(player: EntityId, vararg phases: ExtraPhaseKind) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        replaceState(state.updateEntity(player) {
            it.with(AdditionalPhasesComponent(phases.map { kind -> QueuedPhase(kind) }))
        })
    }

    /**
     * Pass priority for both players through the rest of the active player's turn, returning the
     * ordered list of steps entered (from each [StepChangedEvent]). Reading the events — rather than
     * sampling `currentStep` — keeps re-entries of the same step distinct (a redirect into a fresh
     * postcombat main phase right after the natural one emits its own StepChangedEvent).
     */
    fun GameTestDriver.recordStepsThroughTurn(maxRounds: Int = 400): List<Step> {
        // Bound the recording to the *active player's* turn, stopping as soon as the turn passes to
        // the opponent — an inserted extra combat phase is part of the same turn, so watching the
        // active player is what distinguishes "still this turn" from "the turn ended".
        val startPlayer = activePlayer
        val startStep = currentStep
        val firstEventIndex = events.size
        var guard = 0
        while (activePlayer == startPlayer && guard < maxRounds) {
            bothPass()
            guard++
        }
        return buildList {
            add(startStep) // the step the recording started in
            events.drop(firstEventIndex)
                .filterIsInstance<StepChangedEvent>()
                .forEach { add(it.newStep) }
        }
    }

    test("AddCombatPhase queues a single combat phase with no trailing main phase") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!

        driver.queuePhases(attacker, ExtraPhaseKind.COMBAT)
        val steps = driver.recordStepsThroughTurn()

        // The extra combat phase happens: BEGIN_COMBAT is visited twice (natural + inserted).
        steps.count { it == Step.BEGIN_COMBAT } shouldBe 2
        // ...but the postcombat main phase is visited only ONCE — the inserted combat phase goes
        // straight to the end step, granting no additional main phase.
        steps.count { it == Step.POSTCOMBAT_MAIN } shouldBe 1
        // The queue is fully drained by the time the turn ends.
        driver.state.getEntity(attacker)?.has<AdditionalPhasesComponent>() shouldBe false
    }

    test("AddCombatPhase + AddMainPhase (Aggravated Assault shape) is preserved: extra combat AND extra main") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!

        driver.queuePhases(attacker, ExtraPhaseKind.COMBAT, ExtraPhaseKind.MAIN)
        val steps = driver.recordStepsThroughTurn()

        // Extra combat phase happens (BEGIN_COMBAT twice) AND the postcombat main phase is visited
        // twice: the natural one, then the additional one after the bonus combat.
        steps.count { it == Step.BEGIN_COMBAT } shouldBe 2
        steps.count { it == Step.POSTCOMBAT_MAIN } shouldBe 2
        driver.state.getEntity(attacker)?.has<AdditionalPhasesComponent>() shouldBe false
    }

    test("AddMainPhase alone queues a standalone extra main phase, no extra combat") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!

        driver.queuePhases(attacker, ExtraPhaseKind.MAIN)
        val steps = driver.recordStepsThroughTurn()

        steps.count { it == Step.BEGIN_COMBAT } shouldBe 1     // only the natural combat phase
        steps.count { it == Step.POSTCOMBAT_MAIN } shouldBe 2  // natural + the extra main
        driver.state.getEntity(attacker)?.has<AdditionalPhasesComponent>() shouldBe false
    }

    test("queued combat phase orders the natural postcombat main before the inserted combat phase") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!

        driver.queuePhases(attacker, ExtraPhaseKind.COMBAT)
        val steps = driver.recordStepsThroughTurn()

        // The combat-relevant slice of the turn, in order: the natural combat, the natural
        // postcombat main, then the inserted combat phase, then straight to the end step.
        val phaseMarkers = steps.filter {
            it == Step.BEGIN_COMBAT || it == Step.POSTCOMBAT_MAIN || it == Step.END
        }
        phaseMarkers shouldContainExactly listOf(
            Step.BEGIN_COMBAT,      // natural combat
            Step.POSTCOMBAT_MAIN,   // natural postcombat main
            Step.BEGIN_COMBAT,      // inserted extra combat
            Step.END                // end step — NO second postcombat main
        )
    }
})
