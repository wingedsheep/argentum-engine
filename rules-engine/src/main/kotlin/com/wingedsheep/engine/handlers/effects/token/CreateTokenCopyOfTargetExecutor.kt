package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateTokenCopyOfTargetEffect.
 *
 * Creates N token copies of a targeted permanent (resolved via EffectTarget).
 * Used for "Create X tokens that are copies of target token you control."
 */
class CreateTokenCopyOfTargetExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<CreateTokenCopyOfTargetEffect> {

    override val effectType: KClass<CreateTokenCopyOfTargetEffect> = CreateTokenCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: CreateTokenCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val targetCard = targetContainer.get<CardComponent>()
            ?: return EffectResult.success(state)

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) return EffectResult.success(state)

        val controllerId = context.controllerId
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        repeat(count) {
            val tokenId = EntityId.generate()
            val op = effect.overridePower
            val ot = effect.overrideToughness
            val overrideStats = if (op != null && ot != null) {
                CreatureStats(op, ot)
            } else null
            val tokenCard = targetCard.copy(
                ownerId = controllerId,
                baseStats = overrideStats ?: targetCard.baseStats,
                baseKeywords = targetCard.baseKeywords + effect.addedKeywords
            )

            val components = mutableListOf<Component>(
                tokenCard,
                TokenComponent,
                ControllerComponent(controllerId),
                SummoningSicknessComponent,
                EnteredThisTurnComponent
            )
            if (effect.tapped) {
                components.add(TappedComponent)
            }
            if (effect.attacking) {
                val defenderId = newState.getOpponent(controllerId)
                if (defenderId != null) {
                    components.add(AttackingComponent(defenderId))
                }
            }

            var container = ComponentContainer.of(*components.toTypedArray())

            if (staticAbilityHandler != null) {
                container = staticAbilityHandler.addContinuousEffectComponent(container)
                container = staticAbilityHandler.addReplacementEffectComponent(container)
            }

            newState = newState.withEntity(tokenId, container)
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)

            for (ability in effect.triggeredAbilities) {
                val grant = GrantedTriggeredAbility(
                    entityId = tokenId,
                    ability = ability,
                    duration = Duration.Permanent
                )
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities + grant
                )
            }

            events.add(
                ZoneChangeEvent(
                    entityId = tokenId,
                    entityName = tokenCard.name,
                    fromZone = null,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        }

        return EffectResult.success(newState, events)
    }
}
