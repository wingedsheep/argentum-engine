package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.mechanics.targeting.ColorProtection
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.SourceTypeTargeting
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.targeting.HexproofSuppression
import com.wingedsheep.engine.mechanics.targeting.PlayerTargetRestriction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

/**
 * The CR 608.2b target re-check a spell or ability makes as it resolves: every target is checked
 * again for legality (zone, object identity, shroud / hexproof / protection, and the requirement's
 * own filter) and the illegal ones are dropped. Shared by [SpellResolver] and [AbilityResolver].
 */
internal class ResolutionTargetValidator(
    private val predicateEvaluator: PredicateEvaluator
) {
    /**
     * Validate targets and return only valid ones.
     *
     * Checks zone existence, protection (Rule 702.16), and target filter matching
     * (Rule 608.2b — targets must still be legal when the spell/ability resolves).
     */
    fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        controllerId: EntityId,
        targetRequirements: List<TargetRequirement> = emptyList(),
        sourceId: EntityId? = null,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        xValue: Int? = null,
        triggeringEntityId: EntityId? = null,
        triggeringPlayerId: EntityId? = null,
        /**
         * The object-identity stamps captured when these targets were chosen
         * ([TargetsComponent.targetEntryStamps]) — a permanent that left the battlefield and came
         * back in the meantime is a different object and no longer a legal target (CR 400.7).
         */
        targetEntryStamps: Map<EntityId, Long> = emptyMap(),
        /**
         * Pipeline collections available at resolution time (e.g. the amassed Army under
         * `EffectTarget.AmassedArmy`, from a `ReflexiveTriggerEffect`'s carried pipeline) — the
         * CR 608.2b re-validation below re-checks the target filter, and a filter like Grishnákh's
         * "power <= the amassed Army's power" needs this to resolve the referenced entity, or every
         * target wrongly fails re-validation as unresolvable.
         */
        storedCollections: Map<String, List<EntityId>> = emptyMap()
    ): List<ChosenTarget> {
        // Always project state for shroud/hexproof checks (Rule 702.18, 702.11)
        val projected = state.projectedState
        val predicateContext = PredicateContext(
            controllerId = controllerId,
            sourceId = sourceId,
            xValue = xValue,
            triggeringEntityId = triggeringEntityId,
            triggeringPlayerId = triggeringPlayerId,
            storedCollections = storedCollections,
            targets = targets,
            // A filter bound to an earlier named target ("target creature that player controls",
            // Ravager of the Fells) re-checks against the same choice at resolution.
            namedTargets = EffectContext.buildNamedTargets(targetRequirements, targets),
        )

        return targets.filterIndexed { index, target ->
            anyTargetStillMatches(state, projected, index, target, targetRequirements, predicateContext) &&
                when (target) {
                    is ChosenTarget.Player ->
                        isPlayerTargetLegal(state, index, target, targetRequirements, controllerId, sourceId)

                    is ChosenTarget.Permanent -> isPermanentTargetLegal(
                        state, projected, index, target, targetRequirements, sourceColors, sourceSubtypes,
                        controllerId, sourceId, targetingSourceType, targetEntryStamps, predicateContext
                    )

                    is ChosenTarget.Card -> {
                        // Card is valid if in expected zone
                        val zoneKey = ZoneKey(target.ownerId, target.zone)
                        target.cardId in state.getZone(zoneKey)
                    }

                    is ChosenTarget.Spell -> {
                        // Spell is valid if still on stack
                        target.spellEntityId in state.stack
                    }
                }
        }
    }

    /** An "any target" slot (CR 115.4) still holds a creature, planeswalker, battle or player its filter accepts. */
    private fun anyTargetStillMatches(
        state: GameState,
        projected: ProjectedState,
        index: Int,
        target: ChosenTarget,
        targetRequirements: List<TargetRequirement>,
        predicateContext: PredicateContext
    ): Boolean {
        var baseRequirement = getRequirementForTargetIndex(index, targetRequirements)
        while (baseRequirement is TargetOther) baseRequirement = baseRequirement.baseRequirement
        if (baseRequirement is AnyTarget) {
            val recipient = when (target) {
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Permanent -> target.entityId
                else -> return false
            }
            if (target is ChosenTarget.Permanent && !projected.isCreature(recipient) &&
                !projected.isPlaneswalker(recipient) && !projected.isBattle(recipient)
            ) return false
            if (!predicateEvaluator.matches(state, projected, recipient, baseRequirement.filter, predicateContext)) {
                return false
            }
        }
        return true
    }

    private fun isPlayerTargetLegal(
        state: GameState,
        index: Int,
        target: ChosenTarget.Player,
        targetRequirements: List<TargetRequirement>,
        controllerId: EntityId,
        sourceId: EntityId?
    ): Boolean {
        // Player is valid if they exist and haven't lost...
        if (!state.hasEntity(target.playerId)) return false
        // ...and (CR 608.2b) the player-target restriction still holds. A player who
        // gained life above the threshold, or whose "lost life this turn" never
        // happened, is removed at resolution.
        val requirement = getRequirementForTargetIndex(index, targetRequirements)
        val restriction = when (requirement) {
            is TargetPlayer -> requirement.restriction
            is TargetOpponent -> requirement.restriction
            else -> null
        }
        return PlayerTargetRestriction.isSatisfied(state, restriction, target.playerId, controllerId, sourceId, predicateEvaluator = predicateEvaluator)
    }

    private fun isPermanentTargetLegal(
        state: GameState,
        projected: ProjectedState,
        index: Int,
        target: ChosenTarget.Permanent,
        targetRequirements: List<TargetRequirement>,
        sourceColors: Set<Color>,
        sourceSubtypes: Set<String>,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType,
        targetEntryStamps: Map<EntityId, Long>,
        predicateContext: PredicateContext
    ): Boolean {
        // Permanent is valid if still on battlefield
        if (target.entityId !in state.getBattlefield()) return false

        // ...and if it's still the same object. A permanent blinked in response
        // (Personify, Cloudshift) reuses its entity id here, but it returned as a new
        // object (CR 400.7) that was never targeted, so the target is illegal.
        if (TargetsComponent.isDifferentObject(state, target.entityId, targetEntryStamps)) {
            return false
        }

        val entityController = projected.getController(target.entityId)
            ?: state.getEntity(target.entityId)?.get<ControllerComponent>()?.playerId
        return passesTargetingRestrictions(
            state, projected, target, entityController, controllerId, sourceId, sourceColors, targetingSourceType
        ) &&
            passesProtection(state, projected, target, entityController, controllerId, sourceId, sourceColors, sourceSubtypes) &&
            stillMatchesRequirementFilter(state, projected, index, target, targetRequirements, predicateContext)
    }

    /** Shroud, hexproof and the "can't be the target of …" restrictions, re-checked as at targeting. */
    private fun passesTargetingRestrictions(
        state: GameState,
        projected: ProjectedState,
        target: ChosenTarget.Permanent,
        entityController: EntityId?,
        controllerId: EntityId,
        sourceId: EntityId?,
        sourceColors: Set<Color>,
        targetingSourceType: TargetingSourceType
    ): Boolean {
        // Check shroud — can't be targeted by anyone (Rule 702.18)
        if (projected.hasKeyword(target.entityId, "SHROUD")) return false

        // "Can't be the target of spells" (Lurker) — spells only, so an ability
        // resolving against the same permanent is unaffected. Mirrors the cast-time
        // check in TargetValidator so CR 608.2b re-validation agrees with it: a
        // permanent that gained the restriction after being targeted is dropped here.
        if (targetingSourceType == TargetingSourceType.SPELL &&
            projected.hasKeyword(target.entityId, AbilityFlag.CANT_BE_TARGETED_BY_SPELLS)
        ) {
            return false
        }

        // Check hexproof — can't be targeted by opponents (Rule 702.11)
        val hexproofSuppressed = HexproofSuppression.isSuppressedForCaster(state, projected, target.entityId, controllerId, predicateEvaluator = predicateEvaluator)
        if (!hexproofSuppressed && projected.hasKeyword(target.entityId, "HEXPROOF") && entityController != controllerId) return false

        // Check hexproof from color (Rule 702.11b)
        if (!hexproofSuppressed && entityController != controllerId) {
            for (color in sourceColors) {
                if (projected.hasKeyword(target.entityId, "HEXPROOF_FROM_${color.name}")) {
                    return false
                }
            }
            // ...and from the source's card types, e.g. "hexproof from instants"
            // (Elenda, Saint of Dusk). Same source-type resolution as protection.
            if (sourceId != null) {
                for (cardType in SourceTypeTargeting.sourceCardTypes(state, sourceId)) {
                    if (projected.hasKeyword(
                            target.entityId,
                            "HEXPROOF_FROM_CARDTYPE_${cardType.uppercase()}"
                        )
                    ) {
                        return false
                    }
                }
            }
        }

        // Check can't-be-targeted-by-abilities (Shanna, Sisay's Legacy)
        if (targetingSourceType != TargetingSourceType.SPELL && entityController != controllerId) {
            if (ControllerGrants.isActiveOn<CantBeTargetedByOpponentAbilitiesComponent>(
                    state,
                    target.entityId,
                    predicateEvaluator = predicateEvaluator
                )
            ) {
                return false
            }
        }

        // Artifact Ward family: can't be the target of abilities from sources of a
        // given card type. Keys off the ability's source (CR 113.7) by card type, not
        // controller — applies even to the warded creature's own controller's sources.
        // Spells bypass (abilities-only).
        if (SourceTypeTargeting.cantBeTargetedBySourceTypeAbility(
                state, target.entityId, sourceId, targetingSourceType
            )
        ) {
            return false
        }
        return true
    }

    /** Protection from the source's colors, subtypes, card types, or from each opponent (CR 702.16). */
    private fun passesProtection(
        state: GameState,
        projected: ProjectedState,
        target: ChosenTarget.Permanent,
        entityController: EntityId?,
        controllerId: EntityId,
        sourceId: EntityId?,
        sourceColors: Set<Color>,
        sourceSubtypes: Set<String>
    ): Boolean {
        // Check protection from source colors/subtypes (Rule 702.16)
        val sourceKnown = sourceId != null && state.getEntity(sourceId) != null
        if ((sourceKnown || sourceColors.isNotEmpty()) &&
            ColorProtection.isProtected(projected, target.entityId, sourceColors.map { it.name })
        ) {
            return false
        }
        for (subtype in sourceSubtypes) {
            if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                return false
            }
        }
        // Check protection from the source's card type, e.g. "protection from creatures"
        // (Rule 702.16). Prefer projected types (permanent sources); fall back to the
        // card's printed card types for spell/ability sources not in the projection.
        if (sourceId != null) {
            val projectedTypes = projected.getTypes(sourceId)
            val sourceCardTypes = if (projectedTypes.isNotEmpty()) {
                projectedTypes
            } else {
                state.getEntity(sourceId)?.get<CardComponent>()
                    ?.typeLine?.cardTypes?.map { it.name }?.toSet() ?: emptySet()
            }
            for (cardType in sourceCardTypes) {
                if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_CARDTYPE_${cardType.uppercase()}")) {
                    return false
                }
            }
        }

        // Check protection from each opponent (Rule 702.16e)
        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_EACH_OPPONENT") &&
            entityController != null && entityController != controllerId) {
            return false
        }
        return true
    }

    private fun stillMatchesRequirementFilter(
        state: GameState,
        projected: ProjectedState,
        index: Int,
        target: ChosenTarget.Permanent,
        targetRequirements: List<TargetRequirement>,
        predicateContext: PredicateContext
    ): Boolean {
        // Re-validate target filter (Rule 608.2b)
        val requirement = getRequirementForTargetIndex(index, targetRequirements)
        val filter = extractTargetFilter(requirement)
        if (filter != null) {
            if (!predicateEvaluator.matches(
                    state, projected, target.entityId, filter.baseFilter, predicateContext
                )
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Project [validTargets] (the compacted output of [validateTargets]) back onto
     * [originalTargets] positions, returning a list parallel to [originalTargets] with
     * `null` in slots whose target was dropped by 608.2b validation. Walks both lists
     * in order — [validateTargets] preserves the relative ordering of survivors — so the
     * mapping is unambiguous even when two original targets compare structurally equal.
     */
    fun buildAlignedValidated(
        originalTargets: List<ChosenTarget>,
        validTargets: List<ChosenTarget>
    ): List<ChosenTarget?> {
        var v = 0
        return originalTargets.map { orig ->
            if (v < validTargets.size && validTargets[v] === orig) {
                v++
                orig
            } else {
                null
            }
        }
    }

    /**
     * Find the TargetRequirement that corresponds to a given target index.
     * Requirements are matched to targets in order, with each requirement
     * consuming `count` targets.
     */
    private fun getRequirementForTargetIndex(
        targetIndex: Int,
        requirements: List<TargetRequirement>
    ): TargetRequirement? {
        var idx = 0
        for (req in requirements) {
            val end = idx + req.count
            if (targetIndex in idx until end) return req
            idx = end
        }
        return null
    }

    /**
     * Extract the TargetFilter from a TargetRequirement, if it has one.
     */
    private fun extractTargetFilter(requirement: TargetRequirement?): TargetFilter? {
        return when (requirement) {
            is TargetObject -> requirement.filter
            else -> null
        }
    }
}
