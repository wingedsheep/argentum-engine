package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * Candidate pools and client cost data for an additional cost that the caster pays by **selecting
 * objects** — sacrifice, discard, tap, bounce, exile from a zone, behold, or a sum-gated exile from
 * the graveyard.
 *
 * Four cast-time shapes need exactly this, off the same cost vocabulary: the non-mana leg of an
 * "… or pay {N}" cost ([AdditionalCost.OrPay]), escalate whose cost isn't mana
 * ([com.wingedsheep.sdk.scripting.effects.ModalEffect.additionalCostPerExtraMode]), a card's own
 * [com.wingedsheep.sdk.scripting.SelfAlternativeCost] non-mana half, and the same half on a
 * battlefield-granted [com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost] (Conspiracy
 * Unraveler). Each has to answer "which objects could pay this, can it be paid at all, and which
 * client picker drives the choice?" before the cost is committed to, and each is declined outright
 * when the answer is "none".
 *
 * The emitted `costType`s are deliberately the ones each cost already emits when it stands alone,
 * so these paths drive the existing pickers with no client-side special-casing.
 */
object SelectionCostPresentation {

    /**
     * Objects the caster could pick to pay [cost], excluding the spell being cast ([castCardId]) —
     * it is on the stack, not in the zone the cost draws from.
     *
     * Empty for a cost that carries no selection (pay life, mill) or one no picker covers; callers
     * treat that as "not payable this way" and decline the path rather than offering a dead UI.
     */
    fun candidates(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        cost: AdditionalCost,
        costUtils: CostEnumerationUtils,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        return when (cost) {
            // Behold spans battlefield *and* hand in one candidate pool.
            is AdditionalCost.Behold -> {
                val projected = state.projectedState
                val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                    predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
                }
                val handMatches = state.getZone(ZoneKey(playerId, Zone.HAND))
                    .filter { it != castCardId }
                    .filter { predicateEvaluator.matches(state, projected, it, cost.filter, predicateContext) }
                battlefieldMatches + handMatches
            }
            is AdditionalCost.Atom -> when (val atom = cost.atom) {
                is CostAtom.Sacrifice -> costUtils.findSacrificeTargets(state, playerId, atom)
                is CostAtom.ExileFrom -> costUtils.findExileTargets(state, playerId, atom.filter, atom.zone)
                    .filter { it != castCardId }
                is CostAtom.Discard -> handCandidates(state, playerId, castCardId, atom.filter, predicateEvaluator)
                is CostAtom.RevealFromHand -> handCandidates(state, playerId, castCardId, atom.filter, predicateEvaluator)
                is CostAtom.TapPermanents -> costUtils.findAbilityTapTargets(
                    state, playerId, atom.filter, if (atom.excludeSelf) castCardId else null
                )
                is CostAtom.ReturnToHand -> costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                // The sum-gated graveyard costs: the pool is the whole (filtered) graveyard and the
                // binding constraint is a summed measure, not a count — so [selectionCount] can't
                // express it and [canPay] consults the resolver instead of counting candidates.
                is CostAtom.CollectEvidence ->
                    CollectEvidenceResolver.candidates(state, playerId, excludeCardId = castCardId).cards
                is CostAtom.ExileFromGraveyardForTotal -> GraveyardTotalExileResolver
                    .candidates(state, playerId, atom.measure, atom.filter, excludeCardId = castCardId).cards
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * How many objects the caster must pick to pay [cost] — 0 when it carries no selection.
     *
     * Zero also means "no *counted* selection", which is not the same as free: the sum-gated
     * graveyard costs report 1 (a floor of one card) while their real constraint is a summed
     * measure. Ask [canPay], never this, whether a cost is affordable.
     */
    fun selectionCount(cost: AdditionalCost): Int = when (cost) {
        is AdditionalCost.Behold -> cost.count
        is AdditionalCost.Atom -> cost.atom.selectionCount
        else -> 0
    }

    /**
     * Whether [playerId] could pay [cost] right now, given its [candidates] pool.
     *
     * For a counted selection this is just "enough candidates". The sum-gated graveyard costs
     * (collect evidence, and the filtered `ExileFromGraveyardForTotal` generalization behind it)
     * are asked of their own resolver, because a graveyard can hold plenty of cards and still not
     * reach the measure — and per CR 701.59b a payer who cannot reach the floor may not choose to
     * pay at all, so the cost has to fail closed rather than be offered and refused.
     */
    fun canPay(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        cost: AdditionalCost,
        candidates: List<EntityId>,
    ): Boolean {
        val atom = (cost as? AdditionalCost.Atom)?.atom
        return when (atom) {
            is CostAtom.CollectEvidence ->
                CollectEvidenceResolver.canCollect(state, playerId, atom.amount, excludeCardId = castCardId)
            is CostAtom.ExileFromGraveyardForTotal -> GraveyardTotalExileResolver
                .canPay(state, playerId, atom.measure, atom.minTotal, atom.filter, excludeCardId = castCardId)
            else -> {
                val required = selectionCount(cost)
                required == 0 || candidates.size >= required
            }
        }
    }

    /**
     * A short label for the cast action's description ("Discard", "Sacrifice", …) paired with the
     * client cost data for paying [cost] from [candidates], or null when [cost] carries no
     * selection a picker could drive.
     *
     * Exile costs identify their source zone so the client opens the matching picker instead of
     * assuming every exile payment comes from the graveyard.
     *
     * The sum-gated graveyard costs are delegated to their own resolvers rather than rebuilt here:
     * their payload carries per-card weights and a summed floor the client tallies against, which
     * [candidates] alone cannot supply. That keeps one sum-gated picker for every context, exactly
     * as the two resolvers already share one implementation.
     */
    fun costData(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        cost: AdditionalCost,
        candidates: List<EntityId>,
    ): Pair<String, AdditionalCostData>? = when (cost) {
        is AdditionalCost.Behold -> "Behold" to AdditionalCostData(
            description = cost.description,
            costType = "Behold",
            validBeholdTargets = candidates,
            beholdCount = cost.count,
        )
        is AdditionalCost.Atom -> {
            val description = cost.description
            when (val atom = cost.atom) {
                is CostAtom.Sacrifice -> "Sacrifice" to AdditionalCostData(
                    description = description,
                    costType = "SacrificePermanent",
                    validSacrificeTargets = candidates,
                    sacrificeCount = atom.count,
                )
                is CostAtom.Discard -> "Discard" to AdditionalCostData(
                    description = description,
                    costType = "DiscardCard",
                    validDiscardTargets = candidates,
                    discardCount = atom.count,
                )
                is CostAtom.ExileFrom -> {
                    val (label, costType) = when (atom.zone) {
                        Zone.GRAVEYARD -> "Exile from graveyard" to "ExileFromGraveyard"
                        Zone.HAND -> "Exile from hand" to "ExileFromHand"
                        else -> return null
                    }
                    label to AdditionalCostData(
                        description = description,
                        costType = costType,
                        validExileTargets = candidates,
                        exileMinCount = atom.count,
                        exileMaxCount = atom.count,
                    )
                }
                is CostAtom.TapPermanents -> "Tap" to AdditionalCostData(
                    description = description,
                    costType = "TapPermanents",
                    validTapTargets = candidates,
                    tapCount = atom.count,
                )
                is CostAtom.ReturnToHand -> "Return to hand" to AdditionalCostData(
                    description = description,
                    costType = "BouncePermanent",
                    validBounceTargets = candidates,
                    bounceCount = atom.count,
                )
                // Collect evidence names its amount, because the amount *is* the choice —
                // "Collect evidence 10" reads the way the card is printed where a bare
                // "Collect evidence" would not (CR 701.59).
                is CostAtom.CollectEvidence -> {
                    val info = CollectEvidenceResolver
                        .costInfo(state, playerId, atom.amount, excludeCardId = castCardId)
                        ?: return null
                    "Collect evidence ${atom.amount}" to info
                }
                is CostAtom.ExileFromGraveyardForTotal -> {
                    val info = GraveyardTotalExileResolver
                        .costInfo(state, playerId, atom, excludeCardId = castCardId)
                        ?: return null
                    "Exile from graveyard" to info
                }
                else -> null
            }
        }
        else -> null
    }

    private fun handCandidates(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        filter: GameObjectFilter,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val handCards = state.getZone(ZoneKey(playerId, Zone.HAND)).filter { it != castCardId }
        if (filter == GameObjectFilter.Any) return handCards
        val predicateContext = PredicateContext(controllerId = playerId)
        return handCards.filter {
            predicateEvaluator.matches(state, state.projectedState, it, filter, predicateContext)
        }
    }
}
