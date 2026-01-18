package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.Layer
import com.wingedsheep.rulesengine.ecs.layers.Modification
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.layers.ModifierFilter
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Executes effects against an EcsGameState.
 *
 * This is the ECS counterpart to EffectExecutor, working with the new
 * entity-component architecture instead of the legacy GameState.
 *
 * Each effect is executed as a pure transformation:
 * (EcsGameState, Effect, Context) -> EcsGameState
 *
 * Example usage:
 * ```kotlin
 * val executor = EcsEffectExecutor()
 * val newState = executor.execute(
 *     state = gameState,
 *     effect = DealDamageEffect(3, EffectTarget.TargetCreature),
 *     context = ExecutionContext(controllerId, sourceId, targets)
 * )
 * ```
 */
class EcsEffectExecutor {

    /**
     * Execute an effect and return the new game state.
     */
    fun execute(
        state: EcsGameState,
        effect: Effect,
        context: ExecutionContext
    ): ExecutionResult {
        return when (effect) {
            is GainLifeEffect -> executeGainLife(state, effect, context)
            is LoseLifeEffect -> executeLoseLife(state, effect, context)
            is DealDamageEffect -> executeDealDamage(state, effect, context)
            is DrawCardsEffect -> executeDrawCards(state, effect, context)
            is DiscardCardsEffect -> executeDiscardCards(state, effect, context)
            is DestroyEffect -> executeDestroy(state, effect, context)
            is ExileEffect -> executeExile(state, effect, context)
            is ReturnToHandEffect -> executeReturnToHand(state, effect, context)
            is TapUntapEffect -> executeTapUntap(state, effect, context)
            is ModifyStatsEffect -> executeModifyStats(state, effect, context)
            is AddCountersEffect -> executeAddCounters(state, effect, context)
            is AddManaEffect -> executeAddMana(state, effect, context)
            is AddColorlessManaEffect -> executeAddColorlessMana(state, effect, context)
            is CreateTokenEffect -> executeCreateToken(state, effect, context)
            is CompositeEffect -> executeComposite(state, effect, context)
            is ConditionalEffect -> executeConditional(state, effect, context)
            is ShuffleIntoLibraryEffect -> executeShuffleIntoLibrary(state, effect, context)
            is LookAtTopCardsEffect -> executeLookAtTopCards(state, effect, context)
            is MustBeBlockedEffect -> executeMustBeBlocked(state, effect, context)
            is GrantKeywordUntilEndOfTurnEffect -> executeGrantKeywordUntilEndOfTurn(state, effect, context)
            is DestroyAllLandsEffect -> executeDestroyAllLands(state, context)
            is DestroyAllCreaturesEffect -> executeDestroyAllCreatures(state, context)
        }
    }

    // =========================================================================
    // Life Effects
    // =========================================================================

    private fun executeGainLife(
        state: EcsGameState,
        effect: GainLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        val container = state.getEntity(targetPlayerId) ?: return ExecutionResult(state)
        val lifeComponent = container.get<LifeComponent>() ?: return ExecutionResult(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.gainLife(effect.amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.LifeGained(targetPlayerId, effect.amount))
        )
    }

    private fun executeLoseLife(
        state: EcsGameState,
        effect: LoseLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        val container = state.getEntity(targetPlayerId) ?: return ExecutionResult(state)
        val lifeComponent = container.get<LifeComponent>() ?: return ExecutionResult(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.loseLife(effect.amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.LifeLost(targetPlayerId, effect.amount))
        )
    }

    // =========================================================================
    // Damage Effects
    // =========================================================================

    private fun executeDealDamage(
        state: EcsGameState,
        effect: DealDamageEffect,
        context: ExecutionContext
    ): ExecutionResult {
        return when (effect.target) {
            is EffectTarget.Controller -> dealDamageToPlayer(state, context.controllerId, effect.amount, context.sourceId)
            is EffectTarget.Opponent -> {
                val opponentId = getOpponent(context.controllerId, state)
                dealDamageToPlayer(state, opponentId, effect.amount, context.sourceId)
            }
            is EffectTarget.EachOpponent -> {
                var currentState = state
                val events = mutableListOf<EcsEvent>()
                for (playerId in state.getPlayerIds()) {
                    if (playerId != context.controllerId) {
                        val result = dealDamageToPlayer(currentState, playerId, effect.amount, context.sourceId)
                        currentState = result.state
                        events.addAll(result.events)
                    }
                }
                ExecutionResult(currentState, events)
            }
            is EffectTarget.TargetCreature, is EffectTarget.AnyTarget -> {
                val target = context.targets.firstOrNull()
                when (target) {
                    is EcsTarget.Player -> dealDamageToPlayer(state, target.playerId, effect.amount, context.sourceId)
                    is EcsTarget.Permanent -> dealDamageToCreature(state, target.entityId, effect.amount, context.sourceId)
                    null -> ExecutionResult(state)
                }
            }
            else -> ExecutionResult(state)
        }
    }

    private fun dealDamageToPlayer(
        state: EcsGameState,
        playerId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val container = state.getEntity(playerId) ?: return ExecutionResult(state)
        val lifeComponent = container.get<LifeComponent>() ?: return ExecutionResult(state)

        val newState = state.updateEntity(playerId) { c ->
            c.with(lifeComponent.loseLife(amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.DamageDealtToPlayer(sourceId, playerId, amount))
        )
    }

    private fun dealDamageToCreature(
        state: EcsGameState,
        creatureId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val container = state.getEntity(creatureId) ?: return ExecutionResult(state)
        val damageComponent = container.get<DamageComponent>() ?: DamageComponent(0)

        val newState = state.updateEntity(creatureId) { c ->
            c.with(damageComponent.addDamage(amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.DamageDealtToCreature(sourceId, creatureId, amount))
        )
    }

    // =========================================================================
    // Card Drawing Effects
    // =========================================================================

    private fun executeDrawCards(
        state: EcsGameState,
        effect: DrawCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        repeat(effect.count) {
            val result = drawCard(currentState, targetPlayerId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }

    private fun drawCard(state: EcsGameState, playerId: EntityId): ExecutionResult {
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)
        val handZone = ZoneId(ZoneType.HAND, playerId)

        val library = state.getZone(libraryZone)
        if (library.isEmpty()) {
            return ExecutionResult(state, listOf(EcsEvent.DrawFailed(playerId)))
        }

        val cardId = library.first()
        val newState = state
            .removeFromZone(cardId, libraryZone)
            .addToZone(cardId, handZone)

        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.CardDrawn(playerId, cardId, cardName))
        )
    }

    private fun executeDiscardCards(
        state: EcsGameState,
        effect: DiscardCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val handZone = ZoneId(ZoneType.HAND, targetPlayerId)
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, targetPlayerId)
        val hand = currentState.getZone(handZone)

        repeat(minOf(effect.count, hand.size)) {
            val cardId = currentState.getZone(handZone).lastOrNull() ?: return@repeat
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, graveyardZone)

            events.add(EcsEvent.CardDiscarded(targetPlayerId, cardId, cardName))
        }

        return ExecutionResult(currentState, events)
    }

    // =========================================================================
    // Destruction/Removal Effects
    // =========================================================================

    private fun executeDestroy(
        state: EcsGameState,
        effect: DestroyEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        return destroyPermanent(state, target.entityId)
    }

    private fun destroyPermanent(state: EcsGameState, entityId: EntityId): ExecutionResult {
        val container = state.getEntity(entityId) ?: return ExecutionResult(state)
        val cardComponent = container.get<CardComponent>() ?: return ExecutionResult(state)

        val ownerId = cardComponent.ownerId
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, ownerId)

        val newState = state
            .removeFromZone(entityId, ZoneId.BATTLEFIELD)
            .addToZone(entityId, graveyardZone)
            // Clear damage when moving to graveyard
            .updateEntity(entityId) { c -> c.without<DamageComponent>() }

        val events = mutableListOf<EcsEvent>(
            EcsEvent.PermanentDestroyed(entityId, cardComponent.definition.name)
        )

        if (cardComponent.definition.isCreature) {
            events.add(EcsEvent.CreatureDied(entityId, cardComponent.definition.name, ownerId))
        }

        return ExecutionResult(newState, events)
    }

    private fun executeExile(
        state: EcsGameState,
        effect: ExileEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        val container = state.getEntity(target.entityId) ?: return ExecutionResult(state)
        val cardComponent = container.get<CardComponent>() ?: return ExecutionResult(state)

        val newState = state
            .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
            .addToZone(target.entityId, ZoneId.EXILE)

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.PermanentExiled(target.entityId, cardComponent.definition.name))
        )
    }

    private fun executeReturnToHand(
        state: EcsGameState,
        effect: ReturnToHandEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        val container = state.getEntity(target.entityId) ?: return ExecutionResult(state)
        val cardComponent = container.get<CardComponent>() ?: return ExecutionResult(state)

        val ownerHand = ZoneId(ZoneType.HAND, cardComponent.ownerId)

        val newState = state
            .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
            .addToZone(target.entityId, ownerHand)

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.PermanentReturnedToHand(target.entityId, cardComponent.definition.name))
        )
    }

    // =========================================================================
    // Tap/Untap Effects
    // =========================================================================

    private fun executeTapUntap(
        state: EcsGameState,
        effect: TapUntapEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        val newState = if (effect.tap) {
            state.updateEntity(target.entityId) { c -> c.with(TappedComponent) }
        } else {
            state.updateEntity(target.entityId) { c -> c.without<TappedComponent>() }
        }

        val cardName = state.getEntity(target.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        val event = if (effect.tap) {
            EcsEvent.PermanentTapped(target.entityId, cardName)
        } else {
            EcsEvent.PermanentUntapped(target.entityId, cardName)
        }

        return ExecutionResult(newState, listOf(event))
    }

    // =========================================================================
    // Stat Modification Effects
    // =========================================================================

    private fun executeModifyStats(
        state: EcsGameState,
        effect: ModifyStatsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Until-end-of-turn stat modifications would need to be tracked differently
        // For now, this creates a temporary modifier (would need turn-based cleanup)
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        // Store the modifier - in practice this would be handled by a temporary modifier system
        return ExecutionResult(
            state = state,
            events = listOf(EcsEvent.StatsModified(target.entityId, effect.powerModifier, effect.toughnessModifier)),
            temporaryModifiers = if (effect.untilEndOfTurn) {
                listOf(
                    Modifier(
                        layer = Layer.PT_MODIFY,
                        sourceId = context.sourceId,
                        timestamp = Modifier.nextTimestamp(),
                        modification = Modification.ModifyPT(
                            effect.powerModifier,
                            effect.toughnessModifier
                        ),
                        filter = ModifierFilter.Specific(target.entityId)
                    )
                )
            } else emptyList()
        )
    }

    private fun executeAddCounters(
        state: EcsGameState,
        effect: AddCountersEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetId = when (effect.target) {
            is EffectTarget.Self -> context.sourceId
            else -> context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()?.entityId
                ?: return ExecutionResult(state)
        }

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(" ", "_")
                    .replace("+1/+1", "PLUS_ONE_PLUS_ONE")
                    .replace("-1/-1", "MINUS_ONE_MINUS_ONE")
            )
        } catch (e: IllegalArgumentException) {
            return ExecutionResult(state)
        }

        val container = state.getEntity(targetId) ?: return ExecutionResult(state)
        val counters = container.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { c ->
            c.with(counters.add(counterType, effect.count))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.CountersAdded(targetId, counterType.name, effect.count))
        )
    }

    // =========================================================================
    // Mana Effects
    // =========================================================================

    private fun executeAddMana(
        state: EcsGameState,
        effect: AddManaEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val container = state.getEntity(context.controllerId) ?: return ExecutionResult(state)
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        val newState = state.updateEntity(context.controllerId) { c ->
            c.with(manaPool.add(effect.color, effect.amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.ManaAdded(context.controllerId, effect.color.displayName, effect.amount))
        )
    }

    private fun executeAddColorlessMana(
        state: EcsGameState,
        effect: AddColorlessManaEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val container = state.getEntity(context.controllerId) ?: return ExecutionResult(state)
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        val newState = state.updateEntity(context.controllerId) { c ->
            c.with(manaPool.addColorless(effect.amount))
        }

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.ManaAdded(context.controllerId, "Colorless", effect.amount))
        )
    }

    // =========================================================================
    // Token Effects
    // =========================================================================

    private fun executeCreateToken(
        state: EcsGameState,
        effect: CreateTokenEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Token creation requires CardDefinition creation - placeholder for now
        // Full implementation would create a new entity with TokenComponent
        return ExecutionResult(
            state = state,
            events = listOf(EcsEvent.TokenCreated(context.controllerId, effect.count, "${effect.power}/${effect.toughness}"))
        )
    }

    // =========================================================================
    // Composite Effects
    // =========================================================================

    private fun executeComposite(
        state: EcsGameState,
        effect: CompositeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<EcsEvent>()
        val allModifiers = mutableListOf<Modifier>()

        for (subEffect in effect.effects) {
            val result = execute(currentState, subEffect, context)
            currentState = result.state
            allEvents.addAll(result.events)
            allModifiers.addAll(result.temporaryModifiers)
        }

        return ExecutionResult(currentState, allEvents, allModifiers)
    }

    private fun executeConditional(
        state: EcsGameState,
        effect: ConditionalEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val conditionMet = evaluateCondition(state, effect.condition, context)

        return if (conditionMet) {
            execute(state, effect.effect, context)
        } else if (effect.elseEffect != null) {
            execute(state, effect.elseEffect, context)
        } else {
            ExecutionResult(state)
        }
    }

    // =========================================================================
    // Portal Card Effects
    // =========================================================================

    private fun executeShuffleIntoLibrary(
        state: EcsGameState,
        effect: ShuffleIntoLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Placeholder - would shuffle the source card back into library
        return ExecutionResult(state)
    }

    private fun executeLookAtTopCards(
        state: EcsGameState,
        effect: LookAtTopCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Placeholder - would need player choice mechanism
        return ExecutionResult(state)
    }

    private fun executeMustBeBlocked(
        state: EcsGameState,
        effect: MustBeBlockedEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        val newState = state.updateEntity(target.entityId) { c ->
            c.with(MustBeBlockedComponent)
        }

        return ExecutionResult(newState)
    }

    private fun executeGrantKeywordUntilEndOfTurn(
        state: EcsGameState,
        effect: GrantKeywordUntilEndOfTurnEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return ExecutionResult(state)

        // Create a temporary modifier for the keyword grant
        return ExecutionResult(
            state = state,
            events = listOf(EcsEvent.KeywordGranted(target.entityId, effect.keyword)),
            temporaryModifiers = listOf(
                Modifier(
                    layer = Layer.ABILITY,
                    sourceId = context.sourceId,
                    timestamp = Modifier.nextTimestamp(),
                    modification = Modification.AddKeyword(effect.keyword),
                    filter = ModifierFilter.Specific(target.entityId)
                )
            )
        )
    }

    private fun executeDestroyAllLands(
        state: EcsGameState,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val lands = state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.definition?.isLand == true
        }

        for (landId in lands) {
            val result = destroyPermanent(currentState, landId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }

    private fun executeDestroyAllCreatures(
        state: EcsGameState,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val creatures = state.getCreaturesControlledBy(null) // All creatures

        for (creatureId in creatures) {
            val result = destroyPermanent(currentState, creatureId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }

    // =========================================================================
    // Helper Functions
    // =========================================================================

    private fun resolvePlayerTarget(target: EffectTarget, controllerId: EntityId, state: EcsGameState): EntityId {
        return when (target) {
            is EffectTarget.Controller -> controllerId
            is EffectTarget.Opponent -> getOpponent(controllerId, state)
            else -> controllerId
        }
    }

    private fun getOpponent(playerId: EntityId, state: EcsGameState): EntityId {
        return state.getPlayerIds().first { it != playerId }
    }

    private fun evaluateCondition(
        state: EcsGameState,
        condition: Condition,
        context: ExecutionContext
    ): Boolean {
        // Condition evaluation would need to be implemented based on ECS state
        // For now, return true as a placeholder
        return true
    }

    private fun EcsGameState.getCreaturesControlledBy(playerId: EntityId?): List<EntityId> {
        return getBattlefield().filter { entityId ->
            val container = getEntity(entityId) ?: return@filter false
            val card = container.get<CardComponent>() ?: return@filter false
            val controller = container.get<ControllerComponent>()?.controllerId ?: card.ownerId

            card.definition.isCreature && (playerId == null || controller == playerId)
        }
    }
}

/**
 * Context for effect execution.
 */
data class ExecutionContext(
    val controllerId: EntityId,
    val sourceId: EntityId,
    val targets: List<EcsTarget> = emptyList()
)

/**
 * Result of effect execution.
 */
data class ExecutionResult(
    val state: EcsGameState,
    val events: List<EcsEvent> = emptyList(),
    val temporaryModifiers: List<Modifier> = emptyList()
)

/**
 * Target types for ECS effect execution.
 */
sealed interface EcsTarget {
    data class Player(val playerId: EntityId) : EcsTarget
    data class Permanent(val entityId: EntityId) : EcsTarget
}

/**
 * Events generated during effect execution.
 */
sealed interface EcsEvent {
    data class LifeGained(val playerId: EntityId, val amount: Int) : EcsEvent
    data class LifeLost(val playerId: EntityId, val amount: Int) : EcsEvent
    data class DamageDealtToPlayer(val sourceId: EntityId, val targetId: EntityId, val amount: Int) : EcsEvent
    data class DamageDealtToCreature(val sourceId: EntityId, val targetId: EntityId, val amount: Int) : EcsEvent
    data class CardDrawn(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EcsEvent
    data class DrawFailed(val playerId: EntityId) : EcsEvent
    data class CardDiscarded(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EcsEvent
    data class PermanentDestroyed(val entityId: EntityId, val name: String) : EcsEvent
    data class CreatureDied(val entityId: EntityId, val name: String, val ownerId: EntityId) : EcsEvent
    data class PermanentExiled(val entityId: EntityId, val name: String) : EcsEvent
    data class PermanentReturnedToHand(val entityId: EntityId, val name: String) : EcsEvent
    data class PermanentTapped(val entityId: EntityId, val name: String) : EcsEvent
    data class PermanentUntapped(val entityId: EntityId, val name: String) : EcsEvent
    data class StatsModified(val entityId: EntityId, val powerDelta: Int, val toughnessDelta: Int) : EcsEvent
    data class CountersAdded(val entityId: EntityId, val counterType: String, val count: Int) : EcsEvent
    data class ManaAdded(val playerId: EntityId, val color: String, val amount: Int) : EcsEvent
    data class TokenCreated(val controllerId: EntityId, val count: Int, val description: String) : EcsEvent
    data class KeywordGranted(val entityId: EntityId, val keyword: com.wingedsheep.rulesengine.core.Keyword) : EcsEvent
}
