package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Bloated Contaminator (ONE #159) — {2}{G} 4/4 Phyrexian Beast, trample, toxic 1.
 *
 *   Whenever this creature deals combat damage to a player, proliferate.
 *
 * Pins that toxic's poison counter lands with the combat damage, so the proliferate trigger
 * that resolves afterwards can add a second one to the damaged player.
 */
class BloatedContaminatorScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun attackUnblocked(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Bloated Contaminator" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        var guard = 0
        while (game.state.pendingDecision == null && guard++ < 20) {
            game.passPriority().error shouldBe null
        }
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Bloated Contaminator")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("combat damage gives a poison counter, then proliferate adds another and grows a permanent") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }

            attackUnblocked(game)

            game.getLifeTotal(2) shouldBe 16
            count(game, game.player2Id, CounterType.POISON) shouldBe 1

            game.selectCards(listOf(game.player2Id, giant)).error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.POISON) shouldBe 2
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("choosing nothing leaves only the toxic poison counter") {
            val game = board()

            attackUnblocked(game)
            game.skipSelection().error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.POISON) shouldBe 1
            game.getLifeTotal(2) shouldBe 16
        }
    }
}
