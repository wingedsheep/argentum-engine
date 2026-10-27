package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cragplate Baloth (ZNR #183) — {5}{G}{G} Creature — Beast, 6/6.
 *
 *   Kicker {2}{G}
 *   This spell can't be countered.
 *   Hexproof, haste
 *   If this creature was kicked, it enters with four +1/+1 counters on it.
 */
class CragplateBalothScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Cragplate Baloth")
        .withLandsOnBattlefield(1, "Forest", 10)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Cragplate Baloth").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    private fun TestGame.balothCounters(): Int =
        state.getEntity(findPermanent("Cragplate Baloth")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("kicked, it enters with four +1/+1 counters as a 10/10") {
            val game = board()
            game.cast(kicked = true)

            val baloth = game.findPermanent("Cragplate Baloth")!!
            withClue("four counters from the kicker") { game.balothCounters() shouldBe 4 }
            game.state.projectedState.getPower(baloth) shouldBe 10
            game.state.projectedState.getToughness(baloth) shouldBe 10
        }

        test("unkicked, it enters as a plain 6/6") {
            val game = board()
            game.cast(kicked = false)

            val baloth = game.findPermanent("Cragplate Baloth")!!
            withClue("no counters without the kicker") { game.balothCounters() shouldBe 0 }
            game.state.projectedState.getPower(baloth) shouldBe 6
        }
    }
}
