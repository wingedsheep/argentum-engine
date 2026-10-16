package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A beneficial target for an until-end-of-turn grant goes to a creature that can still fight this
 * turn. 2026-10-16 engine-vs-engine logs (`live`), game 2 turn 11: Ezekiel Sims's
 * beginning-of-combat +2/+2 went to a Spider-Man cast that main phase, then Ezekiel attacked
 * unpumped. A permanent payoff (a +1/+1 counter) makes no end-of-turn effect, so the preference
 * never touches it.
 */
class PumpTargetCanFightTest : ScenarioTestBase() {

    /** Pass priority until the beginning-of-combat trigger asks for its target. */
    private fun TestGame.advanceToTargets(): ChooseTargetsDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    private fun TestGame.liveTarget(decision: ChooseTargetsDecision): String {
        val response = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).respondToDecision(state, decision)
        val picked = (response as TargetsResponse).selectedTargets.getValue(0).single()
        return state.getEntity(picked)!!.get<CardComponent>()!!.name
    }

    private fun TestGame.addPlusOneCounters(cardName: String, amount: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { container ->
            val current = container.get<CountersComponent>() ?: CountersComponent()
            container.with(current.withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    init {

        test("g2 T11: Ezekiel's +2/+2 goes to the Spider that can attack, not the bigger one cast this turn") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Ezekiel Sims, Spider-Totem")
                .withCardOnBattlefield(1, "Spider-Man, Brooklyn Visionary", summoningSickness = true)
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .build()
            // Spider-Man a 6/5, bigger than the 3/5 Ezekiel, as it was in the game.
            game.addPlusOneCounters("Spider-Man, Brooklyn Visionary", 2)
            val decision = game.advanceToTargets()

            game.liveTarget(decision) shouldBe "Ezekiel Sims, Spider-Totem"
        }

        test("with both Spiders able to attack, the +2/+2 still goes to the bigger one") {
            // The control: the can-fight preference only separates a creature that can use the
            // grant from one that cannot; between two attackers the existing ranking stands.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Ezekiel Sims, Spider-Totem")
                .withCardOnBattlefield(1, "Spider-Man, Brooklyn Visionary")
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .build()
            game.addPlusOneCounters("Spider-Man, Brooklyn Visionary", 2)
            val decision = game.advanceToTargets()

            game.liveTarget(decision) shouldBe "Spider-Man, Brooklyn Visionary"
        }
    }
}
