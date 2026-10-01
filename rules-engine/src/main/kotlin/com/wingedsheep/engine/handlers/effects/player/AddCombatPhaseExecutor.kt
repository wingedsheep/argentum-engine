package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.state.components.player.QueuedPhase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCombatPhaseEffect
import kotlin.reflect.KClass

/**
 * Queue a [kind] phase (optionally carrying an [attackerRestriction] for a COMBAT phase) on the
 * active player's [AdditionalPhasesComponent] (CR 500.8). The queue is drained by the TurnManager
 * after the postcombat main phase. Shared by every add-a-phase executor so they stay one insertion.
 *
 * CR 500.8: when several phases are added at the same point, the most recently created one happens
 * first — so a new creation goes to the *front* of the queue. Phases one effect creates in sequence
 * ("an additional combat phase followed by an additional main phase") are one creation, identified
 * by [context]'s source and the current timestamp, and keep their order: the new entry goes after
 * the leading run of entries from that same creation.
 */
internal fun GameState.queueAdditionalPhase(
    player: EntityId,
    kind: ExtraPhaseKind,
    context: EffectContext,
    attackerRestriction: GameObjectFilter? = null
): GameState {
    val existing = getEntity(player)?.get<AdditionalPhasesComponent>()?.phases.orEmpty()
    val entry = QueuedPhase(kind, attackerRestriction, createdBy = context.sourceId, createdAt = timestamp)
    val insertAt = existing.indexOfFirst { !it.sameCreationAs(entry) }.let { if (it < 0) existing.size else it }
    val newPhases = existing.take(insertAt) + entry + existing.drop(insertAt)
    return updateEntity(player) { it.with(AdditionalPhasesComponent(newPhases)) }
}

/**
 * Executor for [AddCombatPhaseEffect] — "After this phase, there is an additional combat phase."
 *
 * Queues a single COMBAT phase on the active player; it is inserted after the postcombat main phase
 * and is NOT followed by an extra main phase (that requires composing with [AddMainPhaseEffect]).
 */
class AddCombatPhaseExecutor : EffectExecutor<AddCombatPhaseEffect> {

    override val effectType: KClass<AddCombatPhaseEffect> = AddCombatPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: AddCombatPhaseEffect,
        context: EffectContext
    ): EffectResult {
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player for AddCombatPhaseEffect")
        return EffectResult.success(
            state.queueAdditionalPhase(activePlayer, ExtraPhaseKind.COMBAT, context, effect.attackerRestriction)
        )
    }
}
