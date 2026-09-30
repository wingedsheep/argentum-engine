package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantPlayerActionEffect
import kotlinx.serialization.Serializable

@Serializable
data class PlayerActionPermission(
    val id: String,
    val playerId: EntityId,
    val action: GrantPlayerActionEffect,
    val context: EffectContext,
    /** A captured null means the reference was already lost, never recapture it. */
    val references: Map<EntityId, ObjectRef?>,
) {
    fun executionContext(state: GameState): EffectContext {
        fun current(target: ChosenTarget?): ChosenTarget? {
            val id = target?.entityId() ?: return null
            return target.takeIf { id in state.turnOrder || references[id]?.let(state::isCurrentObject) == true }
        }
        val aligned = if (context.alignedTargets.isNotEmpty()) context.alignedTargets else context.targets
        val refreshed = aligned.map(::current)
        return context.withCurrentObjectReferences(state).copy(
            targets = refreshed.filterNotNull(), alignedTargets = refreshed,
            pipeline = context.pipeline.copy(namedTargets = context.pipeline.namedTargets.mapNotNull { (key, target) -> current(target)?.let { key to it } }.toMap()),
        )
    }
}

internal fun ChosenTarget.entityId(): EntityId = when (this) {
    is ChosenTarget.Player -> playerId
    is ChosenTarget.Permanent -> entityId
    is ChosenTarget.Card -> cardId
    is ChosenTarget.Spell -> spellEntityId
}

/** Combat declarations happen before the priority window in their step. */
internal fun canTakePlayerActionAtPriority(state: GameState, playerId: EntityId): Boolean {
    if (!state.hasPriority(playerId) || state.step == com.wingedsheep.sdk.core.Step.UNTAP) return false
    if (state.step == com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS && state.isActiveTurnFor(playerId) &&
        state.getEntity(playerId)?.has<com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent>() != true) return false
    if (state.step == com.wingedsheep.sdk.core.Step.DECLARE_BLOCKERS &&
        com.wingedsheep.engine.mechanics.combat.CombatDefenders.isDefendingPlayer(state, playerId) &&
        state.getEntity(playerId)?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true) return false
    return true
}
