package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Utility functions for resolving effect targets from symbolic references to concrete entity IDs.
 *
 * Targets in MTG are late-bound: effects reference targets symbolically
 * (e.g., ContextTarget(0) = "the first target chosen at cast time") and these
 * are resolved at execution time against the current game state.
 */
object TargetResolutionUtils {

    /**
     * Resolve a target from the effect target definition and context.
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Self -> context.pipeline.iterationTarget ?: context.sourceId
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.SpecificEntity -> effectTarget.entityId
            is EffectTarget.TriggeringEntity -> context.triggeringEntityId
            is EffectTarget.PipelineTarget ->
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            else -> null
        }
    }

    /**
     * Resolve a target with access to game state (for targets like EnchantedCreature
     * that need to look up attachment relationships).
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        if (effectTarget is EffectTarget.EnchantedCreature || effectTarget is EffectTarget.EquippedCreature) {
            val sourceId = context.sourceId ?: return null
            return state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
        }
        if (effectTarget is EffectTarget.ChosenCreature) {
            val sourceId = context.sourceId ?: return null
            return state.getEntity(sourceId)?.get<com.wingedsheep.engine.state.components.identity.ChosenCreatureComponent>()?.creatureId
        }
        if (effectTarget is EffectTarget.TargetController) {
            val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
            return state.getEntity(targetEntity)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
        }
        if (effectTarget is EffectTarget.ControllerOfTriggeringEntity) {
            val triggerId = context.triggeringEntityId ?: return null
            val entity = state.getEntity(triggerId) ?: return null
            return entity.get<ControllerComponent>()?.playerId
                ?: entity.get<CardComponent>()?.ownerId
        }
        if (effectTarget is EffectTarget.ControllerOfPipelineTarget) {
            val targetEntityId = context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index) ?: return null
            val entity = state.getEntity(targetEntityId) ?: return null
            return entity.get<ControllerComponent>()?.playerId
                ?: entity.get<CardComponent>()?.ownerId
        }
        if (effectTarget is EffectTarget.PipelineTarget) {
            return context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
        }
        return resolveTarget(effectTarget, context)
    }

    /**
     * Resolve a player target from the effect target definition and context.
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()
            is EffectTarget.PipelineTarget ->
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.You -> context.controllerId
                Player.Opponent, Player.TargetOpponent, Player.EachOpponent -> context.opponentId
                Player.TargetPlayer, Player.Any -> context.targets.firstOrNull()?.toEntityId()
                Player.TriggeringPlayer -> context.triggeringPlayerId ?: context.triggeringEntityId
                else -> null
            }
            else -> null
        }
    }

    /**
     * Resolve a player target with access to game state (for relational references like OwnerOf/ControllerOf).
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext, state: GameState): EntityId? {
        // Try stateless resolution first
        resolvePlayerTarget(effectTarget, context)?.let { return it }

        // Handle TargetController: resolve the first target, then look up its controller
        if (effectTarget is EffectTarget.TargetController) {
            val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
            return state.getEntity(targetEntity)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
        }

        // Handle ControllerOfPipelineTarget: look up controller of the pipeline-stored entity
        if (effectTarget is EffectTarget.ControllerOfPipelineTarget) {
            val targetEntityId = context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index) ?: return null
            return state.getEntity(targetEntityId)?.get<ControllerComponent>()?.playerId
                ?: state.getEntity(targetEntityId)?.get<CardComponent>()?.ownerId
        }

        // Handle state-dependent relational references
        return when (effectTarget) {
            is EffectTarget.PlayerRef -> when (val player = effectTarget.player) {
                is Player.OwnerOf -> {
                    val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
                    state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
                }
                is Player.ControllerOf -> {
                    val targetEntity = context.targets.firstOrNull()?.toEntityId() ?: return null
                    state.getEntity(targetEntity)?.get<ControllerComponent>()?.playerId
                        ?: state.getEntity(targetEntity)?.get<CardComponent>()?.ownerId
                }
                else -> null
            }
            else -> null
        }
    }

    /**
     * Resolve a player target to a list of player IDs (for multi-player effects like "each player").
     */
    fun resolvePlayerTargets(effectTarget: EffectTarget, state: GameState, context: EffectContext): List<EntityId> {
        return when (effectTarget) {
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.BoundVariable -> context.pipeline.namedTargets[effectTarget.name]?.toEntityId()?.let { listOf(it) } ?: emptyList()
            is EffectTarget.PipelineTarget -> {
                context.pipeline.storedCollections[effectTarget.collectionName]?.getOrNull(effectTarget.index)
                    ?.let { listOf(it) } ?: emptyList()
            }
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.Each -> state.turnOrder
                Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.You -> listOf(context.controllerId)
                Player.Opponent, Player.TargetOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.TargetPlayer, Player.Any -> {
                    context.targets.firstOrNull()?.toEntityId()?.let { listOf(it) } ?: emptyList()
                }
                Player.TriggeringPlayer -> {
                    (context.triggeringPlayerId ?: context.triggeringEntityId)?.let { listOf(it) } ?: emptyList()
                }
                is Player.OwnerOf, is Player.ControllerOf -> {
                    resolvePlayerTarget(effectTarget, context, state)?.let { listOf(it) } ?: emptyList()
                }
                else -> emptyList()
            }
            else -> resolvePlayerTarget(effectTarget, context)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Convert a ChosenTarget to an EntityId.
     */
    fun ChosenTarget.toEntityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }
}
