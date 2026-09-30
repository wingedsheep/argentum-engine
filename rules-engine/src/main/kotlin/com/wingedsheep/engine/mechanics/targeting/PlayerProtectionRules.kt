package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.ControllerGrants
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
 * A protected player cannot be enchanted, targeted, or dealt damage by a matching source.
 * Attachment choice, state-based checks, targeting, and damage share this reading.
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
        casterId: EntityId?,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        // Player-level protection comes from two sources, unioned:
        //  1. A one-shot [PlayerProtectionComponent] on the player (e.g. The One Ring).
        //  2. Continuous statics ([GrantProtectionToController]) on permanents the player
        //     controls, stamped as [GrantsControllerProtectionComponent] (Absolute Virtue).
        val ownScopes = state.getEntity(playerId)?.get<PlayerProtectionComponent>()?.scopes.orEmpty()
        if (ownScopes.any { scopeMatchesSource(state, playerId, it, sourceId, casterId) }) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            // Projected controller: a stolen Absolute Virtue protects its thief, not the player it
            // was taken from — see [ControllerGrants.granterController].
            if (ControllerGrants.granterController(state, entityId) != playerId) return@any false
            container.get<GrantsControllerProtectionComponent>()?.grants
                // Each scope carries its own "as long as …" gate, re-evaluated here on every read
                // because the marker was stamped once, on entry — see [ControllerGrantMarker].
                ?.any {
                    ControllerGrants.isActive(state, entityId, it.condition, predicateEvaluator = predicateEvaluator) &&
                        scopeMatchesSource(state, playerId, it.scope, sourceId, casterId)
                } == true
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
        // Attachment choices inspect an Aura before entry; spells likewise need their current
        // off-battlefield characteristics rather than an absent battlefield projection.
        val card = state.getEntity(sourceId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
        val onBattlefield = sourceId in state.getBattlefield()
        return when (scope) {
            is ProtectionScope.Color -> if (onBattlefield) scope.color.name in projected.getColors(sourceId)
                else scope.color in card?.colors.orEmpty()
            is ProtectionScope.Colors -> scope.colors.any { color ->
                if (onBattlefield) color.name in projected.getColors(sourceId) else color in card?.colors.orEmpty()
            }
            is ProtectionScope.NonColor -> if (onBattlefield) scope.color.name !in projected.getColors(sourceId)
                else scope.color !in card?.colors.orEmpty()
            ProtectionScope.Multicolored -> if (onBattlefield) projected.getColors(sourceId).size >= 2
                else card?.colors.orEmpty().size >= 2
            is ProtectionScope.Subtype -> if (onBattlefield)
                projected.getSubtypes(sourceId).any { it.equals(scope.subtype, ignoreCase = true) }
                else card?.typeLine?.subtypes?.any { it.value.equals(scope.subtype, ignoreCase = true) } == true
            is ProtectionScope.Supertype -> if (onBattlefield)
                projected.getSupertypes(sourceId).any { it.equals(scope.supertype, ignoreCase = true) }
                else card?.typeLine?.supertypes?.any { it.name.equals(scope.supertype, ignoreCase = true) } == true
            is ProtectionScope.CardType -> if (onBattlefield) projected.hasType(sourceId, scope.cardType.uppercase())
                else card?.typeLine?.cardTypes?.any { it.name.equals(scope.cardType, ignoreCase = true) } == true
            is ProtectionScope.EachOpponent -> {
                val sourceController = casterId
                    ?: projected.getController(sourceId)
                    ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                sourceController != null && sourceController != protectedPlayerId
            }
            ProtectionScope.Everything -> true
            ProtectionScope.Spells -> SourceKindProtection.isSpell(state, sourceId)
            ProtectionScope.PermanentsCastThisTurn -> SourceKindProtection.isPermanentCastThisTurn(state, sourceId)
            // An ability kind is a property of the targeting spell-or-ability, not of the source
            // object this reading is given; no player-protection grant names one.
            ProtectionScope.ActivatedAbilities, ProtectionScope.TriggeredAbilities -> false
        }
    }
}
