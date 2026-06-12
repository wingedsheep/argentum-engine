package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.life.LoseLifeExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Phase 0 verification gate for `backlog/multiplayer.md`: the engine's core loop is
 * N-player correct once the single-opponent context is gone. A four-player game
 * initializes, priority round-trips through all four seats, a full turn cycle hands
 * the turn to the next player in turn order, [GameState.getOpponents] excludes lost
 * players, an `EachOpponent` effect fans out to all three opponents, and
 * [Player.DefendingPlayer] resolves per CR 802.2a from the attack assignment.
 */
class MultiplayerSmokeTest : FunSpec({

    val vanilla = CardDefinition.creature(
        name = "Smoke Test Bear",
        manaCost = com.wingedsheep.sdk.core.ManaCost.parse("{1}{G}"),
        subtypes = setOf(com.wingedsheep.sdk.core.Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun initFourPlayerGame(): Pair<GameState, List<EntityId>> {
        val registry = CardRegistry()
        registry.register(vanilla)
        val deck = Deck(cards = List(40) { "Smoke Test Bear" })
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..4).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    test("a four-player game initializes with four seats in turn order") {
        val (state, players) = initFourPlayerGame()
        players.size shouldBe 4
        state.turnOrder shouldBe players
        state.activePlayers shouldBe players
        state.activePlayerId shouldBe players[0]
    }

    test("getOpponents returns the three other players in turn order") {
        val (state, players) = initFourPlayerGame()
        state.getOpponents(players[0]) shouldContainExactly listOf(players[1], players[2], players[3])
        state.getOpponents(players[2]) shouldContainExactly listOf(players[0], players[1], players[3])
    }

    test("getOpponents and activePlayers exclude a player who has lost") {
        val (state, players) = initFourPlayerGame()
        val withLoss = state.updateEntity(players[1]) { c ->
            c.with(PlayerLostComponent(LossReason.CONCESSION))
        }
        withLoss.activePlayers shouldContainExactly listOf(players[0], players[2], players[3])
        withLoss.getOpponents(players[0]) shouldContainExactly listOf(players[2], players[3])
    }

    test("priority round-trips through all four players and a full turn cycle advances to the next seat") {
        val (initial, players) = initFourPlayerGame()
        val registry = CardRegistry().also { it.register(vanilla) }
        val processor = ActionProcessor(registry)
        var state = initial

        val playersSeenWithPriority = mutableSetOf<EntityId>()
        var safety = 0
        // Drive the game until the turn passes to the second seat.
        while (state.activePlayerId == players[0]) {
            check(++safety < 500) { "turn never advanced (stuck at step ${state.step})" }
            val prio = state.priorityPlayerId ?: break
            playersSeenWithPriority.add(prio)
            val action = when {
                state.step == Step.DECLARE_ATTACKERS &&
                    state.activePlayerId == prio &&
                    state.getEntity(prio)?.has<AttackersDeclaredThisCombatComponent>() != true ->
                    DeclareAttackers(prio, emptyMap())
                state.step == Step.DECLARE_BLOCKERS && state.pendingDecision == null &&
                    state.activePlayerId != prio -> DeclareBlockers(prio, emptyMap())
                else -> PassPriority(prio)
            }
            val result = processor.process(state, action).result
            check(result.isSuccess || result.isPaused) {
                "action $action failed: ${result.error}"
            }
            state = result.newState
        }

        // Every seat held priority at least once during the first turn (CR 101.4 APNAP
        // ordering means all four get a window before the turn ends).
        playersSeenWithPriority shouldBe players.toSet()
        // The next turn belongs to the next player in turn order.
        state.activePlayerId shouldBe players[1]
    }

    test("an EachOpponent life-loss effect hits all three opponents and not the controller") {
        val (state, players) = initFourPlayerGame()
        val effect = LoseLifeEffect(
            amount = DynamicAmount.Fixed(3),
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        )
        val context = EffectContext(sourceId = null, controllerId = players[0])
        val result = LoseLifeExecutor().execute(state, effect, context)

        fun life(s: GameState, p: EntityId) = s.getEntity(p)?.get<LifeTotalComponent>()?.life
        life(result.state, players[0]) shouldBe 20
        life(result.state, players[1]) shouldBe 17
        life(result.state, players[2]) shouldBe 17
        life(result.state, players[3]) shouldBe 17
    }

    test("Player.DefendingPlayer resolves to the player the source is attacking (CR 802.2a)") {
        val (initial, players) = initFourPlayerGame()
        // Materialize an attacker for player 0 declared against player 2 specifically.
        val attackerId = EntityId.generate()
        val container = com.wingedsheep.engine.state.ComponentContainer.of(
            com.wingedsheep.engine.state.components.identity.CardComponent(
                cardDefinitionId = "Smoke Test Bear",
                name = "Smoke Test Bear",
                manaCost = com.wingedsheep.sdk.core.ManaCost.parse("{1}{G}"),
                typeLine = com.wingedsheep.sdk.core.TypeLine.parse("Creature — Bear"),
                ownerId = players[0]
            ),
            com.wingedsheep.engine.state.components.identity.ControllerComponent(players[0]),
            AttackingComponent(defenderId = players[2])
        )
        val onBattlefield = initial
            .withEntity(attackerId, container)
            .addToZone(
                com.wingedsheep.engine.state.ZoneKey(players[0], com.wingedsheep.sdk.core.Zone.BATTLEFIELD),
                attackerId
            )
        val context = EffectContext(sourceId = attackerId, controllerId = players[0])
        TargetResolutionUtils.resolveDefendingPlayer(context, onBattlefield) shouldBe players[2]
        TargetResolutionUtils.resolvePlayerRef(Player.DefendingPlayer, context, onBattlefield) shouldBe players[2]
    }
})
