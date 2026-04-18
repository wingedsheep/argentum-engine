package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Validates that chosen targets match their target requirements.
 *
 * This class checks if a target:
 * - Is the correct type (creature, permanent, player, etc.)
 * - Matches any filters (attacking, nonblack, you control, etc.)
 *
 * Uses PredicateEvaluator to match unified filters against game state.
 */
class TargetValidator {
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Validate all targets for a spell/ability against their requirements.
     *
     * @param state The current game state
     * @param targets The chosen targets
     * @param requirements The target requirements from the card definition
     * @param casterId The player casting the spell
     * @param sourceColors Colors of the source spell/ability (for protection checks)
     * @return Error message if any target is invalid, null if all targets are valid
     */
    fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        requirements: List<TargetRequirement>,
        casterId: EntityId,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        sourceId: EntityId? = null
    ): String? {
        // Use the game state for validation
        // StateProjector is used for P/T checks to account for continuous effects

        // Match targets to requirements (assuming targets are in order of requirements)
        for ((index, requirement) in requirements.withIndex()) {
            // Get targets for this requirement (handle multi-target requirements)
            val targetCount = requirement.count
            val startIdx = requirements.take(index).sumOf { it.count }
            val endIdx = startIdx + targetCount
            val targetsForReq = targets.subList(
                startIdx.coerceAtMost(targets.size),
                endIdx.coerceAtMost(targets.size)
            )

            // Check minimum targets
            if (targetsForReq.size < requirement.effectiveMinCount) {
                return "Not enough targets for ${requirement.description}"
            }

            // Validate each target against the requirement
            for (target in targetsForReq) {
                val error = validateSingleTarget(state, target, requirement, casterId, sourceColors, sourceSubtypes, sourceId)
                if (error != null) return error
            }
        }

        return null
    }

    /**
     * Validate a single target against a requirement.
     */
    private fun validateSingleTarget(
        state: GameState,
        target: ChosenTarget,
        requirement: TargetRequirement,
        casterId: EntityId,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        sourceId: EntityId? = null
    ): String? {
        val error = when (requirement) {
            is TargetPlayer -> validatePlayerTarget(state, target, casterId)
            is TargetOpponent -> validateOpponentTarget(state, target, casterId)
            is AnyTarget -> validateAnyTarget(state, target, casterId)
            is TargetCreatureOrPlayer -> validateCreatureOrPlayerTarget(state, target, casterId)
            is TargetOpponentOrPlaneswalker -> validateOpponentOrPlaneswalkerTarget(state, target, casterId)
            is TargetPlayerOrPlaneswalker -> validatePlayerOrPlaneswalkerTarget(state, target, casterId)
            is TargetCreatureOrPlaneswalker -> validateCreatureOrPlaneswalkerTarget(state, target)
            is TargetSpellOrPermanent -> validateSpellOrPermanentTarget(state, target, requirement, casterId, sourceId)
            is TargetObject -> validateObjectTarget(state, target, requirement.filter, casterId, sourceId)
            is TargetOther -> validateSingleTarget(state, target, requirement.baseRequirement, casterId, sourceColors, sourceSubtypes, sourceId)
        }
        if (error != null) return error

        // Check hexproof and shroud on permanent targets (Rule 702.11, 702.18)
        val hexproofShroudError = checkHexproofAndShroud(state, target, casterId)
        if (hexproofShroudError != null) return hexproofShroudError

        // Check hexproof from color (Rule 702.11b)
        val hexproofError = checkHexproofFromColor(state, target, casterId, sourceColors)
        if (hexproofError != null) return hexproofError

        // Check protection from color and creature subtype (Rule 702.16)
        return checkProtection(state, target, sourceColors, sourceSubtypes)
    }

    /**
     * Check if a permanent target has hexproof or shroud.
     * Hexproof prevents opponents from targeting; shroud prevents all targeting.
     */
    private fun checkHexproofAndShroud(
        state: GameState,
        target: ChosenTarget,
        casterId: EntityId
    ): String? {
        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            else -> return null
        }

        if (entityId !in state.getBattlefield()) return null

        val projected = state.projectedState
        val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

        if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
            val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
            return "$cardName has shroud"
        }
        if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != casterId) {
            val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
            return "$cardName has hexproof"
        }
        return null
    }

    /**
     * Check if a target has hexproof from any of the source's colors.
     * "Hexproof from [color]" prevents opponents from targeting with spells/abilities of that color.
     * Returns an error message if hexproof blocks this targeting, null otherwise.
     */
    private fun checkHexproofFromColor(
        state: GameState,
        target: ChosenTarget,
        casterId: EntityId,
        sourceColors: Set<Color>
    ): String? {
        if (sourceColors.isEmpty()) return null

        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            else -> return null
        }

        // Only check permanents on the battlefield
        if (entityId !in state.getBattlefield()) return null

        // Hexproof from color only blocks opponents — owner can still target
        val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
        if (entityController == casterId) return null

        val projected = state.projectedState
        for (color in sourceColors) {
            if (projected.hasKeyword(entityId, "HEXPROOF_FROM_${color.name}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has hexproof from ${color.displayName.lowercase()}"
            }
        }
        return null
    }

    /**
     * Check if a target has protection from any of the source's colors or creature subtypes.
     * Returns an error message if the target is protected, null otherwise.
     */
    private fun checkProtection(
        state: GameState,
        target: ChosenTarget,
        sourceColors: Set<Color>,
        sourceSubtypes: Set<String> = emptySet()
    ): String? {
        if (sourceColors.isEmpty() && sourceSubtypes.isEmpty()) return null

        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Player -> return null  // Protection on players is handled separately
            else -> return null
        }

        // Only check permanents on the battlefield
        if (entityId !in state.getBattlefield()) return null

        val projected = state.projectedState
        for (color in sourceColors) {
            if (projected.hasKeyword(entityId, "PROTECTION_FROM_${color.name}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has protection from ${color.displayName.lowercase()}"
            }
        }
        for (subtype in sourceSubtypes) {
            if (projected.hasKeyword(entityId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has protection from ${subtype.lowercase()}s"
            }
        }
        return null
    }

    private fun validatePermanentTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId,
        sourceId: EntityId? = null
    ): String? {
        if (target !is ChosenTarget.Permanent) {
            return "Target must be a permanent"
        }

        state.getEntity(target.entityId)
            ?: return "Target not found"

        // Check if target is on the battlefield
        if (target.entityId !in state.getBattlefield()) {
            return "Target must be on the battlefield"
        }

        // Use unified filter with projection (face-down creatures have CMC 0 per Rule 707.2)
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = casterId, sourceId = sourceId)
        val matches = predicateEvaluator.matchesWithProjection(state, projected, target.entityId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    private fun validatePlayerTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        if (target !is ChosenTarget.Player) {
            return "Target must be a player"
        }
        if (!state.hasEntity(target.playerId)) {
            return "Target player not found"
        }
        if (playerHasShroud(state, target.playerId)) {
            return "Target player has shroud"
        }
        if (playerHasHexproofAgainst(state, target.playerId, casterId)) {
            return "Target player has hexproof"
        }
        return null
    }

    private fun validateOpponentTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        if (target !is ChosenTarget.Player) {
            return "Target must be a player"
        }
        if (!state.hasEntity(target.playerId)) {
            return "Target player not found"
        }
        if (target.playerId == casterId) {
            return "Target must be an opponent"
        }
        if (playerHasShroud(state, target.playerId)) {
            return "Target player has shroud"
        }
        if (playerHasHexproof(state, target.playerId)) {
            return "Target player has hexproof"
        }
        return null
    }

    private fun validateAnyTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found"
                else if (playerHasShroud(state, target.playerId)) "Target player has shroud"
                else if (playerHasHexproofAgainst(state, target.playerId, casterId)) "Target player has hexproof"
                else null
            }
            is ChosenTarget.Permanent -> {
                if (target.entityId !in state.getBattlefield()) "Target not on battlefield" else null
            }
            else -> "Invalid target type"
        }
    }

    private fun validateCreatureOrPlayerTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found"
                else if (playerHasShroud(state, target.playerId)) "Target player has shroud"
                else if (playerHasHexproofAgainst(state, target.playerId, casterId)) "Target player has hexproof"
                else null
            }
            is ChosenTarget.Permanent -> {
                val container = state.getEntity(target.entityId)
                    ?: return "Target not found"
                val cardComponent = container.get<CardComponent>()
                    ?: return "Target is not a card"
                // Face-down permanents are always creatures (Rule 707.2)
                if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) {
                    return "Target must be a creature or player"
                }
                if (target.entityId !in state.getBattlefield()) {
                    return "Target must be on the battlefield"
                }
                null
            }
            else -> "Target must be a creature or player"
        }
    }

    private fun validateOpponentOrPlaneswalkerTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found"
                else if (target.playerId == casterId) "Target must be an opponent"
                else if (playerHasShroud(state, target.playerId)) "Target player has shroud"
                else if (playerHasHexproof(state, target.playerId)) "Target player has hexproof"
                else null
            }
            is ChosenTarget.Permanent -> {
                val container = state.getEntity(target.entityId)
                    ?: return "Target not found"
                val cardComponent = container.get<CardComponent>()
                    ?: return "Target is not a card"
                if (CardType.PLANESWALKER !in cardComponent.typeLine.cardTypes) {
                    return "Target must be an opponent or planeswalker"
                }
                if (target.entityId !in state.getBattlefield()) {
                    return "Target must be on the battlefield"
                }
                null
            }
            else -> "Target must be an opponent or planeswalker"
        }
    }

    private fun validatePlayerOrPlaneswalkerTarget(state: GameState, target: ChosenTarget, casterId: EntityId): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found"
                else if (playerHasShroud(state, target.playerId)) "Target player has shroud"
                else if (playerHasHexproofAgainst(state, target.playerId, casterId)) "Target player has hexproof"
                else null
            }
            is ChosenTarget.Permanent -> {
                val container = state.getEntity(target.entityId)
                    ?: return "Target not found"
                val cardComponent = container.get<CardComponent>()
                    ?: return "Target is not a card"
                if (CardType.PLANESWALKER !in cardComponent.typeLine.cardTypes) {
                    return "Target must be a player or planeswalker"
                }
                if (target.entityId !in state.getBattlefield()) {
                    return "Target must be on the battlefield"
                }
                null
            }
            else -> "Target must be a player or planeswalker"
        }
    }

    private fun validateCreatureOrPlaneswalkerTarget(state: GameState, target: ChosenTarget): String? {
        if (target !is ChosenTarget.Permanent) {
            return "Target must be a creature or planeswalker"
        }
        val container = state.getEntity(target.entityId)
            ?: return "Target not found"
        val cardComponent = container.get<CardComponent>()
            ?: return "Target is not a card"
        val isPlaneswalker = CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
        // Face-down permanents are always creatures (Rule 707.2)
        val isFaceDown = container.has<FaceDownComponent>()
        if (!cardComponent.typeLine.isCreature && !isPlaneswalker && !isFaceDown) {
            return "Target must be a creature or planeswalker"
        }
        if (target.entityId !in state.getBattlefield()) {
            return "Target must be on the battlefield"
        }
        return null
    }

    private fun validateGraveyardTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId,
        sourceId: EntityId? = null
    ): String? {
        if (target !is ChosenTarget.Card) {
            return "Target must be a card in a graveyard"
        }
        if (target.zone != Zone.GRAVEYARD) {
            return "Target must be in a graveyard"
        }

        val zoneKey = ZoneKey(target.ownerId, Zone.GRAVEYARD)
        if (target.cardId !in state.getZone(zoneKey)) {
            return "Target not found in graveyard"
        }

        if (filter.excludeSelf && sourceId != null && target.cardId == sourceId) {
            return "Target must be another card"
        }

        // Use unified filter - OwnedByYou predicate handles "your graveyard" restriction
        val predicateContext = PredicateContext(controllerId = casterId, ownerId = target.ownerId)
        val matches = predicateEvaluator.matches(state, target.cardId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    private fun validateSpellTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId
    ): String? {
        if (target !is ChosenTarget.Spell) {
            return "Target must be a spell on the stack"
        }
        if (target.spellEntityId !in state.stack) {
            return "Target spell not on the stack"
        }

        // Use unified filter with projected state (face-down spells need projection to be seen as creatures)
        val predicateContext = PredicateContext(controllerId = casterId)
        val matches = predicateEvaluator.matchesWithProjection(state, state.projectedState, target.spellEntityId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    /**
     * Validate a target for TargetObject, dispatching based on the filter's zone.
     */
    private fun validateObjectTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId,
        sourceId: EntityId? = null
    ): String? {
        return when (filter.zone) {
            Zone.GRAVEYARD -> validateGraveyardTarget(state, target, filter, casterId, sourceId)
            Zone.BATTLEFIELD -> validatePermanentTarget(state, target, filter, casterId, sourceId)
            Zone.STACK -> validateSpellTarget(state, target, filter, casterId)
            else -> validateCardInZoneTarget(state, target, filter, casterId)
        }
    }

    /**
     * Validate a target for TargetSpellOrPermanent.
     * Accepts either a spell on the stack or a permanent on the battlefield.
     * If [requirement.permanentFilter] is set, permanent targets must also match it
     * (e.g., "target spell or creature" restricts the permanent side to creatures).
     */
    private fun validateSpellOrPermanentTarget(
        state: GameState,
        target: ChosenTarget,
        requirement: TargetSpellOrPermanent,
        casterId: EntityId,
        sourceId: EntityId?
    ): String? {
        return when (target) {
            is ChosenTarget.Permanent -> {
                state.getEntity(target.entityId)
                    ?: return "Target not found"
                if (target.entityId !in state.getBattlefield()) {
                    return "Target must be on the battlefield or on the stack"
                }
                val filter = requirement.permanentFilter
                if (filter != null) {
                    val projected = state.projectedState
                    val context = PredicateContext(controllerId = casterId, sourceId = sourceId)
                    if (!predicateEvaluator.matchesWithProjection(state, projected, target.entityId, filter, context)) {
                        return "Target does not match ${filter.description}"
                    }
                }
                null
            }
            is ChosenTarget.Spell -> {
                if (target.spellEntityId !in state.stack) {
                    return "Target spell not on the stack"
                }
                null
            }
            else -> "Target must be a spell or permanent"
        }
    }

    /**
     * Validate a card target in a non-battlefield, non-stack zone (hand, library, exile).
     */
    private fun validateCardInZoneTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId
    ): String? {
        if (target !is ChosenTarget.Card) {
            return "Target must be a card"
        }

        val expectedZone = filter.zone

        if (target.zone != expectedZone) {
            return "Target must be in ${filter.zone.displayName}"
        }

        val zoneKey = ZoneKey(target.ownerId, expectedZone)
        if (target.cardId !in state.getZone(zoneKey)) {
            return "Target not found in ${filter.zone.displayName}"
        }

        val predicateContext = PredicateContext(controllerId = casterId, ownerId = target.ownerId)
        val matches = predicateEvaluator.matches(state, target.cardId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    /**
     * Check if a player has shroud (e.g., from True Believer's "You have shroud"
     * or Gilded Light's "You gain shroud until end of turn").
     */
    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerShroudComponent>() == true) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    private fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerHexproofComponent>() == true) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerHexproofComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    private fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, casterId: EntityId): Boolean {
        return playerId != casterId && playerHasHexproof(state, playerId)
    }
}
