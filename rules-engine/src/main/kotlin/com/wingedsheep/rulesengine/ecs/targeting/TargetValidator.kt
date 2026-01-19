package com.wingedsheep.rulesengine.ecs.targeting

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.StateProjector

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
        /** A player entity as a target. */
        data class Player(val playerId: EntityId) : ValidatorTarget

        /** A permanent on the battlefield as a target. */
        data class Permanent(val entityId: EntityId) : ValidatorTarget

        /** A spell/ability on the stack as a target. */
        data class StackObject(val entityId: EntityId) : ValidatorTarget

        /** A card in a graveyard as a target. */
        data class GraveyardCard(val entityId: EntityId, val ownerId: EntityId) : ValidatorTarget

        /** A card in exile as a target. */
        data class ExiledCard(val entityId: EntityId) : ValidatorTarget
    }

    /**
     * Targeting requirements for ECS validation.
     */
    sealed interface TargetRequirement {
        val description: String
        val count: Int get() = 1
        val optional: Boolean get() = false

        /** Any target (player or permanent). */
        data class AnyTarget(
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = "any target"
        }

        /** Target player. */
        data class TargetPlayer(
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = "target player"
        }

        /** Target opponent. */
        data class TargetOpponent(
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = "target opponent"
        }

        /** Target creature. */
        data class TargetCreature(
            val filter: CreatureFilter = CreatureFilter.Any,
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = when (filter) {
                CreatureFilter.Any -> "target creature"
                CreatureFilter.YouControl -> "target creature you control"
                CreatureFilter.YouDontControl -> "target creature you don't control"
                CreatureFilter.Attacking -> "target attacking creature"
                CreatureFilter.Blocking -> "target blocking creature"
                CreatureFilter.Tapped -> "target tapped creature"
                CreatureFilter.Untapped -> "target untapped creature"
                is CreatureFilter.WithKeyword -> "target creature with ${filter.keyword.name.lowercase()}"
            }
        }

        /** Target permanent. */
        data class TargetPermanent(
            val filter: PermanentFilter = PermanentFilter.Any,
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = when (filter) {
                PermanentFilter.Any -> "target permanent"
                PermanentFilter.YouControl -> "target permanent you control"
                PermanentFilter.YouDontControl -> "target permanent you don't control"
                PermanentFilter.Artifact -> "target artifact"
                PermanentFilter.Enchantment -> "target enchantment"
                PermanentFilter.ArtifactOrEnchantment -> "target artifact or enchantment"
                PermanentFilter.Land -> "target land"
                PermanentFilter.Nonland -> "target nonland permanent"
            }
        }

        /** Target spell on the stack. */
        data class TargetSpell(
            val filter: SpellFilter = SpellFilter.Any,
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = when (filter) {
                SpellFilter.Any -> "target spell"
                SpellFilter.Instant -> "target instant spell"
                SpellFilter.Sorcery -> "target sorcery spell"
                SpellFilter.Creature -> "target creature spell"
                SpellFilter.Noncreature -> "target noncreature spell"
            }
        }

        /** Target card in a graveyard. */
        data class TargetGraveyardCard(
            val filter: GraveyardFilter = GraveyardFilter.Any,
            override val count: Int = 1,
            override val optional: Boolean = false
        ) : TargetRequirement {
            override val description = when (filter) {
                GraveyardFilter.Any -> "target card in a graveyard"
                GraveyardFilter.YourGraveyard -> "target card in your graveyard"
                GraveyardFilter.OpponentsGraveyard -> "target card in an opponent's graveyard"
                GraveyardFilter.Creature -> "target creature card in a graveyard"
                GraveyardFilter.Instant -> "target instant card in a graveyard"
                GraveyardFilter.Sorcery -> "target sorcery card in a graveyard"
            }
        }
    }

    /** Filters for creature targeting. */
    sealed interface CreatureFilter {
        data object Any : CreatureFilter
        data object YouControl : CreatureFilter
        data object YouDontControl : CreatureFilter
        data object Attacking : CreatureFilter
        data object Blocking : CreatureFilter
        data object Tapped : CreatureFilter
        data object Untapped : CreatureFilter
        data class WithKeyword(val keyword: Keyword) : CreatureFilter
    }

    /** Filters for permanent targeting. */
    sealed interface PermanentFilter {
        data object Any : PermanentFilter
        data object YouControl : PermanentFilter
        data object YouDontControl : PermanentFilter
        data object Artifact : PermanentFilter
        data object Enchantment : PermanentFilter
        data object ArtifactOrEnchantment : PermanentFilter
        data object Land : PermanentFilter
        data object Nonland : PermanentFilter
    }

    /** Filters for spell targeting. */
    sealed interface SpellFilter {
        data object Any : SpellFilter
        data object Instant : SpellFilter
        data object Sorcery : SpellFilter
        data object Creature : SpellFilter
        data object Noncreature : SpellFilter
    }

    /** Filters for graveyard card targeting. */
    sealed interface GraveyardFilter {
        data object Any : GraveyardFilter
        data object YourGraveyard : GraveyardFilter
        data object OpponentsGraveyard : GraveyardFilter
        data object Creature : GraveyardFilter
        data object Instant : GraveyardFilter
        data object Sorcery : GraveyardFilter
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
            is TargetRequirement.AnyTarget -> isValidAnyTarget(target, state, projector, controllerId)
            is TargetRequirement.TargetPlayer -> isValidPlayerTarget(target, state, controllerId, opponentOnly = false)
            is TargetRequirement.TargetOpponent -> isValidPlayerTarget(target, state, controllerId, opponentOnly = true)
            is TargetRequirement.TargetCreature -> isValidCreatureTarget(target, state, projector, controllerId, requirement.filter)
            is TargetRequirement.TargetPermanent -> isValidPermanentTarget(target, state, projector, controllerId, requirement.filter)
            is TargetRequirement.TargetSpell -> isValidSpellTarget(target, state, projector, requirement.filter)
            is TargetRequirement.TargetGraveyardCard -> isValidGraveyardTarget(target, state, controllerId, requirement.filter)
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
        filter: CreatureFilter
    ): Boolean {
        if (target !is ValidatorTarget.Permanent) return false

        val view = projector.getView(target.entityId) ?: return false
        if (!view.isCreature) return false
        if (!canBeTargetedBy(view, controllerId)) return false

        return when (filter) {
            CreatureFilter.Any -> true
            CreatureFilter.YouControl -> view.controllerId == controllerId
            CreatureFilter.YouDontControl -> view.controllerId != controllerId
            CreatureFilter.Attacking -> state.combat?.isAttacking(target.entityId) == true
            CreatureFilter.Blocking -> state.combat?.isBlocking(target.entityId) == true
            CreatureFilter.Tapped -> view.isTapped
            CreatureFilter.Untapped -> !view.isTapped
            is CreatureFilter.WithKeyword -> view.hasKeyword(filter.keyword)
        }
    }

    private fun isValidPermanentTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        controllerId: EntityId,
        filter: PermanentFilter
    ): Boolean {
        if (target !is ValidatorTarget.Permanent) return false

        val view = projector.getView(target.entityId) ?: return false
        if (!view.isPermanent) return false
        if (!canBeTargetedBy(view, controllerId)) return false

        return when (filter) {
            PermanentFilter.Any -> true
            PermanentFilter.YouControl -> view.controllerId == controllerId
            PermanentFilter.YouDontControl -> view.controllerId != controllerId
            PermanentFilter.Artifact -> view.isArtifact
            PermanentFilter.Enchantment -> view.isEnchantment
            PermanentFilter.ArtifactOrEnchantment -> view.isArtifact || view.isEnchantment
            PermanentFilter.Land -> view.isLand
            PermanentFilter.Nonland -> !view.isLand
        }
    }

    private fun isValidSpellTarget(
        target: ValidatorTarget,
        state: GameState,
        projector: StateProjector,
        filter: SpellFilter
    ): Boolean {
        if (target !is ValidatorTarget.StackObject) return false
        if (target.entityId !in state.getStack()) return false

        val view = projector.getView(target.entityId) ?: return false

        return when (filter) {
            SpellFilter.Any -> true
            SpellFilter.Instant -> view.isInstant
            SpellFilter.Sorcery -> view.isSorcery
            SpellFilter.Creature -> view.isCreature
            SpellFilter.Noncreature -> !view.isCreature
        }
    }

    private fun isValidGraveyardTarget(
        target: ValidatorTarget,
        state: GameState,
        controllerId: EntityId,
        filter: GraveyardFilter
    ): Boolean {
        if (target !is ValidatorTarget.GraveyardCard) return false

        // Verify it's actually in that player's graveyard
        if (target.entityId !in state.getGraveyard(target.ownerId)) return false

        // Check filter
        return when (filter) {
            GraveyardFilter.Any -> true
            GraveyardFilter.YourGraveyard -> target.ownerId == controllerId
            GraveyardFilter.OpponentsGraveyard -> target.ownerId != controllerId
            GraveyardFilter.Creature -> {
                val component = state.getComponent<com.wingedsheep.rulesengine.ecs.components.CardComponent>(target.entityId)
                component?.isCreature == true
            }
            GraveyardFilter.Instant -> {
                val component = state.getComponent<com.wingedsheep.rulesengine.ecs.components.CardComponent>(target.entityId)
                component?.definition?.typeLine?.isInstant == true
            }
            GraveyardFilter.Sorcery -> {
                val component = state.getComponent<com.wingedsheep.rulesengine.ecs.components.CardComponent>(target.entityId)
                component?.definition?.typeLine?.isSorcery == true
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
            is TargetRequirement.AnyTarget -> {
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

            is TargetRequirement.TargetPlayer,
            is TargetRequirement.TargetOpponent -> {
                for (playerId in state.getPlayerIds()) {
                    val target = ValidatorTarget.Player(playerId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetRequirement.TargetCreature,
            is TargetRequirement.TargetPermanent -> {
                for (entityId in state.getBattlefield()) {
                    val target = ValidatorTarget.Permanent(entityId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetRequirement.TargetSpell -> {
                for (entityId in state.getStack()) {
                    val target = ValidatorTarget.StackObject(entityId)
                    if (isValidTarget(target, requirement, state, projector, controllerId)) {
                        targets.add(target)
                    }
                }
            }

            is TargetRequirement.TargetGraveyardCard -> {
                for (playerId in state.getPlayerIds()) {
                    for (entityId in state.getGraveyard(playerId)) {
                        val target = ValidatorTarget.GraveyardCard(entityId, playerId)
                        if (isValidTarget(target, requirement, state, projector, controllerId)) {
                            targets.add(target)
                        }
                    }
                }
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
        // Use short-circuit evaluation
        when (requirement) {
            is TargetRequirement.AnyTarget -> {
                // Check players first
                for (playerId in state.getPlayerIds()) {
                    if (isValidTarget(ValidatorTarget.Player(playerId), requirement, state, projector, controllerId)) {
                        return true
                    }
                }
                // Check permanents
                for (entityId in state.getBattlefield()) {
                    if (isValidTarget(ValidatorTarget.Permanent(entityId), requirement, state, projector, controllerId)) {
                        return true
                    }
                }
            }

            is TargetRequirement.TargetPlayer,
            is TargetRequirement.TargetOpponent -> {
                for (playerId in state.getPlayerIds()) {
                    if (isValidTarget(ValidatorTarget.Player(playerId), requirement, state, projector, controllerId)) {
                        return true
                    }
                }
            }

            is TargetRequirement.TargetCreature,
            is TargetRequirement.TargetPermanent -> {
                for (entityId in state.getBattlefield()) {
                    if (isValidTarget(ValidatorTarget.Permanent(entityId), requirement, state, projector, controllerId)) {
                        return true
                    }
                }
            }

            is TargetRequirement.TargetSpell -> {
                for (entityId in state.getStack()) {
                    if (isValidTarget(ValidatorTarget.StackObject(entityId), requirement, state, projector, controllerId)) {
                        return true
                    }
                }
            }

            is TargetRequirement.TargetGraveyardCard -> {
                for (playerId in state.getPlayerIds()) {
                    for (entityId in state.getGraveyard(playerId)) {
                        if (isValidTarget(ValidatorTarget.GraveyardCard(entityId, playerId), requirement, state, projector, controllerId)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
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
}
