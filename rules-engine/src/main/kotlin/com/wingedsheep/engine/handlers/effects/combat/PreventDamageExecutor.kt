package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DeflectDamageSourceChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PreventDamageFromChosenSourceContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Unified executor for PreventDamageEffect.
 *
 * Dispatches to the appropriate floating effect creation based on the effect's parameters:
 * - amount-based vs prevent-all
 * - combat-only vs all-damage
 * - direction (to target, from target, both)
 * - source filter (any, attacking, chosen source, chosen creature type, group)
 * - reflect (Deflecting Palm)
 */
class PreventDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<PreventDamageEffect> {

    override val effectType: KClass<PreventDamageEffect> = PreventDamageEffect::class

    override fun execute(
        state: GameState,
        effect: PreventDamageEffect,
        context: EffectContext
    ): EffectResult {
        // Handle ChosenSource filter: requires player decision before creating shield
        if (effect.sourceFilter is PreventionSourceFilter.ChosenSource) {
            return handleChosenSource(state, effect, context)
        }

        // Handle ChosenCreatureType filter: reads from source component
        if (effect.sourceFilter is PreventionSourceFilter.ChosenCreatureType) {
            return handleChosenCreatureType(state, effect, context)
        }

        // All other variants: create floating effect directly
        return createFloatingEffect(state, effect, context)
    }

    private fun handleChosenSource(
        state: GameState,
        effect: PreventDamageEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId

        // Gather all possible damage sources: permanents + spells on stack
        val sourceIds = mutableListOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            if (state.getEntity(entityId)?.get<CardComponent>() != null) {
                sourceIds.add(entityId)
            }
        }
        for (entityId in state.stack) {
            if (state.getEntity(entityId)?.get<CardComponent>() != null) {
                sourceIds.add(entityId)
            }
        }

        if (sourceIds.isEmpty()) return EffectResult.success(state)

        val decisionId = UUID.randomUUID().toString()
        val decisionContext = DecisionContext(
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        )

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a source of damage",
            context = decisionContext,
            options = sourceIds,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        if (effect.reflect) {
            // Deflecting Palm path: reflect damage back to source's controller
            val continuation = DeflectDamageSourceChoiceContinuation(
                decisionId = decisionId,
                controllerId = controllerId,
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            )
            val newState = state.withPendingDecision(decision).pushContinuation(continuation)
            return EffectResult.paused(newState, decision)
        } else {
            // Prevention-only path: prevent N damage from chosen source
            val targetId = context.resolveTarget(effect.target)
                ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect with ChosenSource")
            val amount = effect.amount?.let { amountEvaluator.evaluate(state, it, context) } ?: 0
            if (amount <= 0 && effect.amount != null) return EffectResult.success(state)

            val continuation = PreventDamageFromChosenSourceContinuation(
                decisionId = decisionId,
                controllerId = controllerId,
                targetId = targetId,
                amount = amount,
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            )
            val newState = state.withPendingDecision(decision).pushContinuation(continuation)
            return EffectResult.paused(newState, decision)
        }
    }

    private fun handleChosenCreatureType(
        state: GameState,
        effect: PreventDamageEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for PreventDamageEffect with ChosenCreatureType")

        val sourceEntity = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Source entity not found: $sourceId")

        val chosenType = sourceEntity.get<ChosenCreatureTypeComponent>()?.creatureType
            ?: return EffectResult.error(state, "No chosen creature type on source: $sourceId")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventNextDamageFromCreatureType(chosenType),
            affectedEntities = setOf(context.controllerId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }

    private fun createFloatingEffect(
        state: GameState,
        effect: PreventDamageEffect,
        context: EffectContext
    ): EffectResult {
        // Determine affected entities
        val affectedEntities: Set<EntityId>
        val modification: SerializableModification

        when {
            // Global combat damage prevention (no specific target)
            effect.scope == PreventionScope.CombatOnly &&
            effect.direction == PreventionDirection.ToTarget &&
            effect.sourceFilter is PreventionSourceFilter.AnySource &&
            effect.amount == null -> {
                // "Prevent all combat damage this turn"
                affectedEntities = emptySet()
                modification = SerializableModification.PreventAllCombatDamage
            }

            // Prevent combat damage from a group (e.g., non-Soldier creatures)
            effect.sourceFilter is PreventionSourceFilter.FromGroup -> {
                val fromGroup = effect.sourceFilter as PreventionSourceFilter.FromGroup
                affectedEntities = emptySet()
                modification = SerializableModification.PreventCombatDamageFromGroup(
                    filter = fromGroup.filter.baseFilter
                )
            }

            // Prevent damage from attacking creatures
            effect.sourceFilter is PreventionSourceFilter.AttackingCreatures -> {
                affectedEntities = setOf(context.controllerId)
                modification = SerializableModification.PreventDamageFromAttackingCreatures
            }

            // Bidirectional combat damage prevention (to and by target)
            effect.direction == PreventionDirection.Both -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.success(state)
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventCombatDamageToAndBy
            }

            // Prevent all damage FROM target (silencing)
            effect.direction == PreventionDirection.FromTarget &&
            effect.amount == null -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.success(state)
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventAllDamageDealtBy
            }

            // Amount-based prevention (prevent next N damage to target)
            effect.amount != null -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                val effectAmount = effect.amount!!
                val amount = amountEvaluator.evaluate(state, effectAmount, context)
                if (amount <= 0) return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventNextDamage(amount)
            }

            // Prevent all damage TO target.
            effect.direction == PreventionDirection.ToTarget &&
            effect.scope == PreventionScope.AllDamage &&
            effect.sourceFilter is PreventionSourceFilter.AnySource -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventAllDamageTo
            }

            else -> {
                return EffectResult.error(state, "Unsupported PreventDamageEffect configuration")
            }
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = modification,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context,
            timestamp = state.timestamp
        )

        return EffectResult.success(newState)
    }
}
