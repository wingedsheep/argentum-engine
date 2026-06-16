package com.wingedsheep.engine.handlers.effects.permanent.control

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GainControlByMostEffect
import com.wingedsheep.sdk.scripting.effects.PlayerRankMetric
import kotlin.reflect.KClass

/**
 * Executor for [GainControlByMostEffect].
 *
 * Ranks players by the effect's [PlayerRankMetric] and gives the player with strictly
 * more than every other player control of the target permanent. On a tie for the highest
 * value, nothing happens (Ghazbán Ogre's "if a player has more life than each other
 * player" / Thoughtbound Primoc's "if a player controls more Wizards than each other
 * player" intervening condition).
 */
class GainControlByMostExecutor : EffectExecutor<GainControlByMostEffect> {

    override val effectType: KClass<GainControlByMostEffect> = GainControlByMostEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlByMostEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")

        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // Rank value per player, per the effect's metric.
        val valueByPlayer = state.turnOrder.associateWith { playerId ->
            metricValue(state, effect.metric, playerId)
        }

        val maxValue = valueByPlayer.values.maxOrNull() ?: return EffectResult.success(state)
        // No creatures of the subtype means no one qualifies. (Life ties are caught below.)
        if (effect.metric is PlayerRankMetric.CreaturesOfSubtype && maxValue == 0) {
            return EffectResult.success(state)
        }

        val playersWithMax = valueByPlayer.filter { it.value == maxValue }
        // Strictly more than each other player — a tie means no control change.
        if (playersWithMax.size != 1) return EffectResult.success(state)

        val newControllerId = playersWithMax.keys.first()

        // Use projected controller so floating-effect-based control changes are respected.
        val currentControllerId = state.projectedState.getController(targetId)
            ?: targetContainer.get<ControllerComponent>()?.playerId
        if (currentControllerId == newControllerId) return EffectResult.success(state)

        // "Other players can't gain control of it" (Guardian Beast): a different player can't take
        // control of the permanent.
        if (state.projectedState.hasKeyword(targetId, AbilityFlag.CANT_GAIN_CONTROL)) {
            return EffectResult.success(state)
        }

        // Remove any previous Layer.CONTROL floating effects from the same source on the same target.
        val filteredEffects = state.floatingEffects.filter { floating ->
            !(floating.sourceId == context.sourceId &&
                floating.effect.layer == Layer.CONTROL &&
                targetId in floating.effect.affectedEntities)
        }

        val controlContext = context.copy(controllerId = newControllerId)
        // Rule 302.6: the new controller hasn't controlled this permanent since their most recent turn began.
        val newState = state.copy(floatingEffects = filteredEffects)
            .addFloatingEffect(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(newControllerId),
                affectedEntities = setOf(targetId),
                duration = Duration.Permanent,
                context = controlContext
            )
            .updateEntity(targetId) { it.with(SummoningSicknessComponent) }
            .let { clearRingBearerOnControlChange(it, targetId, newControllerId) }

        val events = listOf(
            ControlChangedEvent(
                permanentId = targetId,
                permanentName = cardComponent.name,
                oldControllerId = currentControllerId ?: context.controllerId,
                newControllerId = newControllerId
            )
        )

        return EffectResult.success(newState, events)
    }

    private fun metricValue(state: GameState, metric: PlayerRankMetric, playerId: EntityId): Int =
        when (metric) {
            is PlayerRankMetric.LifeTotal ->
                state.lifeTotal(playerId) // CR 810.9a — team's shared total
            is PlayerRankMetric.CreaturesOfSubtype ->
                // Projected state so type-changing effects (a permanent animated/typeshifted
                // into the subtype) and stolen control are counted correctly.
                state.projectedState.getBattlefieldControlledBy(playerId).count { entityId ->
                    state.projectedState.hasSubtype(entityId, metric.subtype.value)
                }
        }
}
