package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.targeting.ControllerHexproof
import com.wingedsheep.engine.mechanics.targeting.ControllerShroud
import com.wingedsheep.engine.mechanics.targeting.PlayerTargetRestriction
import com.wingedsheep.engine.mechanics.targeting.StackObjectTargeting
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Identifies the type of source that is doing the targeting.
 * Used to implement restrictions like "can't be the target of abilities your opponents control"
 * which only block abilities, not spells.
 */
enum class TargetingSourceType {
    /** The source is a spell (instant/sorcery/aura/etc.) */
    SPELL,
    /** The source is an activated or triggered ability */
    ABILITY,
    /** Unknown or default — no source-type-based restrictions apply */
    ANY
}

/**
 * Finds legal targets for a given target requirement.
 *
 * This class evaluates a TargetRequirement against the current game state
 * and returns a list of valid target EntityIds.
 */
class TargetFinder(
) {
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Build the per-candidate [PredicateContext] for filter evaluation, folding in any
     * pipeline-derived fields (storedCollections, chosenValues, xValue, …) carried by
     * [pipelineContext]. Keeps the always-present `controllerId`/`sourceId`/`ownerId` from
     * the call site while letting resolution-time filters see the resolving effect's pipeline
     * state — needed for "power <= the amassed Army's power" (EntityReference.AmassedArmy).
     */
    private fun targetingContext(
        controllerId: EntityId,
        sourceId: EntityId? = null,
        ownerId: EntityId? = null,
        triggeringEntityId: EntityId? = null,
        pipelineContext: PredicateContext? = null
    ): PredicateContext =
        (pipelineContext ?: PredicateContext(controllerId = controllerId)).copy(
            controllerId = controllerId,
            sourceId = sourceId,
            ownerId = ownerId,
            // Carry the trigger's associated entity so a target filter can scope to "that player"
            // (ControllerPredicate.ControlledByTriggeringPlayer / OwnedByTriggeringPlayer) — e.g.
            // Dreadmaw's Ire's "destroy target artifact that player controls". Only overrides when a
            // triggering entity is supplied, so a pipeline-derived context keeps its own value.
            triggeringEntityId = triggeringEntityId ?: pipelineContext?.triggeringEntityId
        )

    /**
     * Find all legal targets for a given requirement.
     *
     * @param state The current game state
     * @param requirement The target requirement to satisfy
     * @param controllerId The player who is choosing targets (for "you control" filters)
     * @param sourceId The source of the targeting ability (to exclude "other" targets)
     * @param ignoreTargetingRestrictions If true, hexproof and shroud are bypassed.
     *   Use for aura attachment (Rule 303.4f): when an aura enters the battlefield without
     *   being cast, the controller chooses what it enchants — normal targeting restrictions
     *   like hexproof and shroud do not apply.
     * @return List of valid target EntityIds
     */
    fun findLegalTargets(
        state: GameState,
        requirement: TargetRequirement,
        controllerId: EntityId,
        sourceId: EntityId? = null,
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        triggeringEntityId: EntityId? = null,
        /**
         * Pipeline-derived predicate context (storedCollections, chosenValues, xValue, …) from the
         * resolving effect. Threaded so a target filter can compare candidates against a
         * resolution-time pipeline value — e.g. "power <= the amassed Army's power" reads
         * [EntityReference.AmassedArmy] out of `pipelineContext.storedCollections`. Null for
         * cast-time targeting where no pipeline state exists yet.
         */
        pipelineContext: PredicateContext? = null
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> findPlayerTargets(state, requirement, controllerId, sourceId)
            is TargetOpponent -> findOpponentTargets(state, requirement, controllerId, sourceId)
            is AnyTarget -> findAnyTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetCreatureOrPlayer -> findCreatureOrPlayerTargets(state, controllerId, sourceId, targetingSourceType, pipelineContext)
            is TargetOpponentOrPlaneswalker -> findOpponentOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetPlayerOrPlaneswalker -> findPlayerOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetCreatureOrPlaneswalker -> findCreatureOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetObject -> findObjectTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType, triggeringEntityId, pipelineContext)
            is TargetSpellOrPermanent -> findSpellOrPermanentTargets(state, requirement, controllerId, sourceId, targetingSourceType)
            is TargetOther -> {
                // For TargetOther, find targets for the base requirement but exclude the source
                // (or, for "enchanted creature deals damage to any other target", the attached creature).
                val baseTargets = findLegalTargets(state, requirement.baseRequirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType, triggeringEntityId, pipelineContext)
                val excludeId = requirement.excludeSourceId
                    ?: if (requirement.excludeAttachedCreature) {
                        sourceId?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }
                    } else {
                        sourceId
                    }
                if (excludeId != null) baseTargets.filter { it != excludeId } else baseTargets
            }
        }
    }

    private fun findPlayerTargets(
        state: GameState,
        requirement: TargetPlayer,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        return state.turnOrder.filter { playerId ->
            state.hasEntity(playerId) && !playerHasShroud(state, playerId) &&
                !playerHasHexproofAgainst(state, playerId, controllerId) &&
                PlayerTargetRestriction.isSatisfied(state, requirement.restriction, playerId, controllerId, sourceId)
        }
    }

    private fun findOpponentTargets(
        state: GameState,
        requirement: TargetOpponent,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        return state.turnOrder.filter { it != controllerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproof(state, it) &&
            PlayerTargetRestriction.isSatisfied(state, requirement.restriction, it, controllerId, sourceId) }
    }

    /**
     * Check if a permanent is restricted from being targeted by the given source type.
     * Checks for CantBeTargetedByOpponentAbilitiesComponent — which blocks opponent abilities
     * but not opponent spells.
     */
    private fun hasCantBeTargetedRestriction(
        state: GameState,
        entityId: EntityId,
        entityController: EntityId?,
        controllerId: EntityId,
        targetingSourceType: TargetingSourceType,
        sourceId: EntityId? = null
    ): Boolean {
        // Source-card-type restriction (Artifact Ward) is checked first because, unlike the
        // opponent-ability restriction, it is NOT controller-gated: a matching source can't target
        // the warded creature even if the same player controls both. It still only blocks abilities
        // (not spells); the helper handles the spell/unknown-source short-circuit.
        if (SourceTypeTargeting.cantBeTargetedBySourceTypeAbility(state, entityId, sourceId, targetingSourceType)) {
            return true
        }

        if (entityController == controllerId) return false  // own permanents are never restricted
        if (targetingSourceType == TargetingSourceType.SPELL) return false  // spells bypass this restriction

        // For ABILITY source type, always blocked. For ANY (unknown), conservatively block since
        // we don't know the source type. Read through ControllerGrants so a gated form of the
        // ability switches off with its condition instead of sticking on.
        return ControllerGrants.isActiveOn<CantBeTargetedByOpponentAbilitiesComponent>(state, entityId)
    }

    private fun findOpponentOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add opponents (excluding those with shroud or hexproof)
        targets.addAll(state.turnOrder.filter { it != controllerId && state.hasEntity(it) &&
            !playerHasShroud(state, it) && !playerHasHexproof(state, it) })

        // Add all planeswalkers on the battlefield
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            if (!container.has<CardComponent>()) continue
            val entityController = container.get<ControllerComponent>()?.playerId

            // Read the PROJECTED type line, not the printed one, so a permanent that
            // becomes a planeswalker via a continuous effect is offered (CR 115.4 /
            // projection rule), consistent with findAnyTargets.
            if (!projected.isPlaneswalker(entityId)) continue

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) continue
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) continue

            targets.add(entityId)
        }

        return targets
    }

    private fun findPlayerOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

        // Add all planeswalkers on the battlefield
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            if (!container.has<CardComponent>()) continue
            val entityController = container.get<ControllerComponent>()?.playerId

            // Read the PROJECTED type line, not the printed one, so a permanent that
            // becomes a planeswalker via a continuous effect is offered (CR 115.4 /
            // projection rule), consistent with findAnyTargets.
            if (!projected.isPlaneswalker(entityId)) continue

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) continue
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) continue

            targets.add(entityId)
        }

        return targets
    }

    private fun findPermanentTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId,
        sourceId: EntityId?,
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        triggeringEntityId: EntityId? = null,
        pipelineContext: PredicateContext? = null
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val filter = requirement.filter

        return battlefield.filter { entityId ->
            // Exclude self if filter says "other"
            if (filter.excludeSelf && entityId == sourceId) {
                return@filter false
            }
            // Exclude the trigger's triggering entity (e.g., "other than that creature"
            // for Pawpatch-style triggers where "that creature" is the targeted permanent).
            if (filter.excludeTriggeringEntity && triggeringEntityId != null && entityId == triggeringEntityId) {
                return@filter false
            }

            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            if (!ignoreTargetingRestrictions) {
                // Check hexproof/shroud
                if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                    return@filter false
                }
                if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                    return@filter false
                }
                // Check hexproof from color
                if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) {
                    return@filter false
                }
                // Check can't-be-targeted-by-abilities
                if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) {
                    return@filter false
                }
            }

            // Use unified filter with projected state
            val predicateContext = targetingContext(controllerId, sourceId, triggeringEntityId = triggeringEntityId, pipelineContext = pipelineContext)
            predicateEvaluator.matches(state, projected, entityId, filter.baseFilter, predicateContext)
        }
    }

    private fun findAnyTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

        // Add all creatures, planeswalkers and battles
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            if (!container.has<CardComponent>()) continue
            val entityController = container.get<ControllerComponent>()?.playerId

            // CR 115.4 — "any target" means a creature, player, planeswalker, or battle, and
            // nothing else. Read the PROJECTED type line, not the printed one, so animated lands
            // (Earthbend) and face-down 2/2 creatures are valid targets (projection rule).
            if (!projected.isCreature(entityId) &&
                !projected.isPlaneswalker(entityId) &&
                !projected.isBattle(entityId)
            ) {
                continue
            }

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                continue
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                continue
            }
            // Check hexproof from color
            if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) {
                continue
            }
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) {
                continue
            }

            targets.add(entityId)
        }

        return targets
    }

    private fun findCreatureOrPlayerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        pipelineContext: PredicateContext? = null
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

        // Add all creatures
        targets.addAll(findPermanentTargets(state, TargetCreature(), controllerId, sourceId, targetingSourceType = targetingSourceType, pipelineContext = pipelineContext))

        return targets
    }

    private fun findCreatureOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            if (!container.has<CardComponent>()) return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Must be creature or planeswalker. Read the PROJECTED type line, not the
            // printed one, so animated lands (Earthbend) and face-down 2/2 creatures
            // are valid targets (projection rule, see CR 115.4).
            if (!projected.isCreature(entityId) && !projected.isPlaneswalker(entityId)) {
                return@filter false
            }

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }
            // Check hexproof from color
            if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) {
                return@filter false
            }
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) {
                return@filter false
            }

            true
        }
    }

    private fun findGraveyardTargets(
        state: GameState,
        filter: TargetFilter,
        controllerId: EntityId,
        sourceId: EntityId?,
        pipelineContext: PredicateContext? = null
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Check all graveyards - the unified filter's OwnedByYou predicate handles "your graveyard" restriction
        for (playerId in state.turnOrder) {
            val graveyardKey = ZoneKey(playerId, Zone.GRAVEYARD)
            val graveyard = state.getZone(graveyardKey)

            for (cardId in graveyard) {
                if (filter.excludeSelf && cardId == sourceId) continue
                val predicateContext = targetingContext(controllerId, sourceId, ownerId = playerId, pipelineContext = pipelineContext)
                if (predicateEvaluator.matches(state, state.projectedState, cardId, filter.baseFilter, predicateContext)) {
                    targets.add(cardId)
                }
            }
        }

        return targets
    }

    private fun findSpellTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId
    ): List<EntityId> {
        val filter = requirement.filter
        val predicateContext = PredicateContext(controllerId = controllerId)
        // Whether this requirement is allowed to target *abilities* on the stack, not just spells.
        // "Target spell" (the common case, base filter `Any`) must never reach an ability — a spell
        // is a card on the stack (CR 112.1) while an ability on the stack is a separate object kind
        // (CR 113.3b/c, 113.7a). So an ability entity is offered only when the filter *explicitly*
        // names an ability predicate (Stifle's "counter target ability", Willbender's "spell or
        // ability", Return the Favor's "spell or ability"). For spells the predicate decides as
        // before. This is the single seam where both spells and abilities become legal targets.
        val abilitiesAllowed = StackObjectTargeting.permitsAbilities(filter.baseFilter)
        return state.stack.filter { stackId ->
            val isAbility = !state.isSpellOnStack(stackId)
            if (isAbility && !abilitiesAllowed) return@filter false
            predicateEvaluator.matches(state, state.projectedState, stackId, filter.baseFilter, predicateContext)
        }
    }

    /**
     * Find targets for TargetObject, dispatching based on the filter's zone.
     */
    private fun findObjectTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId,
        sourceId: EntityId?,
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        triggeringEntityId: EntityId? = null,
        pipelineContext: PredicateContext? = null
    ): List<EntityId> {
        val filter = requirement.filter
        // Cross-zone union ("from your graveyard or exiled card with flashback"): the legal set is
        // the union over each single-zone clause. Recurse per clause (each has no alternatives, so
        // this terminates) and dedupe — a single object can't legally match two clauses anyway, but
        // distinct() guards against overlapping filters.
        if (filter.isUnion) {
            return filter.clauses().flatMap { clause ->
                findObjectTargets(state, requirement.copy(filter = clause), controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType, triggeringEntityId, pipelineContext)
            }.distinct()
        }
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findPermanentTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType, triggeringEntityId, pipelineContext)
            Zone.GRAVEYARD -> findGraveyardTargets(state, filter, controllerId, sourceId, pipelineContext)
            Zone.STACK -> findSpellTargets(state, requirement, controllerId)
            else -> findCardTargetsInZone(state, filter, controllerId)
        }
    }

    /**
     * Find targets that are either permanents on the battlefield or spells on the stack.
     * Used by Artificial Evolution's "target spell or permanent" requirement.
     */
    private fun findSpellOrPermanentTargets(
        state: GameState,
        requirement: TargetSpellOrPermanent,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = sourceId)
        val permanentFilter = requirement.permanentFilter

        // Add all permanents on the battlefield matching the optional filter
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSource(state, projected, entityId, entityController, controllerId, sourceId)) continue
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType, sourceId)) continue

            if (permanentFilter != null &&
                !predicateEvaluator.matches(state, projected, entityId, permanentFilter, predicateContext)
            ) continue

            targets.add(entityId)
        }

        // Add all spells on the stack — only actual spells (CR 112.1), never abilities
        // on the stack (CR 113.3b/c, 113.7a), consistent with findSpellTargets above.
        targets.addAll(state.stack.filter { spellId -> state.isSpellOnStack(spellId) })

        return targets
    }

    /**
     * Check if a player has shroud (e.g., from True Believer's "You have shroud"
     * or Gilded Light's "You gain shroud until end of turn").
     */
    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean =
        ControllerShroud.appliesTo(state, playerId)

    /**
     * Check if a player has hexproof (from a permanent like Shalai, Voice of Plenty).
     * Unlike shroud, hexproof only prevents opponents from targeting — the player can still
     * target themselves.
     */
    private fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean =
        ControllerHexproof.appliesTo(state, playerId)

    /**
     * Check if a player has hexproof against a specific controller.
     * Returns true if the player has hexproof AND the controller is an opponent.
     */
    private fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, controllerId: EntityId): Boolean {
        return playerId != controllerId && playerHasHexproof(state, playerId)
    }

    /**
     * Check if a permanent has "hexproof from [quality]" matching the targeting source — either one
     * of its colors ("hexproof from white") or one of its card types ("hexproof from instants").
     * Rule 702.11b: opponents can't target it with spells/abilities of that quality.
     *
     * Gets the source's colors/types from projected state (for battlefield permanents) and falls
     * back to the base [CardComponent] (for spells in hand/on the stack, which aren't projected).
     *
     * @return true if the entity is protected by hexproof-from against the source
     */
    private fun hasHexproofFromSource(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        entityController: EntityId?,
        controllerId: EntityId,
        sourceId: EntityId?
    ): Boolean {
        if (entityController == controllerId || sourceId == null) return false
        // Try projected colors first (for permanents on the battlefield),
        // then fall back to base CardComponent colors (for spells in hand/on stack)
        var sourceColors = projected.getColors(sourceId)
        if (sourceColors.isEmpty()) {
            sourceColors = state.getEntity(sourceId)?.get<CardComponent>()
                ?.colors?.map { it.name }?.toSet() ?: emptySet()
        }
        if (sourceColors.any { colorName -> projected.hasKeyword(entityId, "HEXPROOF_FROM_$colorName") }) {
            return true
        }
        // Hexproof from monocolored: a source with exactly one color can't target (CR 105.2).
        if (sourceColors.size == 1 && projected.hasKeyword(entityId, "HEXPROOF_FROM_MONOCOLORED")) {
            return true
        }
        return SourceTypeTargeting.sourceCardTypes(state, sourceId).any { cardType ->
            projected.hasKeyword(entityId, "HEXPROOF_FROM_CARDTYPE_${cardType.uppercase()}")
        }
    }

    /**
     * Find card targets in non-battlefield, non-stack zones (hand, library, exile, command).
     */
    private fun findCardTargetsInZone(
        state: GameState,
        filter: TargetFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val zoneType = filter.zone
        val targets = mutableListOf<EntityId>()

        for (playerId in state.turnOrder) {
            val zoneKey = ZoneKey(playerId, zoneType)
            val zone = state.getZone(zoneKey)

            for (cardId in zone) {
                val predicateContext = PredicateContext(controllerId = controllerId, ownerId = playerId)
                if (predicateEvaluator.matches(state, state.projectedState, cardId, filter.baseFilter, predicateContext)) {
                    targets.add(cardId)
                }
            }
        }

        return targets
    }
}
