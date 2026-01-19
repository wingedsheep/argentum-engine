package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.AddCountersEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.MustBeBlockedEffect
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.CountersComponent
import com.wingedsheep.rulesengine.ecs.components.MustBeBlockedComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.layers.Layer
import com.wingedsheep.rulesengine.ecs.layers.Modification
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.layers.ModifierFilter
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

// Helper to resolve single permanent target from context or effect definition
private fun resolveTargetId(effectTarget: EffectTarget, context: ExecutionContext): EntityId? {
    return when (effectTarget) {
        is EffectTarget.Self -> context.sourceId
        is EffectTarget.ContextTarget -> {
            val targets = context.getTargetsForIndex(effectTarget.index)
            targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()?.entityId
        }
        else -> context.targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()?.entityId
    }
}

/**
 * Handler for TapUntapEffect.
 */
class TapUntapHandler : BaseEffectHandler<TapUntapEffect>() {
    override val effectClass: KClass<TapUntapEffect> = TapUntapEffect::class

    override fun execute(
        state: GameState,
        effect: TapUntapEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetId = resolveTargetId(effect.target, context) ?: return noOp(state)

        val newState = if (effect.tap) {
            state.updateEntity(targetId) { c -> c.with(TappedComponent) }
        } else {
            state.updateEntity(targetId) { c -> c.without<TappedComponent>() }
        }

        val cardName = state.getEntity(targetId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        val event = if (effect.tap) {
            EffectEvent.PermanentTapped(targetId, cardName)
        } else {
            EffectEvent.PermanentUntapped(targetId, cardName)
        }

        return result(newState, event)
    }
}

/**
 * Handler for ModifyStatsEffect.
 */
class ModifyStatsHandler : BaseEffectHandler<ModifyStatsEffect>() {
    override val effectClass: KClass<ModifyStatsEffect> = ModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: ModifyStatsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // FIX: Use resolveTargetId to handle EffectTarget.Self correctly
        val targetId = resolveTargetId(effect.target, context) ?: return noOp(state)

        return ExecutionResult(
            state = state,
            events = listOf(EffectEvent.StatsModified(targetId, effect.powerModifier, effect.toughnessModifier)),
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
                        filter = ModifierFilter.Specific(targetId)
                    )
                )
            } else emptyList()
        )
    }
}

/**
 * Handler for AddCountersEffect.
 */
class AddCountersHandler : BaseEffectHandler<AddCountersEffect>() {
    override val effectClass: KClass<AddCountersEffect> = AddCountersEffect::class

    override fun execute(
        state: GameState,
        effect: AddCountersEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Use resolveTargetId for consistency
        val targetId = resolveTargetId(effect.target, context) ?: return noOp(state)

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(" ", "_")
                    .replace("+1/+1", "PLUS_ONE_PLUS_ONE")
                    .replace("-1/-1", "MINUS_ONE_MINUS_ONE")
            )
        } catch (e: IllegalArgumentException) {
            return noOp(state)
        }

        val container = state.getEntity(targetId) ?: return noOp(state)
        val counters = container.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { c ->
            c.with(counters.add(counterType, effect.count))
        }

        return result(newState, EffectEvent.CountersAdded(targetId, counterType.name, effect.count))
    }
}

/**
 * Handler for MustBeBlockedEffect.
 */
class MustBeBlockedHandler : BaseEffectHandler<MustBeBlockedEffect>() {
    override val effectClass: KClass<MustBeBlockedEffect> = MustBeBlockedEffect::class

    override fun execute(
        state: GameState,
        effect: MustBeBlockedEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetId = resolveTargetId(effect.target, context) ?: return noOp(state)

        val newState = state.updateEntity(targetId) { c ->
            c.with(MustBeBlockedComponent)
        }

        return ExecutionResult(newState)
    }
}

/**
 * Handler for GrantKeywordUntilEndOfTurnEffect.
 */
class GrantKeywordUntilEndOfTurnHandler : BaseEffectHandler<GrantKeywordUntilEndOfTurnEffect>() {
    override val effectClass: KClass<GrantKeywordUntilEndOfTurnEffect> = GrantKeywordUntilEndOfTurnEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordUntilEndOfTurnEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetId = resolveTargetId(effect.target, context) ?: return noOp(state)

        return ExecutionResult(
            state = state,
            events = listOf(EffectEvent.KeywordGranted(targetId, effect.keyword)),
            temporaryModifiers = listOf(
                Modifier(
                    layer = Layer.ABILITY,
                    sourceId = context.sourceId,
                    timestamp = Modifier.nextTimestamp(),
                    modification = Modification.AddKeyword(effect.keyword),
                    filter = ModifierFilter.Specific(targetId)
                )
            )
        )
    }
}
