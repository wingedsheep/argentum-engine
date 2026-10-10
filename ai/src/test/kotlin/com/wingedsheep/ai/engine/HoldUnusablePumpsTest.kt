package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.holdUnusablePumps], one test per misplay from the 2026-10-10 engine-vs-engine logs
 * (`production-candidate-expiring`), each with the legacy answer pinned beside it so the misplay
 * stays visible.
 */
class HoldUnusablePumpsTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    // Without `combatTricksWaitForBlocks`, so the combat-window bonus the flag has to override is
    // in force in every step these tests probe.
    private val legacyPolicy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }
    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true, holdUnusablePumps = true) }

    private fun TestGame.castVerdict(policy: HoldPolicy, cardName: String): TimingVerdict {
        val card = findCardsInHand(1, cardName).first()
        return policy.verdictFor(state, player1Id, cardName, cast = CastSpell(player1Id, card))
    }

    /** Pass priority until the beginning-of-combat trigger asks its yes/no. */
    private fun TestGame.advanceToYesNo(): YesNoDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }

    private fun TestGame.answer(decision: YesNoDecision, flag: Boolean): Boolean {
        val simulator = GameSimulator(cardRegistry)
        val responder = DecisionResponder(
            simulator,
            AIPlayer.defaultEvaluator(),
            intents = intents,
            holdUnusablePumps = flag,
        )
        // Wired as `AIPlayer` wires it, so the yes branch goes on to pick the reflexive mode.
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

        // ── Rabbit Response: "Creatures you control get +2/+1 until end of turn." ──

        test("g1 T13: a team pump in our declare-attackers step with no attackers is held") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Rabbit Response")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .build().advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.castVerdict(legacyPolicy, "Rabbit Response") shouldBe TimingVerdict.Neutral
            game.castVerdict(policy, "Rabbit Response") shouldBe TimingVerdict.NoWindow
        }

        test("a team pump with an attacker in the fight keeps its combat window") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Rabbit Response")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.passUntilPhase(com.wingedsheep.sdk.core.Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.castVerdict(policy, "Rabbit Response").shouldBeInstanceOf<TimingVerdict.Adjust>()
        }

        test("a trick in our own beginning-of-combat step defers to declare attackers") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Rabbit Response")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .build().advanceToPriority(1, Step.BEGIN_COMBAT)

            game.castVerdict(legacyPolicy, "Rabbit Response") shouldBe TimingVerdict.Neutral
            game.castVerdict(policy, "Rabbit Response") shouldBe TimingVerdict.NoWindow
        }

        // ── Voltstorm Angel: "you may pay {E}{E}. When you do, choose one — …" ──

        test("g3 T15: no energy paid for a grant on a summoning-sick Angel with no other creature") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Voltstorm Angel", summoningSickness = true)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.giveEnergy(3)
            val decision = game.advanceToYesNo()

            game.answer(decision, flag = false) shouldBe true
            game.answer(decision, flag = true) shouldBe false
        }

        test("a sick Angel still pays when the pump lands on a creature that can attack") {
            // The control for the test above: with a ready creature beside it the AI picks the
            // team mode, and a creature that can still attack can spend the +1/+1.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Voltstorm Angel", summoningSickness = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.giveEnergy(3)
            val decision = game.advanceToYesNo()

            game.answer(decision, flag = true) shouldBe true
        }

        test("a team that can attack still pays for its grant") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Voltstorm Angel")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.giveEnergy(3)
            val decision = game.advanceToYesNo()

            game.answer(decision, flag = true) shouldBe true
        }
    }
}
