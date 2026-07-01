package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerProtectionComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Player-level protection (CR 702.16) — consulted by the targeting and damage systems
 * for a player carrying a [PlayerProtectionComponent] (The One Ring's "protection from
 * everything until your next turn").
 *
 * For a player, only the **D**amage and **T**argeting parts of DEBT apply: a protected
 * player can't be the target of, nor be dealt damage by, a source matching one of the
 * player's protection [ProtectionScope]s. This is the single source of truth so the
 * targeting validator, target enumerator, and damage executor stay consistent.
 */
object PlayerProtectionRules {

    /**
     * True if [playerId] has protection from the source [sourceId] (a spell or ability
     * source). [casterId] is the controller of that source, used for the
     * [ProtectionScope.EachOpponent] scope. A null [sourceId] is treated as an unknown
     * source — only [ProtectionScope.Everything] still protects against it.
     */
    fun isProtectedFromSource(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        casterId: EntityId?
    ): Boolean {
        // Player-level protection comes from two sources, unioned:
        //  1. A one-shot [PlayerProtectionComponent] on the player (e.g. The One Ring).
        //  2. Continuous statics ([GrantProtectionToController]) on permanents the player
        //     controls, stamped as [GrantsControllerProtectionComponent] (Absolute Virtue).
        val ownScopes = state.getEntity(playerId)?.get<PlayerProtectionComponent>()?.scopes.orEmpty()
        if (ownScopes.any { scopeMatchesSource(state, playerId, it, sourceId, casterId) }) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            if (container.get<ControllerComponent>()?.playerId != playerId) return@any false
            container.get<GrantsControllerProtectionComponent>()?.scopes
                ?.any { scopeMatchesSource(state, playerId, it, sourceId, casterId) } == true
        }
    }

    private fun scopeMatchesSource(
        state: GameState,
        protectedPlayerId: EntityId,
        scope: ProtectionScope,
        sourceId: EntityId?,
        casterId: EntityId?
    ): Boolean {
        if (scope is ProtectionScope.Everything) return true
        if (sourceId == null) return false

        val projected = state.projectedState
        return when (scope) {
            is ProtectionScope.Color -> scope.color.name in projected.getColors(sourceId)
            is ProtectionScope.Colors -> scope.colors.any { it.name in projected.getColors(sourceId) }
            is ProtectionScope.Subtype ->
                projected.getSubtypes(sourceId).any { it.equals(scope.subtype, ignoreCase = true) }
            is ProtectionScope.Supertype ->
                projected.getSupertypes(sourceId).any { it.equals(scope.supertype, ignoreCase = true) }
            is ProtectionScope.CardType -> projected.hasType(sourceId, scope.cardType.uppercase())
            is ProtectionScope.EachOpponent -> {
                val sourceController = casterId
                    ?: projected.getController(sourceId)
                    ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                sourceController != null && sourceController != protectedPlayerId
            }
            ProtectionScope.Everything -> true
        }
    }
}
