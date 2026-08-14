package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.scenario.PlayerConfig
import com.wingedsheep.gameserver.scenario.ScenarioBuilderService
import com.wingedsheep.gameserver.scenario.ScenarioMode
import com.wingedsheep.gameserver.scenario.ScenarioRequest
import com.wingedsheep.gameserver.scenario.ScenarioSeat
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * A seat that dies while the pod plays on (CR 800.4a) must be told exactly once, whatever killed
 * it. `GamePlayHandler.notifyEliminatedSeats` drives the personal `PlayerEliminated` notice off
 * this bookkeeping; before it existed only a concession produced one, so a player who was simply
 * burned out sat on a dead board with no defeat overlay and no re-seated table.
 */
class EliminationNoticeTest : ScenarioTestBase() {

    private val service get() = ScenarioBuilderService(cardRegistry)

    private fun threeSeatState(): Pair<GameState, List<EntityId>> {
        val build = service.buildScenario(
            ScenarioRequest(
                players = listOf(
                    ScenarioSeat("Alice", PlayerConfig()),
                    ScenarioSeat("Bob", PlayerConfig()),
                    ScenarioSeat("Charlie", PlayerConfig()),
                ),
                mode = ScenarioMode.SELF,
            )
        )
        return build.state to build.playerIds
    }

    private fun newSession() = GameSession(cardRegistry = cardRegistry)

    private fun GameState.markLost(playerId: EntityId, reason: LossReason): GameState =
        updateEntity(playerId) { it.with(PlayerLostComponent(reason)) }

    init {
        test("a seat killed by damage is pending a notice, with its own loss reason") {
            val (state, players) = threeSeatState()
            val session = newSession()
            session.injectStateForDevScenario(state)
            session.unnotifiedEliminations().shouldBeEmpty()

            session.injectStateForDevScenario(state.markLost(players[1], LossReason.LIFE_ZERO))

            session.unnotifiedEliminations() shouldContainExactly listOf(players[1])
            session.getEliminationReason(players[1]) shouldBe GameOverReason.LIFE_ZERO
        }

        test("every loss reason reaches the client, not just concession") {
            val (state, players) = threeSeatState()
            val session = newSession()
            session.injectStateForDevScenario(state.markLost(players[2], LossReason.EMPTY_LIBRARY))

            session.unnotifiedEliminations() shouldContainExactly listOf(players[2])
            session.getEliminationReason(players[2]) shouldBe GameOverReason.DECK_OUT
        }

        test("a seat is only pending until it has been told — the notice never repeats") {
            val (state, players) = threeSeatState()
            val session = newSession()
            val afterDeath = state.markLost(players[0], LossReason.POISON_COUNTERS)
            session.injectStateForDevScenario(afterDeath)

            session.markEliminationNotified(players[0])
            session.unnotifiedEliminations().shouldBeEmpty()

            // Every later broadcast re-injects state; the seat stays dead but stays notified.
            session.injectStateForDevScenario(afterDeath)
            session.unnotifiedEliminations().shouldBeEmpty()
        }

        test("seats queue in elimination order, and a survivor is never pending") {
            val (state, players) = threeSeatState()
            val session = newSession()
            val firstOut = state.markLost(players[2], LossReason.LIFE_ZERO)
            session.injectStateForDevScenario(firstOut)
            session.injectStateForDevScenario(firstOut.markLost(players[0], LossReason.CONCESSION))

            // Bob is still alive, so he never queues.
            session.unnotifiedEliminations() shouldContainExactly listOf(players[2], players[0])
        }
    }
}
