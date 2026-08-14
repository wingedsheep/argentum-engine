package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.SourceTypeTargeting
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The card-type names (CR 205.2a). `ProjectedState.getTypes` folds supertypes (LEGENDARY, BASIC,
 * SNOW, …) in beside them, so a "share a card type" comparison has to sieve through this — else two
 * legendary permanents would qualify by both being legendary.
 */
private val CARD_TYPE_NAMES: Set<String> = CardType.entries.mapTo(mutableSetOf()) { it.name }

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
        sourceId: EntityId? = null,
        xValue: Int? = null
    ): String? {
        // Use the game state for validation
        // StateProjector is used for P/T checks to account for continuous effects

        // Match targets to requirements (assuming targets are in order of requirements).
        // For TargetObject with a dynamicMaxCount, the resolved dynamic value clamps the
        // per-req max count (the static `count` field is just a placeholder). XValue is
        // threaded via the chosen [xValue]; every other DynamicAmount is evaluated against
        // board state here, mirroring TriggerProcessor.snapshotDynamicCount — so a cast-time
        // spell with e.g. `dynamicMaxCount = Count(...)` caps correctly instead of falling
        // back to the static placeholder.
        // An explicit `dynamicMaxCount` outranks the `unlimited` flag. `unlimited` means "no
        // *static* upper bound" — it is the count the author didn't write down — whereas a
        // dynamic cap is a bound the author did write down and is simply not knowable until
        // cast time. Grove's Bounty needs both: "any number of target creatures you control"
        // with X counters to hand out, where CR 601.2d still forbids declaring more targets
        // than there are counters. Checking `unlimited` first would drop that cap on the floor.
        fun effectiveMaxCount(req: TargetRequirement): Int {
            val unboundedFallback = if (req.unlimited) Int.MAX_VALUE else req.count
            if (req is TargetObject) {
                val dyn = req.dynamicMaxCount
                if (dyn == DynamicAmount.XValue) {
                    return xValue ?: unboundedFallback
                }
                if (dyn != null) {
                    return try {
                        val context = EffectContext(
                            sourceId = sourceId,
                            controllerId = casterId,
                            xValue = xValue
                        )
                        DynamicAmountEvaluator().evaluate(state, dyn, context).coerceAtLeast(0)
                    } catch (_: Exception) {
                        unboundedFallback
                    }
                }
            }
            return unboundedFallback
        }
        for ((index, requirement) in requirements.withIndex()) {
            // Get targets for this requirement (handle multi-target requirements)
            val targetCount = effectiveMaxCount(requirement)
            val startIdx = requirements.take(index).sumOf { effectiveMaxCount(it) }
            // Use Long for the end index so an unlimited requirement (targetCount = Int.MAX_VALUE)
            // doesn't overflow to a negative value and make subList throw.
            val endIdx = (startIdx.toLong() + targetCount.toLong())
                .coerceAtMost(targets.size.toLong()).toInt()
            val targetsForReq = targets.subList(
                startIdx.coerceAtMost(targets.size),
                endIdx
            )

            // Reject if too many targets were declared. When a requirement is *effectively*
            // unbounded ("any number of target ...", Drafna's Restoration) there is no upper
            // bound, so skip this check — summing Int.MAX_VALUE would overflow to a negative cap
            // and spuriously reject a legal cast. An unlimited requirement that also carries a
            // resolved `dynamicMaxCount` (Grove's Bounty) is bounded after all, so it is checked.
            if (requirements.none { effectiveMaxCount(it) == Int.MAX_VALUE }) {
                val totalMax = requirements.sumOf { effectiveMaxCount(it) }
                if (targets.size > totalMax) {
                    return "Too many targets for ${requirement.description}"
                }
            }

            // Check minimum targets
            if (targetsForReq.size < requirement.effectiveMinCount) {
                return "Not enough targets for ${requirement.description}"
            }

            // Validate each target against the requirement
            for (target in targetsForReq) {
                val error = validateSingleTarget(state, target, requirement, casterId, sourceColors, sourceSubtypes, sourceId, xValue, targets)
                if (error != null) return error
            }

            // "Two/X target ..." — the same object or player can't be chosen more than once for a
            // single instance of the word "target" (CR 601.2c; applies to abilities via 602.2b).
            // Cross-requirement duplicates are a different "target" instance and stay legal by
            // default — that distinctness is opt-in via TargetOther below.
            if (targetsForReq.size > 1 && targetsForReq.distinct().size != targetsForReq.size) {
                return "The same target can't be chosen more than once for ${requirement.description}"
            }

            // "Another target" — must differ from any earlier target chosen for this same cast
            if (requirement is TargetOther) {
                val priorTargets = targets.subList(0, startIdx.coerceAtMost(targets.size))
                for (target in targetsForReq) {
                    if (priorTargets.any { it == target }) {
                        return "Target must be different from the other chosen targets"
                    }
                }
            }

            // "... controlled by the same player" — every chosen target for this requirement
            // must share a controller (Rule uses current control; projected state respects
            // control-changing effects). No-op for single-target requirements.
            if (requirement is TargetObject && requirement.sameController && targetsForReq.size > 1) {
                val projected = state.projectedState
                val controllers = targetsForReq.mapNotNull { target ->
                    (target as? ChosenTarget.Permanent)?.let { perm ->
                        projected.getController(perm.entityId)
                            ?: state.getEntity(perm.entityId)?.get<ControllerComponent>()?.playerId
                    }
                }
                if (controllers.toSet().size > 1) {
                    return "Targets must be controlled by the same player"
                }
            }

            // "... from a single graveyard" — every chosen card target for this requirement
            // must share an owner (CR uses the graveyard's owner). No-op for single-target
            // requirements and for non-card targets.
            if (requirement is TargetObject && requirement.sameOwner && targetsForReq.size > 1) {
                val owners = targetsForReq.mapNotNull { target ->
                    (target as? ChosenTarget.Card)?.ownerId
                }
                if (owners.toSet().size > 1) {
                    return "Targets must be from a single graveyard"
                }
            }

            // "... that share a creature type" — every chosen permanent target must hold at least
            // one creature type in common with all the others (Secret Tunnel). Uses projected
            // subtypes so granted/changed types count. No-op for single-target requirements; a
            // target with no creature types (or one off the battlefield) can never share, so the
            // set is rejected.
            if (requirement is TargetObject && requirement.sameCreatureType && targetsForReq.size > 1) {
                val projected = state.projectedState
                val subtypeSets = targetsForReq.map { target ->
                    (target as? ChosenTarget.Permanent)
                        ?.takeIf { it.entityId in state.getBattlefield() }
                        ?.let { projected.getSubtypes(it.entityId) }
                        ?: emptySet()
                }
                val shared = subtypeSets.reduce { acc, next -> acc intersect next }
                if (shared.isEmpty()) {
                    return "Targets must share a creature type"
                }
            }

            // "... that share a card type" — every chosen permanent target must hold at least one
            // *card type* (CR 205.2a) in common with all the others (Burglar's Plot). Projected
            // types, so an animated land counts as a creature; supertypes are sieved out, so two
            // legendary permanents don't qualify by both being legendary. No-op for single-target
            // requirements; a target off the battlefield contributes nothing and rejects the set.
            if (requirement is TargetObject && requirement.sameCardType && targetsForReq.size > 1) {
                val projected = state.projectedState
                val typeSets = targetsForReq.map { target ->
                    (target as? ChosenTarget.Permanent)
                        ?.takeIf { it.entityId in state.getBattlefield() }
                        ?.let { perm -> projected.getTypes(perm.entityId).filterTo(mutableSetOf()) { it in CARD_TYPE_NAMES } }
                        ?: emptySet()
                }
                val shared = typeSets.reduce { acc, next -> acc intersect next }
                if (shared.isEmpty()) {
                    return "Targets must share a card type"
                }
            }

            // "... with total mana value N or less" — the summed mana value of the chosen card
            // targets may not exceed the resolved cap (Fire Lord Sozin's "total mana value X or
            // less"; XValue resolves against the paid [xValue]). CR 601.2c. No-op for non-card
            // targets, which contribute 0.
            val totalManaCap = (requirement as? TargetObject)?.totalManaValueAtMost
            if (totalManaCap != null && targetsForReq.isNotEmpty()) {
                val cap = try {
                    DynamicAmountEvaluator().evaluate(
                        state,
                        totalManaCap,
                        EffectContext(sourceId = sourceId, controllerId = casterId, xValue = xValue)
                    ).coerceAtLeast(0)
                } catch (_: Exception) {
                    Int.MAX_VALUE
                }
                val summedManaValue = targetsForReq.sumOf { target ->
                    (target as? ChosenTarget.Card)?.let { card ->
                        state.getEntity(card.cardId)?.get<CardComponent>()?.manaValue ?: 0
                    } ?: 0
                }
                if (summedManaValue > cap) {
                    return "Targets must have total mana value $cap or less"
                }
            }

            // "... with different names" — no two chosen targets for this requirement may share a
            // name (Behold the Sinister Six!: "up to six target creature cards with different
            // names"). CR 601.2c. Grouped by projected name on the battlefield, base card name in
            // other zones (graveyard cards aren't projected).
            if (requirement is TargetObject && requirement.differentNames && targetsForReq.size > 1) {
                val names = targetsForReq.map { target ->
                    val id = (target as? ChosenTarget.Permanent)?.entityId
                        ?: (target as? ChosenTarget.Card)?.cardId
                    id?.let {
                        state.projectedState.getName(it) ?: state.getEntity(it)?.get<CardComponent>()?.name
                    }
                }
                if (names.size != names.toSet().size) {
                    return "Targets must have different names"
                }
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
        sourceId: EntityId? = null,
        xValue: Int? = null,
        allTargets: List<ChosenTarget> = emptyList()
    ): String? {
        // A separately-chosen player target (target index 0 for "target player's graveyard"
        // spells) — lets a later requirement's filter resolve `OwnedByTargetPlayer` /
        // `ControlledByTargetPlayer` against the player already chosen for this same cast
        // (Drafna's Restoration, Hurkyl's-Recall-family relational predicates).
        val chosenPlayerTarget = allTargets.firstNotNullOfOrNull { (it as? ChosenTarget.Player)?.playerId }
        val error = when (requirement) {
            is TargetPlayer -> validatePlayerTarget(state, target, requirement, casterId, sourceId)
            is TargetOpponent -> validateOpponentTarget(state, target, requirement, casterId, sourceId)
            is AnyTarget -> validateAnyTarget(state, target, casterId)
            is TargetCreatureOrPlayer -> validateCreatureOrPlayerTarget(state, target, casterId)
            is TargetOpponentOrPlaneswalker -> validateOpponentOrPlaneswalkerTarget(state, target, casterId)
            is TargetPlayerOrPlaneswalker -> validatePlayerOrPlaneswalkerTarget(state, target, casterId)
            is TargetCreatureOrPlaneswalker -> validateCreatureOrPlaneswalkerTarget(state, target)
            is TargetSpellOrPermanent -> validateSpellOrPermanentTarget(state, target, requirement, casterId, sourceId, xValue)
            is TargetObject -> validateObjectTarget(state, target, requirement.filter, casterId, sourceId, xValue, chosenPlayerTarget)
            is TargetOther -> validateSingleTarget(state, target, requirement.baseRequirement, casterId, sourceColors, sourceSubtypes, sourceId, xValue, allTargets)
        }
        if (error != null) return error

        // Check player-level protection, e.g. The One Ring's "protection from everything" (Rule 702.16).
        // A protected player can't be the target of a source matching one of its protection scopes.
        if (target is ChosenTarget.Player &&
            PlayerProtectionRules.isProtectedFromSource(state, target.playerId, sourceId, casterId)
        ) {
            return "Target player has protection from this source"
        }

        // Check hexproof and shroud on permanent targets (Rule 702.11, 702.18)
        val hexproofShroudError = checkHexproofAndShroud(state, target, casterId)
        if (hexproofShroudError != null) return hexproofShroudError

        // Check hexproof from color (Rule 702.11b)
        val hexproofError = checkHexproofFromColor(state, target, casterId, sourceColors)
        if (hexproofError != null) return hexproofError

        // Check hexproof from card type, e.g. "hexproof from instants" (Rule 702.11b).
        // Elenda, Saint of Dusk.
        val hexproofCardTypeError = checkHexproofFromCardType(state, target, casterId, sourceId)
        if (hexproofCardTypeError != null) return hexproofCardTypeError

        // Check protection from each opponent (Rule 702.16e)
        val protectionFromOpponentError = checkProtectionFromEachOpponent(state, target, casterId)
        if (protectionFromOpponentError != null) return protectionFromOpponentError

        // Check protection from supertype, e.g. "protection from legendary creatures" (Rule 702.16)
        val protectionFromSupertypeError = checkProtectionFromSupertype(state, target, sourceId)
        if (protectionFromSupertypeError != null) return protectionFromSupertypeError

        // Check protection from card type, e.g. "protection from instants and from sorceries"
        // (Rule 702.16). Sword of Wealth and Power.
        val protectionFromCardTypeError = checkProtectionFromCardType(state, target, sourceId)
        if (protectionFromCardTypeError != null) return protectionFromCardTypeError

        // "Can't be enchanted" (CR 303.4): an Aura can't legally target a permanent with the
        // CANT_BE_ENCHANTED restriction (Guardian Beast). Only applies when the source is an Aura.
        val cantBeEnchantedError = checkCantBeEnchanted(state, target, sourceId)
        if (cantBeEnchantedError != null) return cantBeEnchantedError

        // Check protection from color and creature subtype (Rule 702.16)
        return checkProtection(state, target, sourceColors, sourceSubtypes)
    }

    /**
     * Reject an Aura targeting a permanent that can't be enchanted (CR 303.4). No-op for
     * non-Aura sources and for non-permanent targets.
     *
     * Scope note: this only guards Aura *targeting* at cast/activation time, which covers the
     * common case (casting an Aura, or an ability that targets with an Aura on the stack). It does
     * NOT cover effects that move/attach an existing Aura without targeting (e.g. "attach target
     * Aura to ~", reanimate-an-Aura-attached). A card needing full coverage of the non-targeted
     * attachment cases (CR 303.4i for entering attached, CR 303.4j for re-attaching an on-battlefield
     * Aura) must also check [AbilityFlag.CANT_BE_ENCHANTED] at the attachment step.
     */
    private fun checkCantBeEnchanted(
        state: GameState,
        target: ChosenTarget,
        sourceId: EntityId?
    ): String? {
        val sourceIsAura = sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.typeLine?.isAura }
            ?: false
        if (!sourceIsAura) return null
        val targetId = (target as? ChosenTarget.Permanent)?.entityId ?: return null
        return if (state.projectedState.hasKeyword(targetId, AbilityFlag.CANT_BE_ENCHANTED)) {
            "That permanent can't be enchanted"
        } else null
    }

    /**
     * Check if a target has protection from one of the source's supertypes
     * (e.g. "protection from legendary creatures"). Format: PROTECTION_FROM_SUPERTYPE_<SUPERTYPE>.
     */
    private fun checkProtectionFromSupertype(
        state: GameState,
        target: ChosenTarget,
        sourceId: EntityId?
    ): String? {
        if (sourceId == null) return null
        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            else -> return null
        }
        if (entityId !in state.getBattlefield()) return null

        val projected = state.projectedState
        for (supertype in projected.getSupertypes(sourceId)) {
            if (projected.hasKeyword(entityId, "PROTECTION_FROM_SUPERTYPE_${supertype.uppercase()}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has protection from ${supertype.lowercase()} permanents"
            }
        }
        return null
    }

    /**
     * Check if a target has protection from one of the source's card types
     * (e.g. "protection from instants and from sorceries"). Format:
     * PROTECTION_FROM_CARDTYPE_<CARDTYPE>.
     *
     * The source's card types are read from its [CardComponent.typeLine] directly (not the
     * projected state), because the source is typically an instant/sorcery spell on the stack —
     * which the layer projector does not project. Card types of a spell aren't changed by
     * continuous effects in the cases this guards, so the printed type line is authoritative here.
     */
    private fun checkProtectionFromCardType(
        state: GameState,
        target: ChosenTarget,
        sourceId: EntityId?
    ): String? {
        if (sourceId == null) return null
        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            else -> return null
        }
        if (entityId !in state.getBattlefield()) return null

        val sourceCardTypes = state.getEntity(sourceId)
            ?.get<CardComponent>()?.typeLine?.cardTypes ?: return null
        val projected = state.projectedState
        for (cardType in sourceCardTypes) {
            if (projected.hasKeyword(entityId, "PROTECTION_FROM_CARDTYPE_${cardType.name}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has protection from ${cardType.displayName.lowercase()}s"
            }
        }
        return null
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
        if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != casterId &&
            !HexproofSuppression.isSuppressedForCaster(state, projected, entityId, casterId)
        ) {
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
        val hexproofSuppressed = HexproofSuppression.isSuppressedForCaster(state, projected, entityId, casterId)
        if (!hexproofSuppressed) {
            for (color in sourceColors) {
                if (projected.hasKeyword(entityId, "HEXPROOF_FROM_${color.name}")) {
                    val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                    return "$cardName has hexproof from ${color.displayName.lowercase()}"
                }
            }
            // Hexproof from monocolored: a source with exactly one color can't target (CR 105.2).
            if (sourceColors.size == 1 && projected.hasKeyword(entityId, "HEXPROOF_FROM_MONOCOLORED")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has hexproof from monocolored"
            }
        }
        return null
    }

    /**
     * Check if a target has hexproof from one of the source's card types — "hexproof from
     * instants" (Rule 702.11b). Format: `HEXPROOF_FROM_CARDTYPE_<CARDTYPE>`.
     *
     * Like hexproof from a color this only blocks *opponents*: the permanent's controller can
     * still target it with their own instants, and hexproof-suppressing effects (Glaring Spotlight
     * and friends) turn it off for the caster. The source's card types are resolved the same way as
     * for protection-from-card-type — projected types for a permanent source, falling back to the
     * printed type line for a spell on the stack, which the layer projector doesn't cover.
     */
    private fun checkHexproofFromCardType(
        state: GameState,
        target: ChosenTarget,
        casterId: EntityId,
        sourceId: EntityId?
    ): String? {
        if (sourceId == null) return null
        val entityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            else -> return null
        }
        if (entityId !in state.getBattlefield()) return null

        val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
        if (entityController == casterId) return null

        val projected = state.projectedState
        if (HexproofSuppression.isSuppressedForCaster(state, projected, entityId, casterId)) return null

        for (cardType in SourceTypeTargeting.sourceCardTypes(state, sourceId)) {
            if (projected.hasKeyword(entityId, "HEXPROOF_FROM_CARDTYPE_${cardType.uppercase()}")) {
                val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
                return "$cardName has hexproof from ${cardType.lowercase()}s"
            }
        }
        return null
    }

    /**
     * Check if a target has protection from each of the controller's opponents (Rule 702.16e).
     * Prevents being targeted by any spell or ability controlled by an opponent of the target's controller.
     */
    private fun checkProtectionFromEachOpponent(
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
        if (!projected.hasKeyword(entityId, "PROTECTION_FROM_EACH_OPPONENT")) return null

        val entityController = projected.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
        if (entityController == null || entityController == casterId) return null

        val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "target"
        return "$cardName has protection from each of your opponents"
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
        sourceId: EntityId? = null,
        xValue: Int? = null
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

        // "Another target …" — the source itself is not a legal target. Mirrors the same guard
        // in validateGraveyardTarget; enumeration also honors excludeSelf, but direct submission
        // must be rejected here too (Braided Net's "another target nonland permanent").
        if (filter.excludeSelf && sourceId != null && target.entityId == sourceId) {
            return "Target must be another permanent"
        }

        // Use unified filter with projection (face-down creatures have CMC 0 per Rule 708.2)
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = casterId, sourceId = sourceId, xValue = xValue)
        val matches = predicateEvaluator.matches(state, projected, target.entityId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    private fun validatePlayerTarget(
        state: GameState,
        target: ChosenTarget,
        requirement: TargetPlayer,
        casterId: EntityId,
        sourceId: EntityId?
    ): String? {
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
        // CR 608.2b: a target illegal at resolution is removed. Re-checking the restriction
        // here covers both cast-time validation and the resolution-time re-validation.
        if (!PlayerTargetRestriction.isSatisfied(state, requirement.restriction, target.playerId, casterId, sourceId)) {
            return "Target player does not match: ${requirement.description}"
        }
        return null
    }

    private fun validateOpponentTarget(
        state: GameState,
        target: ChosenTarget,
        requirement: TargetOpponent,
        casterId: EntityId,
        sourceId: EntityId?
    ): String? {
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
        if (!PlayerTargetRestriction.isSatisfied(state, requirement.restriction, target.playerId, casterId, sourceId)) {
            return "Target player does not match: ${requirement.description}"
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
                // Face-down permanents are always creatures (Rule 708.2)
                if (!state.projectedState.isCreature(target.entityId) && !container.has<FaceDownComponent>()) {
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
        val projected = state.projectedState
        val isPlaneswalker = projected.isPlaneswalker(target.entityId) ||
            CardType.PLANESWALKER in cardComponent.typeLine.cardTypes
        // Face-down permanents are always creatures (Rule 708.2)
        val isFaceDown = container.has<FaceDownComponent>()
        if (!projected.isCreature(target.entityId) && !isPlaneswalker && !isFaceDown) {
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
        sourceId: EntityId? = null,
        xValue: Int? = null,
        targetPlayerId: EntityId? = null
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

        // Use unified filter - OwnedByYou predicate handles "your graveyard" restriction; a
        // separately-chosen player target (Drafna's Restoration) flows in via targetPlayerId so
        // OwnedByTargetPlayer matches "cards in target player's graveyard".
        val predicateContext = PredicateContext(controllerId = casterId, ownerId = target.ownerId, sourceId = sourceId, xValue = xValue, targetPlayerId = targetPlayerId)
        val matches = predicateEvaluator.matches(state, state.projectedState, target.cardId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    private fun validateSpellTarget(
        state: GameState,
        target: ChosenTarget,
        filter: TargetFilter,
        casterId: EntityId,
        xValue: Int? = null,
        sourceId: EntityId? = null
    ): String? {
        if (target !is ChosenTarget.Spell) {
            return "Target must be a spell on the stack"
        }
        if (target.spellEntityId !in state.stack) {
            return "Target spell not on the stack"
        }

        // Use unified filter with projected state (face-down spells need projection to be seen as
        // creatures); sourceId lets source-relative predicates evaluate (Goblin Artisans'
        // NotTargetedByAbilityFromSameNamedSource).
        val predicateContext = PredicateContext(controllerId = casterId, sourceId = sourceId, xValue = xValue)
        val matches = predicateEvaluator.matches(state, state.projectedState, target.spellEntityId, filter.baseFilter, predicateContext)
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
        sourceId: EntityId? = null,
        xValue: Int? = null,
        targetPlayerId: EntityId? = null
    ): String? {
        // Cross-zone union: the target is legal if it satisfies *any* clause. Validate against each
        // single-zone clause; succeed on the first that accepts, otherwise report that clause's
        // error (the most informative — the target type matched a clause's zone but failed its
        // filter). Each clause has no alternatives, so this recursion terminates.
        if (filter.isUnion) {
            val clauseErrors = filter.clauses().map { clause ->
                validateObjectTarget(state, target, clause, casterId, sourceId, xValue, targetPlayerId)
            }
            if (clauseErrors.any { it == null }) return null
            return clauseErrors.firstOrNull { it != null }
                ?: "Target does not match filter: ${filter.description}"
        }
        return when (filter.zone) {
            Zone.GRAVEYARD -> validateGraveyardTarget(state, target, filter, casterId, sourceId, xValue, targetPlayerId)
            Zone.BATTLEFIELD -> validatePermanentTarget(state, target, filter, casterId, sourceId, xValue)
            Zone.STACK -> validateSpellTarget(state, target, filter, casterId, xValue, sourceId)
            else -> validateCardInZoneTarget(state, target, filter, casterId, xValue, sourceId)
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
        sourceId: EntityId?,
        xValue: Int? = null
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
                    val context = PredicateContext(controllerId = casterId, sourceId = sourceId, xValue = xValue)
                    if (!predicateEvaluator.matches(state, projected, target.entityId, filter, context)) {
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
        casterId: EntityId,
        xValue: Int? = null,
        sourceId: EntityId? = null
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

        // sourceId lets source-relative predicates evaluate (e.g. ExiledWithSource — "target card
        // exiled with ~"). Mirrors the graveyard/spell validation paths.
        val predicateContext = PredicateContext(controllerId = casterId, ownerId = target.ownerId, xValue = xValue, sourceId = sourceId)
        val matches = predicateEvaluator.matches(state, state.projectedState, target.cardId, filter.baseFilter, predicateContext)
        if (!matches) {
            return "Target does not match filter: ${filter.description}"
        }
        return null
    }

    /**
     * Check if a player has shroud (e.g., from True Believer's "You have shroud"
     * or Gilded Light's "You gain shroud until end of turn").
     */
    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean =
        ControllerShroud.appliesTo(state, playerId)

    private fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean =
        ControllerHexproof.appliesTo(state, playerId)

    private fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, casterId: EntityId): Boolean {
        return playerId != casterId && playerHasHexproof(state, playerId)
    }
}
