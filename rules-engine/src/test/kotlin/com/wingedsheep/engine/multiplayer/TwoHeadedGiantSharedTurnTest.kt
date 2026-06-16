package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 4: shared team turns (CR 805 / 810.6).
 *
 * A team takes ONE turn together: both members untap and draw (805.4b), each may play a land
 * (805.4c) and act at sorcery speed on the team's turn (805.5a), the turn passes team-by-team
 * (805.4), and the starting team skips its first draw (810.6). Priority still cycles per player —
 * that already gives each teammate a window — so this verifies turn *structure* and the
 * turn-ownership gates, not the priority machinery.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order: p0,p1 = team 0 (starting);
 * p2,p3 = team 1.
 */
class TwoHeadedGiantSharedTurnTest : FunSpec({

    val forest = "Forest"

    fun registry(): CardRegistry = CardRegistry().also { it.register(TestCards.all) }

    fun boot(): Triple<GameState, List<EntityId>, ActionProcessor> {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = Format.TwoHeadedGiant(),
                players = (1..4).map { PlayerConfig("Player $it", Deck.of(forest to 40)) },
                teams = listOf(listOf(0, 1), listOf(2, 3)),
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return Triple(result.state, result.playerIds, ActionProcessor(registry()))
    }

    fun handSize(state: GameState, p: EntityId) = state.getZone(ZoneKey(p, Zone.HAND)).size

    /** Pass priority (answering any discard decision) until [predicate] holds or the cap is hit. */
    fun drive(start: GameState, proc: ActionProcessor, cap: Int = 400, predicate: (GameState) -> Boolean): GameState {
        var state = start
        var n = 0
        while (!predicate(state)) {
            check(++n < cap) { "drive never reached target (step=${state.step}, active=${state.activePlayerId})" }
            val pending = state.pendingDecision
            if (pending is com.wingedsheep.engine.core.SelectCardsDecision) {
                val resp = com.wingedsheep.engine.core.CardsSelectedResponse(pending.id, pending.options.take(pending.minSelections))
                state = proc.process(state, com.wingedsheep.engine.core.SubmitDecision(pending.playerId, resp)).result.newState
                continue
            }
            val prio = state.priorityPlayerId ?: break
            state = proc.process(state, PassPriority(prio)).result.newState
        }
        return state
    }

    test("isActiveTurnFor / getNextTeam know the team, not the individual") {
        val (state, p, _) = boot()
        state.isActiveTurnFor(p[0]) shouldBe true
        state.isActiveTurnFor(p[1]) shouldBe true  // teammate of the active player
        state.isActiveTurnFor(p[2]) shouldBe false
        // The turn after team 0 belongs to team 1 (its first member), not to the teammate p1.
        state.getNextTeam(p[0]) shouldBe p[2]
        state.getNextTeam(p[2]) shouldBe p[0]
    }

    test("the starting team skips its first draw, but BOTH teammates draw on the next team's turn (CR 810.6 / 805.4b)") {
        val (state, p, proc) = boot()
        val openingP0 = handSize(state, p[0])
        val openingP1 = handSize(state, p[1])
        val openingP2 = handSize(state, p[2])
        val openingP3 = handSize(state, p[3])

        // Drive into team 1's precombat main (their full beginning phase has happened).
        val atTeam1Main = drive(state, proc) {
            it.activePlayerId == p[2] && it.step == Step.PRECOMBAT_MAIN
        }

        // Starting team (0) skipped the first draw entirely — neither member ever drew on turn 1
        // (hands only shrink, never grew; here they never played, so stay at opening size).
        handSize(atTeam1Main, p[0]) shouldBe openingP0
        handSize(atTeam1Main, p[1]) shouldBe openingP1
        // Team 1 drew on their turn — BOTH members drew exactly one card (805.4b), no first-team skip.
        handSize(atTeam1Main, p[2]) shouldBe openingP2 + 1
        handSize(atTeam1Main, p[3]) shouldBe openingP3 + 1
    }

    test("the turn passes to the next TEAM, and both teammates' turn counters advance") {
        val (state, p, proc) = boot()
        // Team 0 starts; turn counter is 1 for both starting-team members at the game's first turn.
        state.getEntity(p[0])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1

        val atTeam1 = drive(state, proc) { it.activePlayerId == p[2] }
        // The active player advanced to team 1's representative (p2), NOT to p0's teammate p1.
        atTeam1.activePlayerId shouldBe p[2]
        // Both team-1 members took the turn together (805.4): both counters incremented to 1.
        atTeam1.getEntity(p[2])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1
        atTeam1.getEntity(p[3])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1
    }

    test("the non-active teammate may play a land on the team's turn (CR 805.4c / 805.5a)") {
        val (state, p, proc) = boot()
        // Reach team 0's precombat main with the teammate (p1) holding priority: pass once from p0.
        var s = drive(state, proc) { it.activePlayerId == p[0] && it.step == Step.PRECOMBAT_MAIN && it.priorityPlayerId == p[0] }
        s = proc.process(s, PassPriority(p[0])).result.newState
        s.priorityPlayerId shouldBe p[1] // priority cycled to the teammate

        val forestInHand = s.getZone(ZoneKey(p[1], Zone.HAND)).first()
        val landsBefore = s.getZone(ZoneKey(p[1], Zone.BATTLEFIELD)).size
        val result = proc.process(s, PlayLand(p[1], forestInHand))
        result.result.isSuccess shouldBe true
        // The teammate's land resolved onto the battlefield even though they are not the active player.
        result.result.newState.getZone(ZoneKey(p[1], Zone.BATTLEFIELD)).size shouldBe landsBefore + 1
    }
})
