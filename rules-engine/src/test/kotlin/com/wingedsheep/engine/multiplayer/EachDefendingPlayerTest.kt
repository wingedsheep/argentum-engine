package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.composite.ForEachExecutor
import com.wingedsheep.engine.handlers.effects.library.GatherCardsExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EachDefendingPlayerTest : FunSpec({
    val players = (0..3).map { EntityId("player-$it") }
    val creature = card("Test Attacker") {
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val reference = Player.EachDefendingPlayer
    val target = EffectTarget.PlayerRef(reference)
    val predicates = PredicateEvaluator(cardRegistry = null)
    val context = EffectContext(sourceId = null, controllerId = players[2])

    fun GameState.permanent(owner: EntityId, defender: EntityId? = null): Pair<GameState, EntityId> {
        val id = EntityId.generate()
        var container = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = creature.name, name = creature.name,
                manaCost = creature.manaCost, typeLine = creature.typeLine,
                baseStats = creature.creatureStats, ownerId = owner,
            ),
            ControllerComponent(owner),
        )
        if (defender != null) container = container.with(AttackingComponent(defender))
        return withEntity(id, container).addToZone(ZoneKey(owner, Zone.BATTLEFIELD), id) to id
    }

    fun combat(): GameState {
        var state = GameState(turnOrder = players, activePlayerId = players[2], phase = Phase.COMBAT, step = Step.DECLARE_BLOCKERS)
        // Deliberately add defenders out of seat order, with two attackers against one player.
        for (defender in listOf(players[1], players[3], players[1])) {
            state = state.permanent(players[2], defender).first
        }
        return state
    }

    test("plural resolution and counting include each defending seat once in wrapping APNAP order") {
        val state = combat()
        TargetResolutionUtils.resolvePlayerTargets(target, state, context) shouldBe listOf(players[3], players[0], players[1])
        predicates.amounts.evaluate(state, DynamicAmount.PlayerCount(reference), context) shouldBe 3
        TargetResolutionUtils.resolvePlayerRef(reference, context, state) shouldBe null
    }

    test("per-player loop visits un-attacked defenders too in APNAP order") {
        val visits = mutableListOf<EntityId>()
        val executor = ForEachExecutor({ state, _, inner ->
            visits.add(inner.controllerId)
            EffectResult.success(state)
        }, predicates)
        val effect = Effects.ForEachPlayer(reference, listOf(Effects.DrawCards(1))) as ForEachEffect
        executor.execute(combat(), effect, context).error shouldBe null
        visits shouldBe listOf(players[3], players[0], players[1])
    }

    test("outside combat means empty resolution and no loop body executions") {
        val state = combat().copy(phase = Phase.POSTCOMBAT_MAIN, step = Step.POSTCOMBAT_MAIN)
        TargetResolutionUtils.resolvePlayerTargets(target, state, context) shouldBe emptyList()
        predicates.amounts.evaluate(state, DynamicAmount.PlayerCount(reference), context) shouldBe 0
        var visits = 0
        val executor = ForEachExecutor({ next, _, _ -> visits++; EffectResult.success(next) }, predicates)
        val effect = Effects.ForEachPlayer(reference, listOf(Effects.DrawCards(1))) as ForEachEffect
        executor.execute(state, effect, context).error shouldBe null
        visits shouldBe 0
    }

    test("all opposing seats defend before attackers are declared even with restricted attack targets") {
        val state = GameState(turnOrder = players, activePlayerId = players[2], phase = Phase.COMBAT, step = Step.BEGIN_COMBAT)
        TargetResolutionUtils.resolvePlayerTargets(target, state, context) shouldBe listOf(players[3], players[0], players[1])
        TargetResolutionUtils.resolvePlayerTargets(target, state.copy(attackMode = AttackMode.LEFT), context) shouldBe listOf(players[3], players[0], players[1])
        TargetResolutionUtils.resolvePlayerTargets(target, state.copy(attackMode = AttackMode.RIGHT), context) shouldBe listOf(players[3], players[0], players[1])
    }

    test("gather, exists and player filters read only the defending seats") {
        var state = combat()
        val hands = players.associateWith { EntityId.generate() }
        for ((player, id) in hands) {
            state = state.withEntity(id, ComponentContainer.of(CardComponent(
                cardDefinitionId = creature.name, name = creature.name,
                manaCost = creature.manaCost, typeLine = creature.typeLine,
                ownerId = player,
            ))).addToZone(ZoneKey(player, Zone.HAND), id)
        }
        val effect = GatherCardsEffect(CardSource.FromZone(Zone.HAND, reference), storeAs = "hands")
        GatherCardsExecutor(predicates).execute(state, effect, context)
            .updatedCollections["hands"] shouldBe listOf(hands.getValue(players[3]), hands.getValue(players[0]), hands.getValue(players[1]))
        predicates.conditions.evaluate(state, Exists(reference, Zone.HAND, GameObjectFilter.Any), context) shouldBe true
        val predicateContext = PredicateContext(controllerId = players[2])
        for (player in players) {
            predicates.matchesPlayer(state, state.projectedState, reference, player, predicateContext) shouldBe
                (player != players[2])
        }
        val noDefenderCards = state.copy(zones = state.zones.filterKeys { it.zoneType != Zone.HAND || it.ownerId == players[2] })
        predicates.conditions.evaluate(noDefenderCards, Exists(reference, Zone.HAND, GameObjectFilter.Any), context) shouldBe false
    }

    test("shared team turns include the attacked player's defending teammate") {
        val registry = CardRegistry().also { it.register(creature) }
        val game = GameInitializer(registry).initializeGame(GameConfig(
            format = Format.TwoHeadedGiant(),
            players = (1..4).map { PlayerConfig("Player $it", Deck.of(creature.name to 40)) },
            teams = listOf(listOf(0, 1), listOf(2, 3)),
            startingPlayerIndex = 0,
            skipMulligans = true,
        ))
        val p = game.playerIds
        val state = game.state.copy(phase = Phase.COMBAT, step = Step.BEGIN_COMBAT)
        TargetResolutionUtils.resolvePlayerTargets(target, state, EffectContext(null, p[0])) shouldBe listOf(p[2], p[3])
    }
})
