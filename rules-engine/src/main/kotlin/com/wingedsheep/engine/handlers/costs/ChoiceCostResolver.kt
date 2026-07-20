package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * Enumeration helper for the cost-vs-cost [AdditionalCost.Choice] additional cost — the general form
 * of Forage ("discard a card **or** sacrifice a permanent"; Souls of the Lost).
 *
 * A [Choice] surfaces at cast time as **one legal action per payable option** — the same multi-action
 * pattern [ForageCostResolver] and the `*OrPay` costs use — so the caster picks the option by choosing
 * which action to play and the existing per-cost picker (SacrificePermanent / DiscardCard /
 * ExileFromGraveyard) drives the selection with **no new client UI**. This object builds the
 * [AdditionalCostData] payload for each option; the enumerator's post-process attaches one to each
 * expanded cast action, and payment is recovered downstream from which field the client populated
 * (see `CastSpellHandler.reduceChoiceCosts`).
 *
 * Only options that map to a client-rendered cost picker are emitted; an option whose atom has no
 * picker (or which can't currently be paid) is dropped, so a [Choice] with no payable option yields
 * an empty list and the card is not castable.
 */
object ChoiceCostResolver {

    /**
     * One legal-action cost payload per **payable** option of [choice], in declaration order.
     * [cardId] is the spell being cast — excluded from its own discard/exile candidate pools.
     */
    fun costInfos(
        state: GameState,
        playerId: EntityId,
        choice: AdditionalCost.Choice,
        costUtils: CostEnumerationUtils,
        cardId: EntityId,
    ): List<AdditionalCostData> = choice.options.mapNotNull { optionCostInfo(state, playerId, it, costUtils, cardId) }

    private fun optionCostInfo(
        state: GameState,
        playerId: EntityId,
        option: AdditionalCost,
        costUtils: CostEnumerationUtils,
        cardId: EntityId,
    ): AdditionalCostData? {
        val atom = (option as? AdditionalCost.Atom)?.atom ?: return null
        return when (atom) {
            is CostAtom.Sacrifice -> {
                val targets = costUtils.findSacrificeTargets(state, playerId, atom)
                if (targets.size < atom.count) return null
                AdditionalCostData(
                    description = atom.description.replaceFirstChar { it.uppercase() },
                    costType = "SacrificePermanent",
                    validSacrificeTargets = targets,
                    sacrificeCount = atom.count,
                )
            }
            is CostAtom.Discard -> {
                if (atom.random) return null // a random discard needs no selection UI
                val targets = costUtils.findDiscardTargets(state, playerId, atom.filter).filter { it != cardId }
                if (targets.size < atom.count) return null
                AdditionalCostData(
                    description = atom.description.replaceFirstChar { it.uppercase() },
                    costType = "DiscardCard",
                    validDiscardTargets = targets,
                    discardCount = atom.count,
                )
            }
            is CostAtom.ExileFrom -> {
                val targets = costUtils.findExileTargets(state, playerId, atom.filter, atom.zone).filter { it != cardId }
                if (targets.size < atom.count) return null
                AdditionalCostData(
                    description = atom.description.replaceFirstChar { it.uppercase() },
                    costType = "ExileFromGraveyard",
                    validExileTargets = targets,
                    exileMinCount = atom.count,
                    exileMaxCount = atom.count,
                )
            }
            // Other atoms don't yet have a cost-vs-cost picker; add them here alongside a client
            // costType when a future Choice card needs one.
            else -> null
        }
    }
}
