package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.special.ConcedeHandler
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.player.WinGameExecutor
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.WinGameEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Two-Headed Giant — Phase 3: team win/loss (CR 810.8).
 *
 * Players win and lose only as a team (810.8a): if either teammate loses, the team loses; a
 * conceding player's team loses (810.8b); a team at 0 life (810.8c) or 15 poison (810.8d) loses.
 * A can't-lose grant on either teammate protects the whole team (810.8a). The game ends when only
 * one team remains, and that whole team wins.
 *
 * Teams are [[0,1],[2,3]] with turn order pinned to player order.
 */
class TwoHeadedGiantTeamLossTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().also { it.register(TestCards.all) }

    fun checker() = StateBasedActionChecker(cardRegistry = registry())

    fun boot(format: Format = Format.TwoHeadedGiant(), playerCount: Int = 4): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry()).initializeGame(
            GameConfig(
                format = format,
                players = (1..playerCount).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                teams = if (format is Format.TwoHeadedGiant) listOf(listOf(0, 1), listOf(2, 3)) else null,
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    fun lost(state: GameState, p: EntityId) = state.getEntity(p)!!.has<PlayerLostComponent>()

    test("a team at 0 shared life loses; the opposing team wins together (CR 810.8a/c)") {
        val (state, p) = boot()
        val zeroed = DamageUtils.loseLife(state, p[0], 30, LifeChangeReason.LIFE_LOSS).first

        val result = checker().checkAndApply(zeroed)
        val s = result.newState

        // Both members of team 0 are out; both members of team 1 survive.
        lost(s, p[0]) shouldBe true
        lost(s, p[1]) shouldBe true
        lost(s, p[2]) shouldBe false
        lost(s, p[3]) shouldBe false

        s.gameOver shouldBe true
        (s.winnerId in listOf(p[2], p[3])) shouldBe true
    }

    test("conceding makes the whole team lose (CR 810.8b)") {
        val (state, p) = boot()
        val result = ConcedeHandler(checker()).execute(state, Concede(p[2]))
        val s = result.newState

        // p2 conceded; teammate p3 goes down with the team; team 0 wins.
        s.getEntity(p[2])!!.get<PlayerLostComponent>()!!.reason shouldBe LossReason.CONCESSION
        s.getEntity(p[3])!!.get<PlayerLostComponent>()!!.reason shouldBe LossReason.TEAM_DEFEATED
        lost(s, p[0]) shouldBe false
        lost(s, p[1]) shouldBe false
        s.gameOver shouldBe true
        (s.winnerId in listOf(p[0], p[1])) shouldBe true
    }

    test("one teammate decking out loses the team, and the game-end reason is the real cause") {
        val (state, p) = boot()
        // Simulate the empty-library SBA having marked p1 (deck-out is recorded per-player).
        val marked = state.updateEntity(p[1]) { it.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY)) }

        val result = checker().checkAndApply(marked)
        val s = result.newState

        lost(s, p[0]) shouldBe true // teammate goes down too
        s.getEntity(p[0])!!.get<PlayerLostComponent>()!!.reason shouldBe LossReason.TEAM_DEFEATED
        s.gameOver shouldBe true
        // The end reason reports the real cause (deck-out), not the propagated TEAM_DEFEATED.
        result.events.filterIsInstance<com.wingedsheep.engine.core.GameEndedEvent>()
            .single().reason shouldBe GameEndReason.DECK_EMPTY
    }

    test("poison is pooled by the team and the team loses at 15, not 10 (CR 810.8d / 810.10)") {
        val (state, p) = boot()
        // 8 + 7 = 15 across the team → loss. (Either alone is below the 2HG threshold.)
        val poisoned = state
            .updateEntity(p[2]) { it.with(CountersComponent(mapOf(CounterType.POISON to 8))) }
            .updateEntity(p[3]) { it.with(CountersComponent(mapOf(CounterType.POISON to 7))) }

        val s = checker().checkAndApply(poisoned).newState
        lost(s, p[2]) shouldBe true
        lost(s, p[3]) shouldBe true
        s.gameOver shouldBe true
        (s.winnerId in listOf(p[0], p[1])) shouldBe true
    }

    test("a team with 10 combined poison does NOT lose under the 15 threshold") {
        val (state, p) = boot()
        val poisoned = state
            .updateEntity(p[2]) { it.with(CountersComponent(mapOf(CounterType.POISON to 5))) }
            .updateEntity(p[3]) { it.with(CountersComponent(mapOf(CounterType.POISON to 5))) }

        val s = checker().checkAndApply(poisoned).newState
        lost(s, p[2]) shouldBe false
        s.gameOver shouldBe false
    }

    test("a can't-lose grant on either teammate protects the whole team (CR 810.8a)") {
        val (state, p) = boot()
        // Put a "can't lose the game" permanent under p1's control (teammate of p0).
        val (permId, withId) = state.newEntity()
        val protectedState = withId
            .withEntity(
                permId,
                ComponentContainer.of(ControllerComponent(p[1]), GrantsCantLoseGameComponent)
            )
            .addToZone(ZoneKey(p[1], Zone.BATTLEFIELD), permId)
        // Now drop team 0 to 0 life.
        val zeroed = DamageUtils.loseLife(protectedState, p[0], 30, LifeChangeReason.LIFE_LOSS).first

        val s = checker().checkAndApply(zeroed).newState
        // The team is protected — neither teammate is marked, game continues.
        lost(s, p[0]) shouldBe false
        lost(s, p[1]) shouldBe false
        s.gameOver shouldBe false
    }

    test("'you win the game' wins for your whole team and defeats only opposing teams (CR 810.8a)") {
        val (state, p) = boot()
        val result = WinGameExecutor().execute(
            state, WinGameEffect(), EffectContext(sourceId = null, controllerId = p[0])
        )
        val s = checker().checkAndApply(result.state).newState

        lost(s, p[0]) shouldBe false // winner
        lost(s, p[1]) shouldBe false // winner's teammate is NOT defeated
        lost(s, p[2]) shouldBe true  // opposing team loses
        lost(s, p[3]) shouldBe true
        s.gameOver shouldBe true
        (s.winnerId in listOf(p[0], p[1])) shouldBe true
    }

    test("the game does not end while both teams still have a player standing") {
        val (state, p) = boot()
        // No one has lost: two active teams, the game continues.
        state.activeTeams.size shouldBe 2
        val s = checker().checkAndApply(state).newState
        s.gameOver shouldBe false

        // A single non-lethal loss on one team wipes that whole team (810.8a), so the only way to
        // still have two teams standing is to have no team fully out — which is the case above.
        // Knock out just one player of team 1 and confirm the team-loss is what ends the game.
        val partial = state.updateEntity(p[2]) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) }
        val s2 = checker().checkAndApply(partial).newState
        s2.activeTeams.size shouldBe 1
        s2.gameOver shouldBe true
    }

    test("non-team game is unchanged: a player at 0 loses alone and poison threshold stays 10") {
        val (state, p) = boot(format = Format.Standard, playerCount = 2)
        val zeroed = DamageUtils.loseLife(state, p[0], 20, LifeChangeReason.LIFE_LOSS).first
        val s = checker().checkAndApply(zeroed).newState
        lost(s, p[0]) shouldBe true
        s.gameOver shouldBe true
        s.winnerId shouldBe p[1]

        val (state2, q) = boot(format = Format.Standard, playerCount = 2)
        val poisoned = state2.updateEntity(q[0]) { it.with(CountersComponent(mapOf(CounterType.POISON to 10))) }
        lost(checker().checkAndApply(poisoned).newState, q[0]) shouldBe true
    }
})
