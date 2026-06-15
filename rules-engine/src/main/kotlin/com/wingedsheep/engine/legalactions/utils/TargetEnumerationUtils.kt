package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.mechanics.targeting.HexproofSuppression
import com.wingedsheep.engine.mechanics.targeting.PlayerTargetRestriction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Extracted target-finding helpers from LegalActionsCalculator.
 * These methods find valid targets for spells, abilities, and effects.
 */
class TargetEnumerationUtils(
    private val predicateEvaluator: PredicateEvaluator
) {
    fun findValidTargets(
        state: GameState,
        playerId: EntityId,
        requirement: TargetRequirement,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproofAgainst(state, it, playerId) && !playerHasProtectionFrom(state, it, sourceId, playerId) &&
                PlayerTargetRestriction.isSatisfied(state, requirement.restriction, it, playerId, sourceId) }
            is TargetOpponent -> state.turnOrder.filter { it != playerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproof(state, it) && !playerHasProtectionFrom(state, it, sourceId, playerId) &&
                PlayerTargetRestriction.isSatisfied(state, requirement.restriction, it, playerId, sourceId) }
            is AnyTarget -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) && !playerHasProtectionFrom(state, it, sourceId, playerId) }
                (creatures + planeswalkers).distinct() + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) && !playerHasProtectionFrom(state, it, sourceId, playerId) }
                creatures + players
            }
            is TargetPlayerOrPlaneswalker -> {
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) && !playerHasProtectionFrom(state, it, sourceId, playerId) }
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                players + planeswalkers
            }
            is TargetOpponentOrPlaneswalker -> {
                val opponents = state.turnOrder.filter { it != playerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproof(state, it) && !playerHasProtectionFrom(state, it, sourceId, playerId) }
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                opponents + planeswalkers
            }
            is TargetCreatureOrPlaneswalker -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                creatures + planeswalkers
            }
            is TargetObject -> findValidObjectTargets(state, playerId, requirement.filter, sourceId)
            is TargetOther -> {
                val baseTargets = findValidTargets(state, playerId, requirement.baseRequirement, sourceId)
                val excludeId = resolveOtherExclusion(state, requirement, sourceId)
                if (excludeId != null) baseTargets.filter { it != excludeId } else baseTargets
            }
            is TargetSpellOrPermanent -> {
                val permanentFilter = requirement.permanentFilter
                val permanents = if (permanentFilter == null) {
                    findValidPermanentTargets(state, playerId, TargetFilter.Permanent, sourceId)
                } else {
                    val projected = state.projectedState
                    val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
                    state.getBattlefield().filter { entityId ->
                        val container = state.getEntity(entityId) ?: return@filter false
                        val entityController = container.get<ControllerComponent>()?.playerId
                        if (projected.hasKeyword(entityId, Keyword.HEXPROOF) &&
                            entityController != playerId &&
                            !HexproofSuppression.isSuppressedForCaster(state, projected, entityId, playerId)
                        ) return@filter false
                        if (projected.hasKeyword(entityId, Keyword.SHROUD)) return@filter false
                        predicateEvaluator.matches(state, projected, entityId, permanentFilter, context)
                    }
                }
                val spells = findValidSpellTargets(state, playerId, TargetFilter.SpellOnStack)
                permanents + spells
            }
        }
    }

    /**
     * Which entity a [TargetOther] requirement excludes: an explicit [TargetOther.excludeSourceId],
     * else the source's attached creature when [TargetOther.excludeAttachedCreature] is set
     * ("enchanted creature deals damage … to any other target"), else the source itself.
     */
    private fun resolveOtherExclusion(
        state: GameState,
        requirement: TargetOther,
        sourceId: EntityId?
    ): EntityId? = requirement.excludeSourceId
        ?: if (requirement.excludeAttachedCreature) {
            sourceId?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }
        } else {
            sourceId
        }

    fun shouldAutoSelectPlayerTarget(
        requirement: TargetRequirement,
        validTargets: List<EntityId>
    ): Boolean {
        val isPlayerTarget = requirement is TargetPlayer || requirement is TargetOpponent
        val requiresExactlyOne = requirement.count == 1 && requirement.effectiveMinCount == 1
        val hasExactlyOneChoice = validTargets.size == 1
        return isPlayerTarget && requiresExactlyOne && hasExactlyOneChoice
    }

    fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return battlefield.filter { entityId ->
            if (filter.excludeSelf && entityId == sourceId) return@filter false
            val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != playerId &&
                !HexproofSuppression.isSuppressedForCaster(state, projected, entityId, playerId)
            ) return@filter false
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter.baseFilter, context)
        }
    }

    fun findValidGraveyardTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val playerIds = if (filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou) {
            listOf(playerId)
        } else {
            state.turnOrder.toList()
        }
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return playerIds.flatMap { pid ->
            state.getGraveyard(pid).filter { entityId ->
                if (filter.excludeSelf && entityId == sourceId) return@filter false
                predicateEvaluator.matches(state, state.projectedState, entityId, filter.baseFilter, context)
            }
        }
    }

    fun findValidObjectTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findValidPermanentTargets(state, playerId, filter, sourceId)
            Zone.GRAVEYARD -> findValidGraveyardTargets(state, playerId, filter, sourceId)
            Zone.EXILE -> findValidExileTargets(state, playerId, filter, sourceId)
            Zone.STACK -> findValidSpellTargets(state, playerId, filter)
            else -> emptyList()
        }
    }

    /**
     * Enumerate exile-zone targets. Mirrors [findValidGraveyardTargets]: when the filter
     * restricts to cards you own/control, only scan the targeting player's exile sub-zone;
     * otherwise scan every player's exile. Used for cards that target cards in exile
     * directly — e.g., Blade of the Swarm's "target exiled card with warp" mode.
     */
    fun findValidExileTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val playerIds = when (filter.baseFilter.controllerPredicate) {
            ControllerPredicate.ControlledByYou, ControllerPredicate.OwnedByYou -> listOf(playerId)
            else -> state.turnOrder.toList()
        }
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return playerIds.flatMap { pid ->
            state.getExile(pid).filter { entityId ->
                if (filter.excludeSelf && entityId == sourceId) return@filter false
                predicateEvaluator.matches(state, state.projectedState, entityId, filter.baseFilter, context)
            }
        }
    }

    fun findValidSpellTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = playerId)
        return state.stack.filter { spellId ->
            // "Target spell" only matches actual spells — never triggered/activated abilities on
            // the stack. A spell is a card on the stack (CR 112.1); an ability on the stack is an
            // ability, not a spell (CR 113.3b/c, 113.7a). The base filter is `Any` (zone = STACK)
            // and would otherwise pass ability entities, surfacing a castable counterspell whenever
            // anything is on the stack — which previously left the AI in an infinite re-pick loop.
            state.isSpellOnStack(spellId) &&
                predicateEvaluator.matches(state, state.projectedState, spellId, filter.baseFilter, context)
        }
    }

    fun getTargetZone(requirement: TargetRequirement): String? {
        return when (requirement) {
            is TargetObject -> requirement.filter.zone.takeIf { it != Zone.BATTLEFIELD }?.let {
                when (requirement.filter.zone) {
                    Zone.GRAVEYARD -> "Graveyard"
                    Zone.STACK -> "Stack"
                    Zone.EXILE -> "Exile"
                    Zone.HAND -> "Hand"
                    Zone.LIBRARY -> "Library"
                    Zone.COMMAND -> "Command"
                    else -> null
                }
            }
            else -> null
        }
    }

    fun buildTargetInfos(
        state: GameState,
        playerId: EntityId,
        targetReqs: List<TargetRequirement>,
        sourceId: EntityId? = null
    ): List<TargetInfo> {
        return targetReqs.mapIndexed { index, req ->
            val validTargets = findValidTargets(state, playerId, req, sourceId)
            TargetInfo(
                index = index,
                description = req.description,
                minTargets = req.effectiveMinCount,
                // "Any number of target ..." has no fixed cap; the real maximum is the
                // number of legal targets on the board, so the client offers all of them.
                // A board-state `dynamicMaxCount` (anything but XValue, which is unbound
                // until the X is chosen and so is surfaced via [xConstrainsCount]) is
                // resolved here so the UI caps at the right number immediately.
                maxTargets = when {
                    req.unlimited -> validTargets.size
                    else -> resolveStaticDynamicMax(state, req, playerId, sourceId) ?: req.count
                },
                validTargets = validTargets,
                targetZone = getTargetZone(req),
                xConstrainsManaValue = requirementUsesManaValueAtMostX(req),
                xConstrainsPower = requirementUsesPowerEqualsX(req),
                xConstrainsCount = requirementXConstrainsCount(req)
            )
        }
    }

    /**
     * Resolve a [TargetObject.dynamicMaxCount] that is knowable at enumeration time —
     * i.e. any [DynamicAmount] except [DynamicAmount.XValue] (which depends on the X the
     * player hasn't chosen yet and is instead clamped client-side via [requirementXConstrainsCount]).
     * Returns null when there is no dynamic cap, so callers fall back to the static `count`.
     */
    private fun resolveStaticDynamicMax(
        state: GameState,
        req: TargetRequirement,
        playerId: EntityId,
        sourceId: EntityId?
    ): Int? {
        val dyn = (req as? TargetObject)?.dynamicMaxCount ?: return null
        if (dyn == DynamicAmount.XValue) return null
        return try {
            val context = EffectContext(
                sourceId = sourceId,
                controllerId = playerId,
            )
            DynamicAmountEvaluator().evaluate(state, dyn, context).coerceAtLeast(0)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True when [requirement] is a [TargetObject] whose filter contains
     * [CardPredicate.ManaValueAtMostX] (anywhere in the predicate tree).
     *
     * Surfaced to the client so it can re-filter [TargetInfo.validTargets] by the
     * chosen X after X selection — the enumerator's list is permissive (X is unbound
     * at enumeration time) and would otherwise let the player click an over-MV
     * target that the server then rejects on cast.
     */
    /**
     * True when [requirement] is a [TargetObject] whose `dynamicMaxCount` is the
     * [DynamicAmount.XValue] sentinel. Surfaced to the client so the targeting UI
     * caps selectable targets at the X chosen for the spell's cost.
     */
    fun requirementXConstrainsCount(requirement: TargetRequirement): Boolean {
        val target = requirement as? TargetObject ?: return false
        return target.dynamicMaxCount == DynamicAmount.XValue
    }

    fun requirementUsesManaValueAtMostX(requirement: TargetRequirement): Boolean {
        val filter = (requirement as? TargetObject)?.filter ?: return false
        return filter.baseFilter.cardPredicates.any { containsManaValueAtMostX(it) }
    }

    private fun containsManaValueAtMostX(predicate: CardPredicate): Boolean = when (predicate) {
        CardPredicate.ManaValueAtMostX -> true
        is CardPredicate.And -> predicate.predicates.any { containsManaValueAtMostX(it) }
        is CardPredicate.Or -> predicate.predicates.any { containsManaValueAtMostX(it) }
        is CardPredicate.Not -> containsManaValueAtMostX(predicate.predicate)
        else -> false
    }

    /**
     * True when [requirement] is a [TargetObject] whose filter contains
     * [CardPredicate.PowerEqualsX] (anywhere in the predicate tree). Surfaced to the client
     * so it re-filters the permissive enumeration down to creatures whose power equals the
     * chosen X after X selection (Ent-Draught Basin).
     */
    fun requirementUsesPowerEqualsX(requirement: TargetRequirement): Boolean {
        val filter = (requirement as? TargetObject)?.filter ?: return false
        return filter.baseFilter.cardPredicates.any { containsPowerEqualsX(it) }
    }

    private fun containsPowerEqualsX(predicate: CardPredicate): Boolean = when (predicate) {
        CardPredicate.PowerEqualsX -> true
        is CardPredicate.And -> predicate.predicates.any { containsPowerEqualsX(it) }
        is CardPredicate.Or -> predicate.predicates.any { containsPowerEqualsX(it) }
        is CardPredicate.Not -> containsPowerEqualsX(predicate.predicate)
        else -> false
    }

    fun allRequirementsSatisfied(targetInfos: List<TargetInfo>): Boolean {
        return targetInfos.all { it.validTargets.isNotEmpty() || it.minTargets == 0 }
    }

    // Player protection checks

    fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerShroudComponent>() == true) return true
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerHexproofComponent>() == true) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerHexproofComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, controllerId: EntityId): Boolean {
        return playerId != controllerId && playerHasHexproof(state, playerId)
    }

    /**
     * True if [playerId] is protected from a source [sourceId] controlled by [casterId]
     * (CR 702.16) — so the source can't legally target that player (The One Ring).
     * Delegates to the shared [PlayerProtectionRules] used by the targeting validator and
     * damage executor.
     */
    fun playerHasProtectionFrom(state: GameState, playerId: EntityId, sourceId: EntityId?, casterId: EntityId): Boolean =
        com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
            .isProtectedFromSource(state, playerId, sourceId, casterId)
}
