package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityResolutionCountThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.WasDealtDamageThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.TokenReplacementOfferedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.engine.state.components.player.DamageReceivedThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.LifeGainedThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.state.components.player.PutCounterOnCreatureThisTurnComponent
import com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent
import com.wingedsheep.engine.state.components.player.WasDealtCombatDamageThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying

/**
 * Handles all end-of-turn cleanup: discard to hand size, damage removal,
 * expiration of temporary effects, and per-turn tracker resets.
 */
class CleanupPhaseManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler
) {

    /**
     * Perform cleanup step actions.
     * - Discard down to maximum hand size (7)
     * - Remove damage from creatures
     * - Remove "until end of turn" effects
     */
    fun performCleanupStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if player needs to discard
        val handKey = ZoneKey(activePlayer, Zone.HAND)
        val hand = newState.getZone(handKey)
        val maxHandSize = 7
        val cardsToDiscard = hand.size - maxHandSize

        if (cardsToDiscard > 0) {
            // Player needs to discard - create a decision
            events.add(DiscardRequiredEvent(activePlayer, cardsToDiscard))

            // Create the card selection decision
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Discard down to $maxHandSize cards (choose $cardsToDiscard to discard)",
                options = hand,
                minSelections = cardsToDiscard,
                maxSelections = cardsToDiscard,
                ordered = false,
                phase = DecisionPhase.STATE_BASED
            )

            // Push continuation to handle the response
            val continuation = HandSizeDiscardContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // Remove damage from all permanents on the battlefield (Rule 514.2).
        // Includes vehicles that reverted from creature status this turn — their damage
        // (and P/T from the expired floating effect) must be cleared.
        val battlefield = newState.getBattlefield().toSet()
        val permanentsWithDamage = newState.entities.filter { (entityId, container) ->
            entityId in battlefield && container.has<DamageComponent>()
        }.keys

        for (entityId in permanentsWithDamage) {
            newState = newState.updateEntity(entityId) { it.without<DamageComponent>() }
        }

        // Remove MustAttackThisTurnComponent from all creatures (Walking Desecration effect)
        val creaturesWithMustAttack = newState.entities.filter { (_, container) ->
            container.has<MustAttackThisTurnComponent>()
        }.keys
        for (entityId in creaturesWithMustAttack) {
            newState = newState.updateEntity(entityId) { it.without<MustAttackThisTurnComponent>() }
        }

        // No priority during cleanup (normally)
        newState = newState.copy(priorityPlayerId = null)

        return ExecutionResult.success(newState, events)
    }

    /**
     * Expire UntilYourNextTurn floating effects after the untap step of the
     * controller's next turn. The effect needs to be active during the untap
     * step (to prevent untapping), then removed afterward.
     */
    fun expireUntilYourNextTurnEffects(state: GameState, activePlayer: EntityId): GameState {
        val remainingFloating = state.floatingEffects.filter { floatingEffect ->
            !(floatingEffect.duration is Duration.UntilYourNextTurn &&
                floatingEffect.controllerId == activePlayer)
        }
        val remainingGlobal = state.globalGrantedTriggeredAbilities.filter { grant ->
            !(grant.duration is Duration.UntilYourNextTurn &&
                grant.controllerId == activePlayer)
        }
        val floatingChanged = remainingFloating.size != state.floatingEffects.size
        val globalChanged = remainingGlobal.size != state.globalGrantedTriggeredAbilities.size
        return if (floatingChanged || globalChanged) {
            state.copy(
                floatingEffects = if (floatingChanged) remainingFloating else state.floatingEffects,
                globalGrantedTriggeredAbilities = if (globalChanged) remainingGlobal else state.globalGrantedTriggeredAbilities
            )
        } else {
            state
        }
    }

    /**
     * Expire UntilAfterAffectedControllersNextUntap floating effects after the untap step.
     * These expire when any of the affected entities are controlled by the active player,
     * meaning the affected creature's controller just had their untap step.
     */
    fun expireAffectedControllersNextUntapEffects(state: GameState, activePlayer: EntityId): GameState {
        val projected = state.projectedState
        val remaining = state.floatingEffects.filter { floatingEffect ->
            if (floatingEffect.duration !is Duration.UntilAfterAffectedControllersNextUntap) return@filter true
            // Expire if any affected entity is controlled by the active player
            val affectedByActivePlayer = floatingEffect.effect.affectedEntities.any { entityId ->
                projected.getController(entityId) == activePlayer
            }
            !affectedByActivePlayer
        }
        return if (remaining.size != state.floatingEffects.size) {
            state.copy(floatingEffects = remaining)
        } else {
            state
        }
    }

    /**
     * Remove WhileSourceTapped floating effects whose source is no longer tapped.
     * Called during untap step to prevent stale effects from accumulating.
     */
    fun cleanupWhileSourceTappedEffects(state: GameState): GameState {
        val remaining = state.floatingEffects.filter { floatingEffect ->
            if (floatingEffect.duration is Duration.WhileSourceTapped) {
                val sourceId = floatingEffect.sourceId
                sourceId != null && state.getBattlefield().contains(sourceId) &&
                    state.getEntity(sourceId)?.has<TappedComponent>() == true
            } else {
                true
            }
        }
        return if (remaining.size != state.floatingEffects.size) {
            state.copy(floatingEffects = remaining)
        } else {
            state
        }
    }

    /**
     * Check if any permanent on the battlefield has the PreventManaPoolEmptying static ability.
     * Used for cards like Upwelling: "Players don't lose unspent mana as steps and phases end."
     */
    private fun isManaPoolEmptyingPrevented(state: GameState): Boolean {
        val registry = cardRegistry
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = registry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventManaPoolEmptying }) {
                return true
            }
        }
        return false
    }

    /**
     * Clean up end-of-turn effects.
     *
     * This is called at the end of each turn and handles:
     * 1. Expiring "until end of turn" floating effects (Giant Growth, etc.)
     * 2. Emptying mana pools
     * 3. Resetting per-turn trackers (land drops)
     */
    fun cleanupEndOfTurn(state: GameState): GameState {
        var newState = state

        // 1. Expire floating effects with EndOfTurn duration
        val remainingEffects = newState.floatingEffects.filter { floatingEffect ->
            when (floatingEffect.duration) {
                is Duration.EndOfTurn -> false  // Remove it
                is Duration.EndOfCombat -> false  // Should already be removed, but clean up
                is Duration.UntilYourNextTurn -> true  // Keep until that player's next turn
                is Duration.UntilYourNextUpkeep -> true  // Keep until upkeep
                is Duration.Permanent -> true  // Never expires
                is Duration.WhileSourceOnBattlefield -> {
                    // Keep if source is still on battlefield
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId)
                }
                is Duration.WhileSourceTapped -> {
                    // Keep if source is still on battlefield AND tapped
                    val sourceId = floatingEffect.sourceId
                    sourceId != null && newState.getBattlefield().contains(sourceId) &&
                        newState.getEntity(sourceId)?.has<TappedComponent>() == true
                }
                is Duration.UntilAfterAffectedControllersNextUntap -> true  // Expires after affected entity's controller's untap
                is Duration.UntilPhase -> true  // Handle in phase transitions
                is Duration.UntilCondition -> true  // Handle condition checking elsewhere
            }
        }
        newState = newState.copy(floatingEffects = remainingEffects)

        // 2. Empty mana pools for all players (unless prevented by a static ability like Upwelling)
        if (!isManaPoolEmptyingPrevented(newState)) {
            for (playerId in newState.turnOrder) {
                newState = newState.updateEntity(playerId) { container ->
                    val manaPool = container.get<ManaPoolComponent>()
                    if (manaPool != null && !manaPool.isEmpty) {
                        container.with(manaPool.empty())
                    } else {
                        container
                    }
                }
            }
        }

        // 3. Reset per-turn trackers (land drops reset at start of turn, but clean up here too)
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                val landDrops = container.get<LandDropsComponent>()
                if (landDrops != null) {
                    container.with(landDrops.reset())
                } else {
                    container
                }
            }
        }

        // 4. Remove any unconsumed additional combat phase components, temporary player shroud, and damage tracking
        for (playerId in newState.turnOrder) {
            newState = newState.updateEntity(playerId) { container ->
                var result = container
                if (result.has<AdditionalCombatPhasesComponent>()) {
                    result = result.without<AdditionalCombatPhasesComponent>()
                }
                val shroud = result.get<PlayerShroudComponent>()
                if (shroud?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerShroudComponent>()
                }
                val hexproof = result.get<PlayerHexproofComponent>()
                if (hexproof?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<PlayerHexproofComponent>()
                }
                val cantCast = result.get<CantCastSpellsComponent>()
                if (cantCast?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<CantCastSpellsComponent>()
                }
                val graveyardForage = result.get<MayCastCreaturesFromGraveyardWithForageComponent>()
                if (graveyardForage?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<MayCastCreaturesFromGraveyardWithForageComponent>()
                }
                val damageBonus = result.get<DamageBonusComponent>()
                if (damageBonus?.removeOn == PlayerEffectRemoval.EndOfTurn) {
                    result = result.without<DamageBonusComponent>()
                }
                if (result.has<DamageReceivedThisTurnComponent>()) {
                    result = result.without<DamageReceivedThisTurnComponent>()
                }
                if (result.has<NonTokenCreaturesDiedThisTurnComponent>()) {
                    result = result.without<NonTokenCreaturesDiedThisTurnComponent>()
                }
                if (result.has<CreaturesDiedThisTurnComponent>()) {
                    result = result.without<CreaturesDiedThisTurnComponent>()
                }
                if (result.has<OpponentCreaturesExiledThisTurnComponent>()) {
                    result = result.without<OpponentCreaturesExiledThisTurnComponent>()
                }
                if (result.has<LifeGainedThisTurnComponent>()) {
                    result = result.without<LifeGainedThisTurnComponent>()
                }
                if (result.has<LifeLostThisTurnComponent>()) {
                    result = result.without<LifeLostThisTurnComponent>()
                }
                if (result.has<CardsLeftGraveyardThisTurnComponent>()) {
                    result = result.without<CardsLeftGraveyardThisTurnComponent>()
                }
                if (result.has<SacrificedFoodThisTurnComponent>()) {
                    result = result.without<SacrificedFoodThisTurnComponent>()
                }
                if (result.has<PutCounterOnCreatureThisTurnComponent>()) {
                    result = result.without<PutCounterOnCreatureThisTurnComponent>()
                }
                if (result.has<WasDealtCombatDamageThisTurnComponent>()) {
                    result = result.without<WasDealtCombatDamageThisTurnComponent>()
                }
                result
            }
        }

        // 5. Clear per-turn ability activation tracking, damage source tracking, and attack tracking
        for ((entityId, container) in newState.entities) {
            var needsUpdate = false
            if (container.has<AbilityActivatedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<DamageDealtToCreaturesThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<TargetedByControllerThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<PlayerAttackedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<GraveyardPlayPermissionUsedComponent>()) {
                needsUpdate = true
            }
            if (container.has<TriggeredAbilityFiredThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<AbilityResolutionCountThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<TokenReplacementOfferedThisTurnComponent>()) {
                needsUpdate = true
            }
            if (container.has<WasDealtDamageThisTurnComponent>()) {
                needsUpdate = true
            }
            if (needsUpdate) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<AbilityActivatedThisTurnComponent>()
                        .without<DamageDealtToCreaturesThisTurnComponent>()
                        .without<TargetedByControllerThisTurnComponent>()
                        .without<PlayerAttackedThisTurnComponent>()
                        .without<GraveyardPlayPermissionUsedComponent>()
                        .without<TriggeredAbilityFiredThisTurnComponent>()
                        .without<AbilityResolutionCountThisTurnComponent>()
                        .without<TokenReplacementOfferedThisTurnComponent>()
                        .without<WasDealtDamageThisTurnComponent>()
                }
            }
        }

        // 6. Expire granted triggered abilities with EndOfTurn duration
        if (newState.grantedTriggeredAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedTriggeredAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedTriggeredAbilities = remainingGrants)
        }

        // 7. Expire granted activated abilities with EndOfTurn duration
        if (newState.grantedActivatedAbilities.isNotEmpty()) {
            val remainingGrants = newState.grantedActivatedAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(grantedActivatedAbilities = remainingGrants)
        }

        // 8. Expire global granted triggered abilities with EndOfTurn duration
        if (newState.globalGrantedTriggeredAbilities.isNotEmpty()) {
            val remainingGrants = newState.globalGrantedTriggeredAbilities.filter { grant ->
                grant.duration !is Duration.EndOfTurn
            }
            newState = newState.copy(globalGrantedTriggeredAbilities = remainingGrants)
        }

        // 9. Remove MayPlayFromExileComponent and PlayWithoutPayingCostComponent (expire at end of turn)
        // Skip permanent ones (used by "for as long as it remains exiled" effects)
        // For expiresAfterTurn: keep alive until that turn number's end step
        for ((entityId, container) in newState.entities) {
            val mayPlay = container.get<MayPlayFromExileComponent>()
            val playFree = container.get<PlayWithoutPayingCostComponent>()
            val removeMayPlay = mayPlay != null && !mayPlay.permanent && when {
                mayPlay.expiresAfterTurn != null -> newState.turnNumber >= mayPlay.expiresAfterTurn
                else -> true // default: expire at end of this turn
            }
            val removePlayFree = playFree != null && !playFree.permanent
            val removeLinkedExileUsed = container.get<com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent>() != null
            if (removeMayPlay || removePlayFree || removeLinkedExileUsed) {
                newState = newState.updateEntity(entityId) { c ->
                    var updated = c
                    if (removeMayPlay) updated = updated.without<MayPlayFromExileComponent>()
                    if (removePlayFree) updated = updated.without<PlayWithoutPayingCostComponent>()
                    if (removeLinkedExileUsed) updated = updated.without<com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent>()
                    updated
                }
            }
        }

        // 10. Expire event-based delayed triggered abilities with EndOfTurn expiry
        // (e.g., Long River Lurker's "whenever that creature deals combat damage this turn").
        if (newState.delayedTriggers.isNotEmpty()) {
            val remainingDelayed = newState.delayedTriggers.filter { delayed ->
                delayed.expiry !is com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry.EndOfTurn
            }
            if (remainingDelayed.size != newState.delayedTriggers.size) {
                newState = newState.copy(delayedTriggers = remainingDelayed)
            }
        }

        return newState
    }
}
