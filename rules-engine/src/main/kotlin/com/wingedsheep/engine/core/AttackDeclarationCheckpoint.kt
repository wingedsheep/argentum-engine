package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Only the declaration-tap changes, never a second GameState with hidden hands or libraries.
 * Independent mana actions taken in the payment window survive cancellation. Restoring an old
 * component is safe only while the original object and that component's provisional value remain.
 */
@Serializable
data class AttackDeclarationCheckpoint(
    val id: String,
    val changes: List<DeclarationComponentChange>,
    val events: List<GameEvent>,
) {
    fun restore(state: GameState): GameState {
        var result = state
        for (change in changes) {
            if (!result.isCurrentObject(change.objectRef)) continue
            result = result.updateEntity(change.objectRef.entityId) { current ->
                val components = current.components.toMutableMap()
                for (key in change.before.components.keys + change.after.components.keys) {
                    if (components[key] != change.after.components[key]) continue
                    val previous = change.before.components[key]
                    if (previous == null) components.remove(key) else components[key] = previous
                }
                ComponentContainer(components)
            }
        }
        return result.copy(pendingTriggers = result.pendingTriggers.filter { it.attackDeclarationId != id })
    }

    companion object {
        fun capture(id: String, before: GameState, after: GameState, attackers: Set<EntityId>, events: List<GameEvent>) =
            AttackDeclarationCheckpoint(id, attackers.mapNotNull { attacker ->
                val original = before.getEntity(attacker) ?: return@mapNotNull null
                val changed = after.getEntity(attacker) ?: return@mapNotNull null
                val ref = before.objectRef(attacker) ?: return@mapNotNull null
                val keys = (original.components.keys + changed.components.keys)
                    .filter { original.components[it] != changed.components[it] }.toSet()
                if (keys.isEmpty()) return@mapNotNull null
                DeclarationComponentChange(ref,
                    ComponentContainer(original.components.filterKeys { it in keys }),
                    ComponentContainer(changed.components.filterKeys { it in keys }))
            }, events)
    }
}

@Serializable
data class DeclarationComponentChange(
    val objectRef: ObjectRef,
    val before: ComponentContainer,
    val after: ComponentContainer,
)

/** Find the still-open declaration through nested mana-payment questions. */
internal fun GameState.attackDeclarationCheckpoint(): AttackDeclarationCheckpoint? =
    continuationStack.asReversed().filterIsInstance<Suspension>().firstNotNullOfOrNull { frame ->
        when (val answer = frame.answer) {
            is AttackEnlistSelectionContinuation -> answer.rollback
            is AttackTaxManaSelectionContinuation -> answer.rollback
            else -> null
        }
    }
