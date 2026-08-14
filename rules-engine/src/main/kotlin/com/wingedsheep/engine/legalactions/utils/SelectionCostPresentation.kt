package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
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
 * objects** — sacrifice, discard, tap, bounce, exile from a zone, behold.
 *
 * Two cast-time shapes need exactly this, off the same cost vocabulary: the non-mana leg of an
 * "… or pay {N}" cost ([AdditionalCost.OrPay]), and escalate whose cost isn't mana
 * ([com.wingedsheep.sdk.scripting.effects.ModalEffect.additionalCostPerExtraMode]). Both have to
 * answer "which objects could pay this, and which client picker drives the choice?" before the
 * cost is committed to, and both are declined outright when the answer is "none".
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
                is CostAtom.TapPermanents -> costUtils.findAbilityTapTargets(state, playerId, atom.filter)
                    .let { if (atom.excludeSelf) it.filter { id -> id != castCardId } else it }
                is CostAtom.ReturnToHand -> costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    /** How many objects the caster must pick to pay [cost] — 0 when it carries no selection. */
    fun selectionCount(cost: AdditionalCost): Int = when (cost) {
        is AdditionalCost.Behold -> cost.count
        is AdditionalCost.Atom -> cost.atom.selectionCount
        else -> 0
    }

    /**
     * A short label for the cast action's description ("Discard", "Sacrifice", …) paired with the
     * client cost data for paying [cost] from [candidates], or null when [cost] carries no
     * selection a picker could drive.
     *
     * `"ExileFromGraveyard"` pins the client's picker to the graveyard, so a non-graveyard exile
     * cost is declined rather than shown against the wrong zone.
     */
    fun costData(
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
                is CostAtom.ExileFrom -> if (atom.zone != Zone.GRAVEYARD) null else {
                    "Exile from graveyard" to AdditionalCostData(
                        description = description,
                        costType = "ExileFromGraveyard",
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
