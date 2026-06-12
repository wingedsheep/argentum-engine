package com.wingedsheep.engine.state.permissions

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Add a permission to the game state's [GameState.mayPlayPermissions] list.
 */
fun GameState.addMayPlayPermission(permission: MayPlayPermission): GameState =
    copy(mayPlayPermissions = mayPlayPermissions + permission)

/**
 * Remove a permission by id. No-op if absent.
 */
fun GameState.removeMayPlayPermission(id: EntityId): GameState =
    copy(mayPlayPermissions = mayPlayPermissions.filterNot { it.id == id })

/**
 * Drop [cardId] from every permission's `cardIds`. Used when a card moves to a zone where
 * the permission is no longer meaningful (e.g., on resolve), to keep the list compact.
 *
 * Multi-card permissions (e.g., Etali / Narset / Mind's Desire grant a single permission
 * spanning all exiled cards) MUST keep authorising the remaining cards after one of them
 * is cast. Removing the whole permission would silently revoke "any number of spells"
 * after the first cast.
 *
 * Permissions whose `cardIds` becomes empty are dropped entirely.
 */
fun GameState.removeMayPlayPermissionsForCard(cardId: EntityId): GameState =
    copy(
        mayPlayPermissions = mayPlayPermissions.mapNotNull { permission ->
            if (cardId !in permission.cardIds) permission
            else {
                val remaining = permission.cardIds - cardId
                if (remaining.isEmpty()) null else permission.copy(cardIds = remaining)
            }
        }
    )

/**
 * Find every active permission that authorizes [playerId] to play [cardId], with the gate
 * condition currently open. Multiple permissions can stack (e.g., a conditional grant and
 * an unconditional one); each read site picks how to combine them.
 */
fun GameState.activeMayPlayFor(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
): List<MayPlayPermission> = mayPlayPermissions.filter { permission ->
    permission.controllerId == playerId &&
        cardId in permission.cardIds &&
        permission.gateOpen(this, cardId, conditionEvaluator)
}

/**
 * True when at least one active permission authorizes [playerId] to play [cardId].
 */
fun GameState.hasMayPlayFor(
    cardId: EntityId,
    playerId: EntityId,
    conditionEvaluator: ConditionEvaluator,
): Boolean = activeMayPlayFor(cardId, playerId, conditionEvaluator).isNotEmpty()

/**
 * Re-evaluate the optional condition gate at the read site. Conditions on permissions
 * (Possibility Technician's "you control a Kavu") must be checked at every query, not
 * just at the moment the permission was created.
 *
 * Supported condition shapes: ambient state (`Exists`, `Compare`, life totals, hand sizes,
 * …) and anything keyed off [EffectContext.controllerId]. Source-referencing conditions
 * (`SourceHas*`, `SourceIs*`) work only when [MayPlayPermission.sourceId] is set; otherwise
 * the source falls back to [cardId] and source-keyed conditions misfire silently. If a card
 * needs a source-keyed gate, set `sourceId` on the permission at grant time.
 */
fun MayPlayPermission.gateOpen(
    state: GameState,
    cardId: EntityId,
    conditionEvaluator: ConditionEvaluator,
): Boolean {
    val condition = condition ?: return true
    val context = EffectContext(
        sourceId = sourceId ?: cardId,
        controllerId = controllerId,
    )
    return conditionEvaluator.evaluate(state, condition, context)
}
