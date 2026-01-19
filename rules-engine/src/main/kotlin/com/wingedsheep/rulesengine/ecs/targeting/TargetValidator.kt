package com.wingedsheep.rulesengine.ecs.targeting

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.components.AttackingComponent
import com.wingedsheep.rulesengine.ecs.components.BlockingComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.LostGameComponent
import com.wingedsheep.rulesengine.ecs.components.PlayerComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.targeting.*

/**
 * ECS-native target validation using StateProjector for entity views.
 *
 * This validator works directly with GameState and uses GameObjectView
 * for all entity data, ensuring that continuous effects (layers) are properly
 * applied when validating targets.
 */
object TargetValidator {

    /**
     * Result of target validation.
     */
    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
        data object Fizzle : ValidationResult  // All targets now illegal
    }

    /**
     * Target types that can be validated.
     */
    sealed interface ValidatorTarget {
        data class Player(val playerId: EntityId) : ValidatorTarget
        data class Permanent(val entityId: EntityId) : ValidatorTarget
        data class StackObject(val entityId: EntityId) : ValidatorTarget
        data class GraveyardCard(val entityId: EntityId, val ownerId: EntityId) : ValidatorTarget
        data class ExiledCard(val entityId: EntityId) : ValidatorTarget
    }

    // ==========================================================================
    // Target Validation
    // ==========================================================================

    /**
     * Validate a single target against a requirement.
     */
    fun validateTarget(
        target: ValidatorTarget,
        requirement: TargetRequirement,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): ValidationResult {
        return if (isValidTarget(target, requirement, state, projector, controllerId)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Target is not valid for: ${requirement.description}")
        }
    }

    /**
     * Check if a target is valid for a requirement.
     */
    fun isValidTarget(
        target: ValidatorTarget,
        requirement: TargetRequirement,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): Boolean {
        return when (requirement) {
            is AnyTarget -> isValidAnyTarget(target, state, projector, controllerId)
            is TargetPlayer -> isValidPlayerTarget(target, state, controllerId, opponentOnly = false)
            is TargetOpponent -> isValidPlayerTarget(target, state, controllerId, opponentOnly = true)
            is TargetCreature -> isValidCreatureTarget(target, state, projector, controllerId, requirement.filter)
            is TargetPermanent -> isValidPermanentTarget(target, state, projector, controllerId, requirement.filter)
            is TargetSpell -> isValidSpellTarget(target, state, projector, requirement.filter)
            is TargetCardInGraveyard -> isValidGraveyardTarget(target, state, controllerId, requirement.filter)
            // Handle other requirements or composite requirements if added later
            else -> false
        }
    }

    private fun isValidAnyTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): Boolean = when (target) {
        is ValidatorTarget.Player -> state.hasEntity(target.playerId) && state.isAlive(target.playerId)
        is ValidatorTarget.Permanent -> {
            val view = projector.getView(target.entityId)
            view != null && canBeTargetedBy(view, controllerId)
        }
        else -> false
    }

    private fun isValidPlayerTarget(
        target: ValidatorTarget,
        state: GameState,
        controllerId: EntityId,
        opponentOnly: Boolean
    ): Boolean {
        if (target !is ValidatorTarget.Player) return false
        if (!state.hasEntity(target.playerId)) return false
        if (!state.isAlive(target.playerId)) return false
        if (opponentOnly && target.playerId == controllerId) return false
        return true
    }

    private fun isValidCreatureTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId,
        filter: CreatureTargetFilter
    ): Boolean {
        if (target !is ValidatorTarget.Permanent) return false

        val view = projector.getView(target.entityId) ?: return false
        if (!view.isCreature) return false
        if (!canBeTargetedBy(view, controllerId)) return false

        return when (filter) {
            CreatureTargetFilter.Any -> true
            CreatureTargetFilter.YouControl -> view.controllerId == controllerId
            CreatureTargetFilter.OpponentControls -> view.controllerId != controllerId
            CreatureTargetFilter.Attacking -> state.hasComponent<AttackingComponent>(target.entityId)
            CreatureTargetFilter.Blocking -> state.hasComponent<BlockingComponent>(target.entityId)
            CreatureTargetFilter.Tapped -> view.isTapped
            CreatureTargetFilter.Untapped -> !view.isTapped
            is CreatureTargetFilter.WithKeyword -> view.hasKeyword(filter.keyword)
            is CreatureTargetFilter.WithoutKeyword -> !view.hasKeyword(filter.keyword)
            is CreatureTargetFilter.WithColor -> view.colors.contains(filter.color)
            is CreatureTargetFilter.WithPowerAtMost -> (view.power ?: 0) <= filter.maxPower
            is CreatureTargetFilter.WithPowerAtLeast -> (view.power ?: 0) >= filter.minPower
            is CreatureTargetFilter.WithToughnessAtMost -> (view.toughness ?: 0) <= filter.maxToughness
            is CreatureTargetFilter.WithSubtype -> view.subtypes.contains(filter.subtype)
            is CreatureTargetFilter.And -> filter.filters.all { subFilter ->
                isValidCreatureTarget(target, state, projector, controllerId, subFilter)
            }
        }
    }

    private fun isValidPermanentTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId,
        filter: PermanentTargetFilter
    ): Boolean {
        if (target !is ValidatorTarget.Permanent) return false

        val view = projector.getView(target.entityId) ?: return false
        if (!view.isPermanent) return false
        if (!canBeTargetedBy(view, controllerId)) return false

        return when (filter) {
            PermanentTargetFilter.Any -> true
            PermanentTargetFilter.YouControl -> view.controllerId == controllerId
            PermanentTargetFilter.OpponentControls -> view.controllerId != controllerId
            PermanentTargetFilter.Artifact -> view.isArtifact
            PermanentTargetFilter.Enchantment -> view.isEnchantment
            PermanentTargetFilter.Creature -> view.isCreature
            PermanentTargetFilter.Land -> view.isLand
            PermanentTargetFilter.NonCreature -> !view.isCreature
            PermanentTargetFilter.NonLand -> !view.isLand
        }
    }

    private fun isValidSpellTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        filter: SpellTargetFilter
    ): Boolean {
        if (target !is ValidatorTarget.StackObject) return false
        if (target.entityId !in state.getStack()) return false

        val view = projector.getView(target.entityId) ?: return false

        return when (filter) {
            SpellTargetFilter.Any -> true
            SpellTargetFilter.Instant -> view.isInstant
            SpellTargetFilter.Sorcery -> view.isSorcery
            SpellTargetFilter.Creature -> view.isCreature
            SpellTargetFilter.Noncreature -> !view.isCreature
        }
    }

    private fun isValidGraveyardTarget(
        target: ValidatorTarget,
        state: GameState,
        controllerId: EntityId,
        filter: GraveyardCardFilter
    ): Boolean {
        if (target !is ValidatorTarget.GraveyardCard) return false

        // Verify it's actually in that player's graveyard
        if (target.entityId !in state.getGraveyard(target.ownerId)) return false

        // Check filter
        return when (filter) {
            GraveyardCardFilter.Any -> true
            // Note: GraveyardCardFilter might need updating to support "YourGraveyard" logic if strictly enforced here
            // or we assume the restriction is handled by the `TargetGraveyardCard` (which doesn't restrict ownership by default)
            GraveyardCardFilter.Creature -> {
                val component = state.getComponent<CardComponent>(target.entityId)
                component?.isCreature == true
            }
            GraveyardCardFilter.Instant -> {
                val component = state.getComponent<CardComponent>(target.entityId)
                component?.definition?.typeLine?.isInstant == true
            }
            GraveyardCardFilter.Sorcery -> {
                val component = state.getComponent<CardComponent>(target.entityId)
                component?.definition?.typeLine?.isSorcery == true
            }
            GraveyardCardFilter.InstantOrSorcery -> {
                val component = state.getComponent<CardComponent>(target.entityId)
                val typeLine = component?.definition?.typeLine
                typeLine?.isInstant == true || typeLine?.isSorcery == true
            }
        }
    }

    /**
     * Check if a permanent can be targeted by a source controller (hexproof/shroud check).
     */
    private fun canBeTargetedBy(view: GameObjectView, sourceControllerId: EntityId): Boolean {
        // Shroud: Can't be targeted by any spell or ability
        if (view.hasKeyword(Keyword.SHROUD)) return false

        // Hexproof: Can't be targeted by opponents
        if (view.hasKeyword(Keyword.HEXPROOF)) {
            return view.controllerId == sourceControllerId
        }

        return true
    }

    // ==========================================================================
    // Legal Target Discovery
    // ==========================================================================

    /**
     * Get all legal targets for a requirement.
     */
    fun getLegalTargets(
        requirement: TargetRequirement,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): List<ValidatorTarget> {
        val targets = mutableListOf<ValidatorTarget>()

        when (requirement) {
            is AnyTarget -> {
                // Add players
                for (playerId in state.getPlayerIds()) {
                    val target = ValidatorTarget.Player(playerId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
                // Add permanents
                for (entityId in state.getBattlefield()) {
                    val target = ValidatorTarget.Permanent(entityId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetPlayer,
            is TargetOpponent -> {
                for (playerId in state.getPlayerIds()) {
                    val target = ValidatorTarget.Player(playerId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetCreature,
            is TargetPermanent -> {
                for (entityId in state.getBattlefield()) {
                    val target = ValidatorTarget.Permanent(entityId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetSpell -> {
                for (entityId in state.getStack()) {
                    val target = ValidatorTarget.StackObject(entityId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetCardInGraveyard -> {
                for (playerId in state.getPlayerIds()) {
                    for (entityId in state.getGraveyard(playerId)) {
                        val target = ValidatorTarget.GraveyardCard(entityId, playerId)
                        if (isValidTarget(target, requirement, state, projector, controllerId)) {
                            targets.add(target)
                        }
                    }
                }
            }

            else -> {
                // Handle other target types if necessary
            }
        }

        return targets
    }

    /**
     * Check if any legal targets exist for a requirement.
     */
    fun hasLegalTargets(
        requirement: TargetRequirement,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): Boolean {
        val legalTargets = getLegalTargets(requirement, state, projector, controllerId)
        return legalTargets.isNotEmpty()
    }

    /**
     * Check if a spell/ability can be cast given its targeting requirements.
     */
    fun canMeetRequirements(
        requirements: List<TargetRequirement>,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId
    ): Boolean {
        for (requirement in requirements) {
            if (!requirement.optional && !hasLegalTargets(requirement, state, projector, controllerId)) {
                return false
            }
        }
        return true
    }

    // ==========================================================================
    // ChosenTarget Validation (Stack Resolution)
    // ==========================================================================

    /**
     * Validate a list of chosen targets against current game state (on resolution).
     * Used by StackResolver.
     */
    fun validateChosenTargets(
        state: GameState,
        targets: List<ChosenTarget>
    ): List<ChosenTarget> {
        return targets.filter { target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player target is valid if they haven't lost
                    val container = state.getEntity(target.playerId)
                    container != null &&
                            container.has<PlayerComponent>() &&
                            !container.has<LostGameComponent>()
                }
                is ChosenTarget.Permanent -> {
                    // Permanent target is valid if still on battlefield
                    target.entityId in state.getBattlefield()
                }
                is ChosenTarget.Card -> {
                    // Card target is valid if still in the specified zone
                    target.cardId in state.getZone(target.zoneId)
                }
                is ChosenTarget.Spell -> {
                    // Spell target is valid if still on the stack
                    target.spellEntityId in state.getStack()
                }
            }
        }
    }
}
