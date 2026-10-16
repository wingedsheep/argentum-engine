package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vexing Radgull (PIP #41): on combat damage to a player, that player gets two rad counters if
 * they have none; otherwise its controller proliferates. The check reads the *damaged* player's
 * rad counters at resolution.
 */
class VexingRadgullScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun attackWithRadgull(): TestGame {
        val game = scenario().withPlayers("Player1", "Opponent")
            .withCardOnBattlefield(1, "Vexing Radgull", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        return game
    }

    init {
        test("a player with no rad counters gets two") {
            val game = attackWithRadgull()
            // The attacker's own rad counter doesn't matter — only the damaged player's.
            seed(game, game.player1Id, CounterType.RAD, 1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Vexing Radgull" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            count(game, game.player2Id, CounterType.RAD) shouldBe 2
            count(game, game.player1Id, CounterType.RAD) shouldBe 1
            game.hasPendingDecision() shouldBe false
        }

        test("a player who already has rad counters makes its controller proliferate instead") {
            val game = attackWithRadgull()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, game.player2Id, CounterType.RAD, 3)
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Vexing Radgull" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player1Id
            decision.options shouldContain game.player2Id
            game.selectCards(listOf(game.player2Id, bears)).error shouldBe null
            game.resolveStack()

            count(game, game.player2Id, CounterType.RAD) shouldBe 4
            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }
    }
}
