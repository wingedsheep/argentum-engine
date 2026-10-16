package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nightkin Ambusher (PIP #112): its ETB gives target player four rad counters, and it can't be
 * blocked as long as the player it is attacking has a rad counter — a static gate that reads the
 * defending player through the Ambusher's own attack assignment at projection time.
 */
class NightkinAmbusherScenarioTest : ScenarioTestBase() {

    private fun TestGame.rads(playerId: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    private fun TestGame.giveRads(playerId: com.wingedsheep.sdk.model.EntityId, amount: Int) {
        state = state.updateEntity(playerId) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.RAD, amount))
        }
    }

    private fun combatBoard(): TestGame = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Nightkin Ambusher", summoningSickness = false)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("entering gives target player four rad counters") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nightkin Ambusher")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nightkin Ambusher").error shouldBe null
            game.resolveStack()
            withClue("ETB trigger asks for a target player") {
                game.getPendingDecision() shouldNotBe null
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.rads(game.player2Id) shouldBe 4
            game.rads(game.player1Id) shouldBe 0
        }

        test("can't be blocked while the defending player has a rad counter") {
            val game = combatBoard()
            game.giveRads(game.player2Id, 1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Nightkin Ambusher" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val block = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Nightkin Ambusher")))
            withClue("defending player has a rad counter, so the block is illegal") {
                block.error shouldNotBe null
            }
        }

        test("can be blocked when the defending player has no rad counters, even if its controller does") {
            val game = combatBoard()
            game.giveRads(game.player1Id, 3)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Nightkin Ambusher" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val block = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Nightkin Ambusher")))
            withClue("defending player has no rad counters: ${block.error}") {
                block.error shouldBe null
            }
        }
    }
}
