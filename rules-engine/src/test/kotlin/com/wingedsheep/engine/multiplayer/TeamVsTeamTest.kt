package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.actions.special.ConcedeHandler
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Team vs. Team variant (CR 808).
 *
 * Unlike Two-Headed Giant (CR 810), a Team-vs-Team game shares **nothing** (CR 808.5): each player
 * keeps their own life total, takes their own turn in seat order (CR 808.4), draws and untaps alone,
 * attacks and blocks only on their own behalf, and is eliminated **individually** when their own life
 * hits 0 (CR 104.3b). A team loses only once *all* of its players have left (CR 104.2c) — the last
 * team with a player still in wins. Membership is still shared: opponents exclude teammates and the
 * win check is by team.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order: p0,p1 = team 0 (starting);
 * p2,p3 = team 1. Behaviourally this is the same board as the 2HG suite, so the two files are direct
 * before/after contrasts of the same capability flags.
 */
class TeamVsTeamTest : FunSpec({

    val forest = "Forest"

    fun registry(): CardRegistry = CardRegistry().also { it.register(TestCards.all) }

    fun checker() = StateBasedActionChecker(cardRegistry = registry())

    fun boot(
        format: Format = Format.TeamVsTeam(),
        teams: List<List<Int>>? = listOf(listOf(0, 1), listOf(2, 3)),
        playerCount: Int = 4,
    ): Triple<GameState, List<EntityId>, ActionProcessor> {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = format,
                players = (1..playerCount).map { PlayerConfig("Player $it", Deck.of(forest to 40)) },
                teams = teams,
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return Triple(result.state, result.playerIds, ActionProcessor(registry()))
    }

    fun lost(state: GameState, p: EntityId) = state.getEntity(p)!!.has<PlayerLostComponent>()
    fun handSize(state: GameState, p: EntityId) = state.getZone(ZoneKey(p, Zone.HAND)).size

    /** Pass priority (answering any discard decision) until [predicate] holds or the cap is hit. */
    fun drive(start: GameState, proc: ActionProcessor, cap: Int = 600, predicate: (GameState) -> Boolean): GameState {
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

    // ---------------------------------------------------------------------------------------------
    // Life: each player has their own total (CR 808.5) — not a shared pool.
    // ---------------------------------------------------------------------------------------------

    test("each player has their own life total; damaging one teammate doesn't touch the other (CR 808.5)") {
        val (state, p, _) = boot()
        state.lifeTotal(p[0]) shouldBe 20
        state.lifeTotal(p[1]) shouldBe 20

        val hurt = DamageUtils.loseLife(state, p[0], 5, LifeChangeReason.LIFE_LOSS).first
        hurt.lifeTotal(p[0]) shouldBe 15
        hurt.lifeTotal(p[1]) shouldBe 20  // teammate untouched — no shared pool
    }

    test("teammates have distinct life owners (no canonical pooling)") {
        val (state, p, _) = boot()
        state.teamLifeOwnerOf(p[0]) shouldBe p[0]
        state.teamLifeOwnerOf(p[1]) shouldBe p[1]
    }

    // ---------------------------------------------------------------------------------------------
    // Elimination: individual (CR 104.3b); the team survives until all members leave (CR 104.2c).
    // ---------------------------------------------------------------------------------------------

    test("a player at 0 life loses ALONE; the teammate plays on and the game continues (CR 104.3b)") {
        val (state, p, _) = boot()
        val zeroed = DamageUtils.loseLife(state, p[0], 20, LifeChangeReason.LIFE_LOSS).first

        val s = checker().checkAndApply(zeroed).newState
        lost(s, p[0]) shouldBe true
        lost(s, p[1]) shouldBe false  // NO team-loss propagation
        lost(s, p[2]) shouldBe false
        lost(s, p[3]) shouldBe false
        s.gameOver shouldBe false      // both teams still have a player
        s.activeTeams.size shouldBe 2
    }

    test("a team loses only once ALL its members are out; the other team wins together (CR 104.2c)") {
        val (state, p, _) = boot()
        // Knock out both members of team 0.
        var dead = DamageUtils.loseLife(state, p[0], 20, LifeChangeReason.LIFE_LOSS).first
        dead = DamageUtils.loseLife(dead, p[1], 20, LifeChangeReason.LIFE_LOSS).first

        val s = checker().checkAndApply(dead).newState
        lost(s, p[0]) shouldBe true
        lost(s, p[1]) shouldBe true
        s.activeTeams.size shouldBe 1
        s.gameOver shouldBe true
        (s.winnerId in listOf(p[2], p[3])) shouldBe true
    }

    test("conceding eliminates only the conceding player; the teammate fights on (no 810.8a propagation)") {
        val (state, p, _) = boot()
        val s = ConcedeHandler(checker()).execute(state, Concede(p[2])).newState

        s.getEntity(p[2])!!.get<PlayerLostComponent>()!!.reason shouldBe LossReason.CONCESSION
        lost(s, p[3]) shouldBe false   // teammate is NOT dragged down with them
        s.gameOver shouldBe false
        s.activeTeams.size shouldBe 2
    }

    test("a can't-lose grant protects only its controller, not the whole team (CR 808 / normal 104)") {
        val (state, p, _) = boot()
        // Put a "can't lose the game" permanent under p1's control (teammate of p0).
        val (permId, withId) = state.newEntity()
        val protectedState = withId
            .withEntity(permId, ComponentContainer.of(ControllerComponent(p[1]), GrantsCantLoseGameComponent))
            .addToZone(ZoneKey(p[1], Zone.BATTLEFIELD), permId)
        // Drop p0 (who does NOT control the grant) to 0 life.
        val zeroed = DamageUtils.loseLife(protectedState, p[0], 20, LifeChangeReason.LIFE_LOSS).first

        val s = checker().checkAndApply(zeroed).newState
        lost(s, p[0]) shouldBe true    // the grant does not reach across to a teammate
        lost(s, p[1]) shouldBe false
        s.gameOver shouldBe false
    }

    // ---------------------------------------------------------------------------------------------
    // Poison: per-player at the normal threshold of 10 (CR 104.3e) — not pooled, not raised to 15.
    // ---------------------------------------------------------------------------------------------

    test("poison is per-player at threshold 10; one poisoned player loses alone") {
        val (state, p, _) = boot()
        val poisoned = state.updateEntity(p[0]) { it.with(CountersComponent(mapOf(CounterType.POISON to 10))) }

        val s = checker().checkAndApply(poisoned).newState
        lost(s, p[0]) shouldBe true
        lost(s, p[1]) shouldBe false
        s.gameOver shouldBe false
    }

    test("poison does NOT pool across the team — 8 + 7 = 15 kills no one (each is below 10)") {
        val (state, p, _) = boot()
        state.teamPoison(p[2]) shouldBe 0
        val poisoned = state
            .updateEntity(p[2]) { it.with(CountersComponent(mapOf(CounterType.POISON to 8))) }
            .updateEntity(p[3]) { it.with(CountersComponent(mapOf(CounterType.POISON to 7))) }

        // teamPoison reads only the player's own counters in Team vs. Team.
        poisoned.teamPoison(p[2]) shouldBe 8
        val s = checker().checkAndApply(poisoned).newState
        lost(s, p[2]) shouldBe false
        lost(s, p[3]) shouldBe false
        s.gameOver shouldBe false
    }

    // ---------------------------------------------------------------------------------------------
    // Membership is still shared: opponents exclude teammates (CR 808 / 802.2).
    // ---------------------------------------------------------------------------------------------

    test("opponents exclude teammates; teammates are recognised") {
        val (state, p, _) = boot()
        state.teamOf(p[0]) shouldBe listOf(p[0], p[1])
        state.teammatesOf(p[0]) shouldBe listOf(p[1])
        state.getOpponents(p[0]) shouldBe listOf(p[2], p[3])
        state.getOpponents(p[2]) shouldBe listOf(p[0], p[1])
    }

    // ---------------------------------------------------------------------------------------------
    // Turns: individual (CR 808.4) — no shared turn ownership, no team-skip turn advancement.
    // ---------------------------------------------------------------------------------------------

    test("turn ownership is individual: only the active player owns the turn; a teammate does not") {
        val (state, p, _) = boot()
        state.isActiveTurnFor(p[0]) shouldBe true
        state.isActiveTurnFor(p[1]) shouldBe false  // teammate of the active player does NOT share the turn
        state.isActiveTurnFor(p[2]) shouldBe false
        // The turn after p0 goes to the next seated player (their teammate p1), not skipping the team.
        state.getNextTeam(p[0]) shouldBe p[1]
        state.getNextTeam(p[1]) shouldBe p[2]
        state.getNextTeam(p[3]) shouldBe p[0]
        // No shared-turn collaboration set: each player acts alone.
        state.sharedTurnTeam(p[0]) shouldBe listOf(p[0])
    }

    test("each player draws on their own turn; no first-turn draw skip in a 4-player game (CR 103.8c)") {
        val (state, p, proc) = boot()
        val openingP0 = handSize(state, p[0])
        val openingP1 = handSize(state, p[1])

        // Reach the starting player's own precombat main — their draw step has happened.
        val atP0Main = drive(state, proc) { it.activePlayerId == p[0] && it.step == Step.PRECOMBAT_MAIN }
        // p0 DREW on turn 1 (no first-turn skip with 3+ players), so hand grew by one...
        handSize(atP0Main, p[0]) shouldBe openingP0 + 1
        // ...and the teammate did NOT draw with them (individual turns, CR 808.4).
        handSize(atP0Main, p[1]) shouldBe openingP1
    }

    test("the turn passes to the next individual player, not skipping the whole team") {
        val (state, p, proc) = boot()
        state.getEntity(p[0])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1
        state.getEntity(p[1])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 0

        val atP1 = drive(state, proc) { it.activePlayerId == p[1] }
        atP1.activePlayerId shouldBe p[1]  // p0's teammate takes the very next turn (not p2)
        atP1.getEntity(p[1])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1
        // p0's counter did not double-advance as a team.
        atP1.getEntity(p[0])!!.get<PlayerTurnsTakenComponent>()!!.count shouldBe 1
    }

    // ---------------------------------------------------------------------------------------------
    // Combat: you defend only your own seat (CR 509.1b) — a teammate can't block for you.
    // ---------------------------------------------------------------------------------------------

    test("only the directly-attacked player defends; a teammate is not pulled in as a defender") {
        val (state, p, _) = boot()
        // p0 has a creature attacking p2; p3 (p2's teammate) is NOT attacked.
        val (attacker, withAttacker) = state.newEntity()
        val combat = withAttacker
            .withEntity(attacker, ComponentContainer.of(ControllerComponent(p[0]), AttackingComponent(p[2])))
            .addToZone(ZoneKey(p[0], Zone.BATTLEFIELD), attacker)

        // In Team vs. Team only p2 defends — the un-attacked teammate p3 may NOT declare blockers.
        CombatDefenders.defendingPlayers(combat) shouldBe setOf(p[2])
    }
})
