package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.pumpsNeedAPlannedAttack]: a beginning-of-combat payment for an end-of-turn grant is
 * only worth it on a creature the attack plan would send. 2026-10-10 engine-vs-engine logs
 * (`production-candidate-expiring`), game 3 turn 19: a ready Voltstorm Angel paid {E}{E} for
 * vigilance and lifelink, then the AI declared no attackers.
 */
class PumpsNeedAPlannedAttackTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** Pass priority until the beginning-of-combat trigger asks its yes/no. */
    private fun TestGame.advanceToYesNo(): YesNoDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    /** Answer as `AIPlayer` wires it: [holdUnusablePumps] on, the planner present only under [planned]. */
    private fun TestGame.answer(decision: YesNoDecision, planned: Boolean): Boolean {
        val simulator = GameSimulator(cardRegistry)
        val evaluator = AIPlayer.defaultEvaluator()
        val responder = DecisionResponder(
            simulator,
            evaluator,
            intents = intents,
            holdUnusablePumps = true,
            combatAdvisor = if (planned) CombatAdvisor(simulator, evaluator, cardRegistry) else null,
        )
        simulator.decisionResolver = { s, d -> responder.respond(s, d, d.playerId) }
        return (responder.respond(state, decision, player1Id) as YesNoResponse).choice
    }

    private fun TestGame.giveEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { container ->
            val current = container.get<CountersComponent>() ?: CountersComponent()
            container.with(current.withAdded(CounterType.ENERGY, amount))
        }
    }

    init {

        test("g3 T19: no energy paid for a grant on an Angel the attack plan keeps home") {
            // A 5/5 flier on the other side eats the 4/4 Angel, vigilance or not, so the plan is
            // no attack — and vigilance and lifelink on a creature that stays home buy nothing.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Voltstorm Angel")
                .withCardOnBattlefield(2, "Shivan Dragon")
                .build()
            game.giveEnergy(3)
            val decision = game.advanceToYesNo()

            game.answer(decision, planned = false) shouldBe true
            game.answer(decision, planned = true) shouldBe false
        }

        test("an Angel the attack plan sends still pays for its grant") {
            // The control: nothing on the other side can block a flier, so the Angel attacks and
            // lifelink is real life.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Voltstorm Angel")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.giveEnergy(3)
            val decision = game.advanceToYesNo()

            game.answer(decision, planned = true) shouldBe true
        }
    }
}
