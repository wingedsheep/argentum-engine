package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EachOpponentDiscardsEffect
import com.wingedsheep.sdk.scripting.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReplaceNextDrawWithEffect].
 *
 * Creates a floating replacement-effect shield that intercepts the controller's
 * next card draw this turn, replacing it with the stored [ReplaceNextDrawWithEffect.replacementEffect].
 *
 * Supported replacement effects and their internal shield types:
 * - [GainLifeEffect]                         → [SerializableModification.ReplaceDrawWithLifeGain]
 * - [EachPlayerReturnsPermanentToHandEffect] → [SerializableModification.ReplaceDrawWithBounce]
 * - [EachOpponentDiscardsEffect]             → [SerializableModification.ReplaceDrawWithDiscard]
 * - [DealDamageEffect]                       → [SerializableModification.ReplaceDrawWithDamage]
 * - [CreateTokenEffect]                      → [SerializableModification.ReplaceDrawWithToken]
 */
class ReplaceNextDrawWithExecutor : EffectExecutor<ReplaceNextDrawWithEffect> {

    override val effectType: KClass<ReplaceNextDrawWithEffect> = ReplaceNextDrawWithEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithEffect,
        context: EffectContext
    ): ExecutionResult {
        val modification: SerializableModification = when (val inner = effect.replacementEffect) {
            is GainLifeEffect -> {
                val amount = (inner.amount as? DynamicAmount.Fixed)?.amount
                    ?: return ExecutionResult.error(state, "ReplaceNextDraw: GainLife must have a fixed amount")
                SerializableModification.ReplaceDrawWithLifeGain(amount)
            }
            is EachPlayerReturnsPermanentToHandEffect ->
                SerializableModification.ReplaceDrawWithBounce
            is EachOpponentDiscardsEffect ->
                SerializableModification.ReplaceDrawWithDiscard
            is DealDamageEffect -> {
                val amount = (inner.amount as? DynamicAmount.Fixed)?.amount
                    ?: return ExecutionResult.error(state, "ReplaceNextDraw: DealDamage must have a fixed amount")
                val targetId = with(EffectExecutorUtils) { context.targets.firstOrNull()?.toEntityId() }
                    ?: return ExecutionResult.error(state, "ReplaceNextDraw: no target for damage replacement")
                SerializableModification.ReplaceDrawWithDamage(amount, targetId)
            }
            is CreateTokenEffect ->
                SerializableModification.ReplaceDrawWithToken(
                    power = inner.power,
                    toughness = inner.toughness,
                    colors = inner.colors,
                    creatureTypes = inner.creatureTypes
                )
            else -> return ExecutionResult.error(
                state,
                "ReplaceNextDraw: unsupported replacement effect type ${inner::class.simpleName}"
            )
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = modification,
                affectedEntities = setOf(context.controllerId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        return ExecutionResult.success(state.copy(floatingEffects = state.floatingEffects + floatingEffect))
    }
}
