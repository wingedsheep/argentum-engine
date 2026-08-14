package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Passenger Ferry (SPM) — {3} Artifact — Vehicle 4/3, Crew 2.
 *
 *  "Whenever this Vehicle attacks, you may pay {U}. When you do, another target attacking
 *   creature can't be blocked this turn."
 *
 * Verifies the two load-bearing pieces:
 *  1. Crew 2 turns the Vehicle into an artifact creature (so it can attack).
 *  2. The attack trigger's reflexive "you may pay {U}. When you do, …" — paying {U} grants
 *     CANT_BE_BLOCKED to *another* attacking creature (the source Vehicle is excluded via
 *     `.other()`), while declining leaves that creature blockable.
 */
class PassengerFerryScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    test("Crew 2 animates the Vehicle; paying {U} makes another attacker unblockable") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val ferry = d.putPermanentOnBattlefield(you, "Passenger Ferry")
        d.removeSummoningSickness(ferry)

        // Crew partner (tapped for the Crew 2 cost — its own summoning sickness is irrelevant).
        val crewBear = d.putCreatureOnBattlefield(you, "Grizzly Bears") // 2/2, power 2 = Crew 2

        // The "another attacking creature" that will be made unblockable.
        val runner = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(runner)

        d.putLandOnBattlefield(you, "Island") // taps for the optional {U}

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Not a creature until crewed.
        d.state.projectedState.isCreature(ferry) shouldBe false
        d.submitSuccess(CrewVehicle(you, ferry, listOf(crewBear)))
        d.bothPass() // resolve the Crew activation
        d.state.projectedState.isCreature(ferry) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Both the animated Vehicle and the runner attack.
        d.declareAttackers(you, listOf(ferry, runner), opponent).isSuccess shouldBe true

        // Answer the reflexive decisions in engine order: pay {U} (yes) → auto-pay mana →
        // choose the *other* attacking creature as the target.
        var targeted = false
        var guard = 0
        while (!targeted && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> d.submitYesNo(you, true)
                is SelectManaSourcesDecision -> d.submitManaAutoPayOrDecline(you, true)
                is ChooseTargetsDecision -> {
                    d.submitTargetSelection(you, listOf(runner)); targeted = true
                }
                else -> d.bothPass()
            }
        }
        targeted shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        projector.project(d.state).hasKeyword(runner, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        // The Vehicle itself ("another") is never granted the restriction.
        projector.project(d.state).hasKeyword(ferry, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
    }

    test("declining the {U} payment leaves the other attacker blockable") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1

        val ferry = d.putPermanentOnBattlefield(you, "Passenger Ferry")
        d.removeSummoningSickness(ferry)
        val crewBear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val runner = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(runner)
        d.putLandOnBattlefield(you, "Island")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.submitSuccess(CrewVehicle(you, ferry, listOf(crewBear)))
        d.bothPass()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(ferry, runner), opponent).isSuccess shouldBe true

        // Decline the optional {U}; the reflexive "when you do" never fires.
        var answered = false
        var guard = 0
        while (!answered && guard++ < 40) {
            when (d.pendingDecision) {
                is YesNoDecision -> {
                    d.submitYesNo(you, false); answered = true
                }
                else -> d.bothPass()
            }
        }
        answered shouldBe true

        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        projector.project(d.state).hasKeyword(runner, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
    }
})
