package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.playerWhoMayLookAtFaceDown
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The engine's single source of truth for which card identities a player may see.
 *
 * This covers both identities in hidden zones and face-down objects in otherwise public zones.
 * Client state and decision masking, AI determinization, and Gym observations deliberately share
 * this service. Adding a reveal rule to one without the others would either leak information or
 * hide information a perspective is legally entitled to use. Consumers still own their
 * representations: knowing an identity does not prescribe a client card DTO, a Gym feature vector,
 * or opaque zone slots.
 */
class Visibility(
    private val cardRegistry: CardRegistry,
    private val debugMode: Boolean = false,
) {
    private val conditionEvaluator = ConditionEvaluator()

    fun isZoneVisibleTo(
        state: GameState,
        zoneKey: ZoneKey,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false,
    ): Boolean = when (zoneKey.zoneType) {
        Zone.LIBRARY -> false
        Zone.HAND -> debugMode || (!isSpectator && (
            zoneKey.ownerId == viewingPlayerId ||
                state.actorFor(zoneKey.ownerId) == viewingPlayerId ||
                zoneKey.ownerId in state.teammatesOf(viewingPlayerId) ||
                (zoneKey.ownerId != viewingPlayerId && revealsOpponentHandsTo(state, viewingPlayerId))
            ))
        Zone.SIDEBOARD -> debugMode || (!isSpectator && (
            zoneKey.ownerId == viewingPlayerId ||
                state.actorFor(zoneKey.ownerId) == viewingPlayerId
            ))
        Zone.BATTLEFIELD,
        Zone.GRAVEYARD,
        Zone.STACK,
        Zone.EXILE,
        Zone.COMMAND -> true
    }

    /**
     * Whether [viewingPlayerId] may know the identity of [entityId] in [zoneKey].
     *
     * A hidden zone is not all-or-nothing: [RevealedToComponent] and top-of-library effects can
     * expose one card while the rest stays hidden. Conversely, a public zone does not make the
     * identity under a face-down object public. This query combines those identity facts while
     * leaving ordered hidden-zone structure and consumer-specific presentation to the caller.
     */
    fun isCardIdentityVisibleTo(
        state: GameState,
        zoneKey: ZoneKey,
        entityId: EntityId,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false,
    ): Boolean {
        // Public-zone visibility does not reveal what is underneath a face-down object.
        if (faceDownDisplayName(state, entityId) != null) {
            if (isSpectator) return false
            if (playerWhoMayLookAtFaceDown(state, entityId) == viewingPlayerId) return true
            if (isCardRevealedTo(state, entityId, viewingPlayerId)) return true
            if (zoneKey.zoneType == Zone.BATTLEFIELD &&
                hasLookAtFaceDownCreatures(state, viewingPlayerId)
            ) return true
            if (zoneKey.zoneType == Zone.EXILE &&
                state.hasMayPlayFor(entityId, viewingPlayerId, conditionEvaluator, cardRegistry)
            ) return true
            return false
        }

        if (isZoneVisibleTo(state, zoneKey, viewingPlayerId, isSpectator)) return true

        // A spectator receives public reveals but never a player's private reveal memory.
        val isTopCard = zoneKey.zoneType == Zone.LIBRARY &&
            state.getLibrary(zoneKey.ownerId).firstOrNull() == entityId
        if (isTopCard && revealsTopOfLibraryPublicly(state, zoneKey.ownerId)) return true
        if (isSpectator) return false

        if (isCardRevealedTo(state, entityId, viewingPlayerId)) return true
        return isTopCard && zoneKey.ownerId == viewingPlayerId &&
            hasLookAtTopOfLibrary(state, viewingPlayerId)
    }

    fun isCardRevealedTo(
        state: GameState,
        entityId: EntityId,
        viewingPlayerId: EntityId,
    ): Boolean = state.getEntity(entityId)
        ?.get<RevealedToComponent>()
        ?.isRevealedTo(viewingPlayerId) == true

    fun hasLookAtFaceDownCreatures(state: GameState, playerId: EntityId): Boolean =
        hasActiveStaticAbility(state, playerId) { it is LookAtFaceDownCreatures }

    fun revealsTopOfLibraryPublicly(state: GameState, playerId: EntityId): Boolean =
        hasActiveStaticAbility(state, playerId) {
            it is PlayFromTopOfLibrary || it is RevealTopOfLibrary
        }

    fun hasLookAtTopOfLibrary(state: GameState, playerId: EntityId): Boolean =
        hasActiveStaticAbility(state, playerId) { it is LookAtTopOfLibrary }

    private fun revealsOpponentHandsTo(state: GameState, playerId: EntityId): Boolean =
        hasActiveStaticAbility(state, playerId) { it is OpponentsPlayWithHandsRevealed }

    private fun hasActiveStaticAbility(
        state: GameState,
        playerId: EntityId,
        predicate: (StaticAbility) -> Boolean,
    ): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { ability ->
                    activeStaticAbility(state, ability, entityId, playerId)?.let(predicate) == true
                }
            ) return true
        }
        // Also scan durationally granted statics (e.g. Gwenom, Remorseless grants LookAtTopOfLibrary
        // to itself on attack until end of turn). These live in `grantedStaticAbilities` anchored to
        // the granting permanent, never in `cardDef.script.staticAbilities`. Mirrors the printed-or-
        // granted scan the cast-legality path already uses (CastPermissionUtils.playFromTopAlternativeCost),
        // so visibility and castability stay in lockstep — otherwise the top card is castable but the
        // controller never sees it, so there is nothing to play.
        for (grant in state.grantedStaticAbilities) {
            // Match the cast-legality scan in CastPermissionUtils.playFromTopAlternativeCost (base
            // control) so visibility and castability stay in lockstep.
            val anchor = state.getEntity(grant.entityId) ?: continue
            if (anchor.get<ControllerComponent>()?.playerId != playerId) continue
            if (activeStaticAbility(state, grant.ability, grant.entityId, playerId)?.let(predicate) == true) {
                return true
            }
        }
        return false
    }

    fun activeStaticAbility(
        state: GameState,
        ability: StaticAbility,
        sourceId: EntityId,
        controllerId: EntityId,
    ): StaticAbility? = when (ability) {
        is ConditionalStaticAbility -> {
            val context = EffectContext(sourceId = sourceId, controllerId = controllerId)
            if (conditionEvaluator.evaluate(state, ability.condition, context)) ability.ability else null
        }
        else -> ability
    }
}
