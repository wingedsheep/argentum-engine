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
 * Scenario tests for Ghastly Gloomhunter (ZNR #103) — {1}{B} Creature — Zombie Bat, 1/1.
 *
 *   Kicker {3}{B}
 *   Flying, lifelink
 *   If this creature was kicked, it enters with two +1/+1 counters on it.
 */
class GhastlyGloomhunterScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Ghastly Gloomhunter")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val cardId = findCardsInHand(1, "Ghastly Gloomhunter").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = actions.single { it.actionType == if (kicked) "CastWithKicker" else "CastSpell" }
        execute(chosen.action).error shouldBe null
        resolveStack()
    }

    private fun TestGame.plusOneCounters(): Int =
        state.getEntity(findPermanent("Ghastly Gloomhunter")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("unkicked: enters as a 1/1 with no counters") {
            val game = board()
            game.cast(kicked = false)

            withClue("no +1/+1 counters without kicker") { game.plusOneCounters() shouldBe 0 }
            val id = game.findPermanent("Ghastly Gloomhunter")!!
            game.state.projectedState.getPower(id) shouldBe 1
            game.state.projectedState.getToughness(id) shouldBe 1
        }

        test("kicked: enters with two +1/+1 counters as a 3/3") {
            val game = board()
            game.cast(kicked = true)

            withClue("kicked Gloomhunter enters with two +1/+1 counters") { game.plusOneCounters() shouldBe 2 }
            val id = game.findPermanent("Ghastly Gloomhunter")!!
            game.state.projectedState.getPower(id) shouldBe 3
            game.state.projectedState.getToughness(id) shouldBe 3
        }
    }
}
