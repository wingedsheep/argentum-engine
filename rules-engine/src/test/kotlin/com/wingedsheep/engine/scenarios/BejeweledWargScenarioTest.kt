package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bejeweled Warg (HOB) — {1}{G} Creature — Wolf 3/2
 *
 * Trample
 * Whenever this creature deals combat damage to a player, choose one —
 * • Put a +1/+1 counter on target Wolf you control.
 * • Create a Treasure token.
 *
 * Proves the modal *ability* (not a modal spell) reaches a mode decision off combat damage, and
 * that each mode does what it says.
 */
class BejeweledWargScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    /**
     * Push through blockers and combat damage until the trigger's mode decision surfaces.
     *
     * The mode arrives as the ability is put onto the stack (CR 603.3c), so the
     * [ChooseOptionDecision] carries `phase = TRIGGER` and the ability is not on the stack yet when
     * it is answered — see `ModalTriggeredAbilityOnStackTest` for the guarantee itself.
     */
    private fun advanceToModeDecision(game: TestGame): ChooseOptionDecision {
        var blockersDeclared = false
        var guard = 0
        while (guard++ < 60) {
            val pending = game.getPendingDecision()
            if (pending is ChooseOptionDecision) return pending
            when {
                pending is CombatResolutionDecision -> game.submitDefaultCombatDamage()
                pending is AssignDamageDecision -> game.submitDefaultDamageAssignment()
                pending != null -> error("unexpected pending decision: $pending")
                game.state.step == Step.DECLARE_BLOCKERS && !blockersDeclared -> {
                    blockersDeclared = true
                    game.declareNoBlockers()
                }
                else -> game.passPriority()
            }
        }
        error("no mode decision surfaced; last was ${game.getPendingDecision()}")
    }

    init {
        context("Bejeweled Warg") {

            test("mode 1 puts a +1/+1 counter on a Wolf you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bejeweled Warg")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val warg = game.findPermanent("Bejeweled Warg")!!
                game.declareAttackers(mapOf("Bejeweled Warg" to 2)).error shouldBe null

                val decision = advanceToModeDecision(game)
                game.submitDecision(OptionChosenResponse(decision.id, optionIndex = 0))

                if (game.getPendingDecision() != null) {
                    game.selectTargets(listOf(warg))
                }
                game.resolveStack()

                withClue("The Warg is the only Wolf, so it counters itself up to 4/3") {
                    projector.project(game.state).getPower(warg) shouldBe 4
                }
            }

            test("mode 2 creates a Treasure token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bejeweled Warg")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Bejeweled Warg" to 2)).error shouldBe null

                val decision = advanceToModeDecision(game)
                game.submitDecision(OptionChosenResponse(decision.id, optionIndex = 1))
                game.resolveStack()

                val projected = projector.project(game.state)
                val treasure = game.state.getBattlefield(game.player1Id).firstOrNull { id ->
                    projected.hasSubtype(id, "Treasure")
                }
                withClue("A Treasure token should be on the battlefield") {
                    (treasure != null) shouldBe true
                }
            }
        }
    }
}
