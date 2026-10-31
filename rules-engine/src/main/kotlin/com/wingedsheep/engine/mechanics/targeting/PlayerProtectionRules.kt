package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerProtectionComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofFromComponent
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
                    val scope = ProtectionKeywords.resolveChosen(it.scope, container) ?: return@any false
                    ControllerGrants.isActive(state, entityId, it.condition, predicateEvaluator = predicateEvaluator) &&
                        scopeMatchesSource(state, playerId, scope, sourceId, casterId, granter = container)
                } == true
        }
    }

    /**
     * True if [playerId] has "hexproof from [quality]" (CR 702.11d, [PlayerHexproofFromComponent])
     * against the source [sourceId] controlled by [casterId] — Veil of Summer. Hexproof never
     * stops the player's own spells and abilities, and an unknown source matches no quality.
     */
    fun hasHexproofFromSource(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        casterId: EntityId?,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val grants = state.getEntity(playerId)?.get<PlayerHexproofFromComponent>()?.grants ?: return false
        if (casterId == null || casterId == playerId || sourceId == null) return false
        return grants.any { scopeMatchesSource(state, playerId, it.scope, sourceId, casterId) } &&
            // CR 702.11e: "as though it didn't have hexproof" reaches "hexproof from [quality]" too.
            !HexproofSuppression.isPlayerSuppressedForCaster(state, playerId, casterId, predicateEvaluator = predicateEvaluator)
    }

    /**
     * Whether [sourceId] (controlled by [casterId]) can't target [playerId] for a source-dependent
     * reason: protection (CR 702.16) or hexproof from a quality (CR 702.11d). Full hexproof and
     * shroud don't depend on the source and are checked separately. Targeting reads this; damage
     * and attachment read [isProtectedFromSource], since hexproof only stops targeting.
     */
    fun blocksTargeting(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        casterId: EntityId?,
        predicateEvaluator: PredicateEvaluator
    ): Boolean = isProtectedFromSource(state, playerId, sourceId, casterId, predicateEvaluator) ||
        hasHexproofFromSource(state, playerId, sourceId, casterId, predicateEvaluator)

    private fun scopeMatchesSource(
        state: GameState,
        protectedPlayerId: EntityId,
        scope: ProtectionScope,
        sourceId: EntityId?,
        casterId: EntityId?,
        granter: ComponentContainer? = null
    ): Boolean {
        if (scope is ProtectionScope.Everything) return true
        if (sourceId == null) return false

        val projected = state.projectedState
        // Attachment choices inspect an Aura before entry; spells likewise need their current
        // off-battlefield characteristics rather than an absent battlefield projection.
        val card = state.getEntity(sourceId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
        val onBattlefield = sourceId in state.getBattlefield()
        // Projected on the battlefield, the card's own (or an off-battlefield SetColors') elsewhere.
        fun colors(): Set<String> = if (card == null && !onBattlefield) emptySet() else projected.colorsOf(sourceId)
        fun hasCardType(cardType: String): Boolean = if (onBattlefield) projected.hasType(sourceId, cardType.uppercase())
            else card?.typeLine?.cardTypes?.any { it.name.equals(cardType, ignoreCase = true) } == true
        return when (scope) {
            is ProtectionScope.Color -> scope.color.name in colors()
            is ProtectionScope.Colors -> scope.colors.any { it.name in colors() }
            is ProtectionScope.NonColor -> scope.color.name !in colors()
            ProtectionScope.Multicolored -> colors().size >= 2
            ProtectionScope.Monocolored -> colors().size == 1
            is ProtectionScope.Subtype -> (if (onBattlefield)
                projected.getSubtypes(sourceId).any { it.equals(scope.subtype, ignoreCase = true) }
                else card?.typeLine?.subtypes?.any { it.value.equals(scope.subtype, ignoreCase = true) } == true) &&
                (scope.cardType?.let { hasCardType(it) } ?: true)
            is ProtectionScope.Supertype -> (if (onBattlefield)
                projected.getSupertypes(sourceId).any { it.equals(scope.supertype, ignoreCase = true) }
                else card?.typeLine?.supertypes?.any { it.name.equals(scope.supertype, ignoreCase = true) } == true) &&
                (scope.cardType?.let { hasCardType(it) } ?: true)
            is ProtectionScope.CardType -> hasCardType(scope.cardType)
            is ProtectionScope.EachOpponent -> {
                val sourceController = casterId
                    ?: projected.getController(sourceId)
                    ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                sourceController != null && sourceController != protectedPlayerId
            }
            ProtectionScope.Everything -> true
            ProtectionScope.Spells -> SourceKindProtection.isSpell(state, sourceId)
            ProtectionScope.PermanentsCastThisTurn -> SourceKindProtection.isPermanentCastThisTurn(state, sourceId)
            ProtectionScope.RingBearers -> SourceKindProtection.isRingBearer(state, sourceId)
            is ProtectionScope.ManaValueParity ->
                SourceKindProtection.manaValue(state, sourceId)?.let(scope.parity::matches) == true
            is ProtectionScope.ManaValueOtherThan ->
                SourceKindProtection.manaValue(state, sourceId)?.let { it != scope.value } == true
            // An ability kind is a property of the targeting spell-or-ability, not of the source
            // object this reading is given; no player-protection grant names one.
            ProtectionScope.ActivatedAbilities, ProtectionScope.TriggeredAbilities -> false
            // Bound to a granting permanent's choice — resolved by the caller; unbound it protects
            // from nothing (a one-shot grant has no permanent to read the choice from).
            is ProtectionScope.ChosenCardType -> false
            // The name the granting permanent chose (Runed Halo). Matches the source's current
            // name — projected on the battlefield (a copy or name-changing effect), printed
            // elsewhere — and never a face-down source, which has no name (CR 708.2a).
            is ProtectionScope.ChosenCardName -> {
                val chosen = ProtectionKeywords.chosenCardName(granter, scope.slot) ?: return false
                val faceDown = if (onBattlefield) projected.isFaceDown(sourceId)
                    else state.getEntity(sourceId)?.has<FaceDownComponent>() == true
                val name = (if (onBattlefield) projected.getName(sourceId) else null) ?: card?.name
                !faceDown && name.equals(chosen, ignoreCase = true)
            }
        }
    }
}
