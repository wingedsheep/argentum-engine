package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.*

/**
 * Validates that chosen targets match their target requirements.
 *
 * This class checks if a target:
 * - Is the correct type (creature, permanent, player, etc.)
 * - Matches any filters (attacking, nonblack, you control, etc.)
 *
 * Uses StateProjector to get accurate P/T values with continuous effects applied.
 */
class TargetValidator(
    private val cardRegistry: CardRegistry? = null,
    private val stateProjector: StateProjector = StateProjector()
) {
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Validate all targets for a spell/ability against their requirements.
     *
     * @param state The current game state
     * @param targets The chosen targets
     * @param requirements The target requirements from the card definition
     * @param casterId The player casting the spell
     * @return Error message if any target is invalid, null if all targets are valid
     */
    fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        requirements: List<TargetRequirement>,
        casterId: EntityId
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
                val error = validateSingleTarget(state, target, requirement, casterId)
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
        casterId: EntityId
    ): String? {
        return when (requirement) {
            is TargetCreature -> validateCreatureTarget(state, target, requirement.filter, requirement.unifiedFilter, casterId)
            is TargetPermanent -> validatePermanentTarget(state, target, requirement.filter, casterId)
            is TargetPlayer -> validatePlayerTarget(state, target)
            is TargetOpponent -> validateOpponentTarget(state, target, casterId)
            is AnyTarget -> validateAnyTarget(state, target)
            is TargetCreatureOrPlayer -> validateCreatureOrPlayerTarget(state, target)
            is TargetCreatureOrPlaneswalker -> validateCreatureOrPlaneswalkerTarget(state, target)
            is TargetCardInGraveyard -> validateGraveyardTarget(state, target, requirement.filter, requirement.unifiedFilter, casterId)
            is TargetSpell -> validateSpellTarget(state, target)
            is TargetOther -> validateSingleTarget(state, target, requirement.baseRequirement, casterId)
        }
    }

    private fun validateCreatureTarget(
        state: GameState,
        target: ChosenTarget,
        filter: CreatureTargetFilter,
        unifiedFilter: TargetFilter?,
        casterId: EntityId
    ): String? {
        if (target !is ChosenTarget.Permanent) {
            return "Target must be a permanent"
        }

        val container = state.getEntity(target.entityId)
            ?: return "Target not found"

        val cardComponent = container.get<CardComponent>()
            ?: return "Target is not a card"

        if (!cardComponent.typeLine.isCreature) {
            return "Target must be a creature"
        }

        // Check if target is on the battlefield
        if (target.entityId !in state.getBattlefield()) {
            return "Target must be on the battlefield"
        }

        // Prefer unified filter when available
        if (unifiedFilter != null) {
            val predicateContext = PredicateContext(controllerId = casterId)
            val matches = predicateEvaluator.matches(state, target.entityId, unifiedFilter.baseFilter, predicateContext)
            if (!matches) {
                return "Target does not match filter: ${unifiedFilter.description}"
            }
            return null
        }

        // Fall back to legacy filter
        return validateCreatureFilter(state, target.entityId, cardComponent, container, filter, casterId)
    }

    private fun validateCreatureFilter(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        container: com.wingedsheep.engine.state.ComponentContainer,
        filter: CreatureTargetFilter,
        casterId: EntityId
    ): String? {
        return when (filter) {
            is CreatureTargetFilter.Any -> null

            is CreatureTargetFilter.YouControl -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller != casterId) "Target must be a creature you control" else null
            }

            is CreatureTargetFilter.OpponentControls -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller == casterId) "Target must be a creature an opponent controls" else null
            }

            is CreatureTargetFilter.Attacking -> {
                if (!container.has<AttackingComponent>()) "Target must be an attacking creature" else null
            }

            is CreatureTargetFilter.Blocking -> {
                if (!container.has<BlockingComponent>()) "Target must be a blocking creature" else null
            }

            is CreatureTargetFilter.Tapped -> {
                if (!container.has<TappedComponent>()) "Target must be a tapped creature" else null
            }

            is CreatureTargetFilter.Untapped -> {
                if (container.has<TappedComponent>()) "Target must be an untapped creature" else null
            }

            is CreatureTargetFilter.WithKeyword -> {
                if (!cardComponent.baseKeywords.contains(filter.keyword)) {
                    "Target must have ${filter.keyword.name.lowercase().replace('_', ' ')}"
                } else null
            }

            is CreatureTargetFilter.WithoutKeyword -> {
                if (cardComponent.baseKeywords.contains(filter.keyword)) {
                    "Target must not have ${filter.keyword.name.lowercase().replace('_', ' ')}"
                } else null
            }

            is CreatureTargetFilter.WithColor -> {
                if (!cardComponent.colors.contains(filter.color)) {
                    "Target must be ${filter.color.displayName.lowercase()}"
                } else null
            }

            is CreatureTargetFilter.NotColor -> {
                if (cardComponent.colors.contains(filter.color)) {
                    "Target must not be ${filter.color.displayName.lowercase()}"
                } else null
            }

            is CreatureTargetFilter.WithPowerAtMost -> {
                // Use projected power to account for continuous effects
                val power = stateProjector.getProjectedPower(state, entityId)
                if (power > filter.maxPower) {
                    "Target must have power ${filter.maxPower} or less"
                } else null
            }

            is CreatureTargetFilter.WithPowerAtLeast -> {
                // Use projected power to account for continuous effects
                val power = stateProjector.getProjectedPower(state, entityId)
                if (power < filter.minPower) {
                    "Target must have power ${filter.minPower} or greater"
                } else null
            }

            is CreatureTargetFilter.WithToughnessAtMost -> {
                // Use projected toughness to account for continuous effects
                val toughness = stateProjector.getProjectedToughness(state, entityId)
                if (toughness > filter.maxToughness) {
                    "Target must have toughness ${filter.maxToughness} or less"
                } else null
            }

            is CreatureTargetFilter.WithSubtype -> {
                if (!cardComponent.typeLine.hasSubtype(filter.subtype)) {
                    "Target must be a ${filter.subtype.value}"
                } else null
            }

            is CreatureTargetFilter.And -> {
                // All filters must pass
                for (subFilter in filter.filters) {
                    val error = validateCreatureFilter(state, entityId, cardComponent, container, subFilter, casterId)
                    if (error != null) return error
                }
                null
            }

            is CreatureTargetFilter.AttackingYouControl -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller != casterId) return "Target must be a creature you control"
                if (!container.has<AttackingComponent>()) return "Target must be an attacking creature"
                null
            }

            is CreatureTargetFilter.AttackingWithSubtypeYouControl -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller != casterId) return "Target must be a creature you control"
                if (!container.has<AttackingComponent>()) return "Target must be an attacking creature"
                if (!cardComponent.typeLine.hasSubtype(filter.subtype)) {
                    return "Target must be a ${filter.subtype.value}"
                }
                null
            }

            is CreatureTargetFilter.WithManaValueAtMost -> {
                if (cardComponent.manaValue > filter.maxManaValue) {
                    "Target must have mana value ${filter.maxManaValue} or less"
                } else null
            }

            is CreatureTargetFilter.WithManaValueAtLeast -> {
                if (cardComponent.manaValue < filter.minManaValue) {
                    "Target must have mana value ${filter.minManaValue} or greater"
                } else null
            }
        }
    }

    private fun validatePermanentTarget(
        state: GameState,
        target: ChosenTarget,
        filter: PermanentTargetFilter,
        casterId: EntityId
    ): String? {
        if (target !is ChosenTarget.Permanent) {
            return "Target must be a permanent"
        }

        val container = state.getEntity(target.entityId)
            ?: return "Target not found"

        val cardComponent = container.get<CardComponent>()
            ?: return "Target is not a card"

        // Check if target is on the battlefield
        if (target.entityId !in state.getBattlefield()) {
            return "Target must be on the battlefield"
        }

        return validatePermanentFilter(cardComponent, container, filter, casterId)
    }

    private fun validatePermanentFilter(
        cardComponent: CardComponent,
        container: com.wingedsheep.engine.state.ComponentContainer,
        filter: PermanentTargetFilter,
        casterId: EntityId
    ): String? {
        return when (filter) {
            is PermanentTargetFilter.Any -> null
            is PermanentTargetFilter.YouControl -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller != casterId) "Target must be a permanent you control" else null
            }
            is PermanentTargetFilter.OpponentControls -> {
                val controller = container.get<ControllerComponent>()?.playerId
                if (controller == casterId) "Target must be a permanent an opponent controls" else null
            }
            is PermanentTargetFilter.Creature -> {
                if (!cardComponent.typeLine.isCreature) "Target must be a creature" else null
            }
            is PermanentTargetFilter.Artifact -> {
                if (!cardComponent.typeLine.isArtifact) "Target must be an artifact" else null
            }
            is PermanentTargetFilter.Enchantment -> {
                if (!cardComponent.typeLine.isEnchantment) "Target must be an enchantment" else null
            }
            is PermanentTargetFilter.Land -> {
                if (!cardComponent.typeLine.isLand) "Target must be a land" else null
            }
            is PermanentTargetFilter.NonCreature -> {
                if (cardComponent.typeLine.isCreature) "Target must be a noncreature permanent" else null
            }
            is PermanentTargetFilter.NonLand -> {
                if (cardComponent.typeLine.isLand) "Target must be a nonland permanent" else null
            }
            is PermanentTargetFilter.CreatureOrLand -> {
                if (!cardComponent.typeLine.isCreature && !cardComponent.typeLine.isLand) {
                    "Target must be a creature or land"
                } else null
            }
            is PermanentTargetFilter.WithColor -> {
                if (!cardComponent.colors.contains(filter.color)) {
                    "Target must be ${filter.color.displayName.lowercase()}"
                } else null
            }
            is PermanentTargetFilter.WithSubtype -> {
                if (!cardComponent.typeLine.hasSubtype(filter.subtype)) {
                    "Target must be a ${filter.subtype.value}"
                } else null
            }
            is PermanentTargetFilter.And -> {
                for (subFilter in filter.filters) {
                    val error = validatePermanentFilter(cardComponent, container, subFilter, casterId)
                    if (error != null) return error
                }
                null
            }
        }
    }

    private fun validatePlayerTarget(state: GameState, target: ChosenTarget): String? {
        if (target !is ChosenTarget.Player) {
            return "Target must be a player"
        }
        if (!state.hasEntity(target.playerId)) {
            return "Target player not found"
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
        return null
    }

    private fun validateAnyTarget(state: GameState, target: ChosenTarget): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found" else null
            }
            is ChosenTarget.Permanent -> {
                if (target.entityId !in state.getBattlefield()) "Target not on battlefield" else null
            }
            else -> "Invalid target type"
        }
    }

    private fun validateCreatureOrPlayerTarget(state: GameState, target: ChosenTarget): String? {
        return when (target) {
            is ChosenTarget.Player -> {
                if (!state.hasEntity(target.playerId)) "Target player not found" else null
            }
            is ChosenTarget.Permanent -> {
                val container = state.getEntity(target.entityId)
                    ?: return "Target not found"
                val cardComponent = container.get<CardComponent>()
                    ?: return "Target is not a card"
                if (!cardComponent.typeLine.isCreature) {
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

    private fun validateCreatureOrPlaneswalkerTarget(state: GameState, target: ChosenTarget): String? {
        if (target !is ChosenTarget.Permanent) {
            return "Target must be a creature or planeswalker"
        }
        val container = state.getEntity(target.entityId)
            ?: return "Target not found"
        val cardComponent = container.get<CardComponent>()
            ?: return "Target is not a card"
        val isPlaneswalker = CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
        if (!cardComponent.typeLine.isCreature && !isPlaneswalker) {
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
        filter: GraveyardCardFilter,
        unifiedFilter: TargetFilter?,
        casterId: EntityId
    ): String? {
        if (target !is ChosenTarget.Card) {
            return "Target must be a card in a graveyard"
        }
        if (target.zone != ZoneType.GRAVEYARD) {
            return "Target must be in a graveyard"
        }

        val zoneKey = ZoneKey(target.ownerId, ZoneType.GRAVEYARD)
        if (target.cardId !in state.getZone(zoneKey)) {
            return "Target not found in graveyard"
        }

        val cardComponent = state.getEntity(target.cardId)?.get<CardComponent>()
            ?: return "Target is not a card"

        // Prefer unified filter when available
        // The unified filter's OwnedByYou predicate handles "your graveyard" restriction
        if (unifiedFilter != null) {
            val predicateContext = PredicateContext(controllerId = casterId)
            val matches = predicateEvaluator.matches(state, target.cardId, unifiedFilter.baseFilter, predicateContext)
            if (!matches) {
                return "Target does not match filter: ${unifiedFilter.description}"
            }
            return null
        }

        // Fall back to legacy filter
        return when (filter) {
            is GraveyardCardFilter.Any -> null
            is GraveyardCardFilter.Creature -> {
                if (!cardComponent.typeLine.isCreature) "Target must be a creature card" else null
            }
            is GraveyardCardFilter.Instant -> {
                if (!cardComponent.typeLine.isInstant) "Target must be an instant card" else null
            }
            is GraveyardCardFilter.Sorcery -> {
                if (!cardComponent.typeLine.isSorcery) "Target must be a sorcery card" else null
            }
            is GraveyardCardFilter.InstantOrSorcery -> {
                if (!cardComponent.typeLine.isInstant && !cardComponent.typeLine.isSorcery) {
                    "Target must be an instant or sorcery card"
                } else null
            }
            is GraveyardCardFilter.CreatureInYourGraveyard -> {
                // Check both type and ownership
                if (!cardComponent.typeLine.isCreature) return "Target must be a creature card"
                if (target.ownerId != casterId) return "Target must be in your graveyard"
                null
            }
        }
    }

    private fun validateSpellTarget(state: GameState, target: ChosenTarget): String? {
        if (target !is ChosenTarget.Spell) {
            return "Target must be a spell on the stack"
        }
        if (target.spellEntityId !in state.stack) {
            return "Target spell not on the stack"
        }
        return null
    }
}
