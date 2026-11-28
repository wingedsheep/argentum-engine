package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bloodcrazed Neonate (ISD #131) — {1}{R} 2/1 Vampire.
 *
 *   This creature attacks each combat if able.
 *   Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 */
class BloodcrazedNeonateScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Bloodcrazed Neonate")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("it must attack when able") {
            val game = buildGame()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

            game.execute(DeclareAttackers(game.player1Id, emptyMap())).error shouldNotBe null
            game.declareAttackers(mapOf("Bloodcrazed Neonate" to 2)).error shouldBe null
        }

        test("dealing combat damage to a player puts a +1/+1 counter on it") {
            val game = buildGame()
            val neonate = game.findPermanent("Bloodcrazed Neonate")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bloodcrazed Neonate" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            game.plusOnes(neonate) shouldBe 1
            game.state.projectedState.getPower(neonate) shouldBe 3
        }
    }
}
