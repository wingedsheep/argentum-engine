package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.EscalateCosts
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/** The same additional-cost terms for announcement, payment, and legal-action presentation. */
class CastAdditionalCosts(
    private val cardRegistry: CardRegistry,
    private val costCalculator: CostCalculator,
    private val zoneResolver: CastZoneResolver,
    private val predicateEvaluator: PredicateEvaluator,
) {
    /**
     * The additional costs a modal spell owes for the modes it chose: per-mode overrides where the
     * chosen modes declare them (their costs combine), card-level costs otherwise, plus the
     * non-mana escalate cost when the card has one ([EscalateCosts.additionalCostFor]).
     */
    fun additionalCostsForModes(cardDef: CardDefinition, action: CastSpell): List<AdditionalCost> {
        if (action.chosenModes.isEmpty()) return cardDef.script.additionalCosts
        val modalEffect = cardDef.script.spellEffect as? ModalEffect ?: return cardDef.script.additionalCosts

        val perModeOverrides = action.chosenModes.mapNotNull { modeIndex ->
            modalEffect.modes.getOrNull(modeIndex)?.additionalCosts
        }
        val base = if (perModeOverrides.isEmpty()) cardDef.script.additionalCosts else perModeOverrides.flatten()
        val escalate = EscalateCosts.additionalCostFor(modalEffect, action.chosenModes.size)
        return if (escalate == null) base else base + escalate
    }

    /**
     * The non-mana half of the optional cost the caster *declared* (kicker, bargain, teamwork —
     * `action.declaredCostSlot`). Kept apart from the card's printed additional costs because only
     * it carries the declared mechanic's identity, which is what names a tap's cause
     * ([com.wingedsheep.sdk.scripting.TapReason.forChoiceSlot]).
     */
    fun declaredSlotCost(action: CastSpell, cardDef: CardDefinition?): AdditionalCost? =
        declaredOptionalCosts(action, cardDef).firstOrNull { it.additionalCost != null }?.additionalCost

    /**
     * Every additional cost this cast owes: the card's (or its chosen modes'), the declared optional
     * cost's non-mana half, the chosen alternative cost's bundled costs, and the costs a cast
     * permission attaches.
     */
    fun owedAdditionalCosts(state: GameState, action: CastSpell, cardDef: CardDefinition?): List<AdditionalCost> = buildList {
        if (cardDef != null) addAll(additionalCostsForModes(cardDef, action))
        declaredSlotCost(action, cardDef)?.let { add(it) }
        if (action.useAlternativeCost && cardDef != null) {
            // Each bundled additional cost is gated by the chosen alternative-cost type so a
            // collision (e.g. granted warp on a card also being evoked) doesn't drag in the
            // unchosen cost's bundled additional cost.
            val selfAltCost = cardDef.script.selfAlternativeCost
            if (selfAltCost != null && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) addAll(selfAltCost.additionalCosts)
            // A battlefield-granted alternative cost's non-mana half (Conspiracy Unraveler's
            // "collect evidence 10"). The mana half was already substituted for the spell's mana
            // cost; this is the rest of the same cost, so it is paid by the ordinary additional-cost
            // kinds — which is also what makes it validate and surface a picker like every other
            // selection cost.
            if (action.altAllows(AlternativeCostType.GRANTED)) {
                costCalculator.findAlternativeCastingCosts(state, action.playerId)
                    .firstOrNull()?.let { addAll(it.additionalCosts) }
            }
            // Flashback's bundled additional cost (e.g., Behold three Elementals)
            if (action.altAllows(AlternativeCostType.FLASHBACK) &&
                zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)
            ) {
                cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.Flashback>()
                    .firstOrNull()
                    ?.additionalCost
                    ?.let { add(it) }
            }
            // Warp's bundled additional cost (e.g., "Pay 2 life" on Timeline Culler). Use
            // [WarpGrants] so granted warps ([GrantWarpToCardsInHand]) participate too — currently
            // they carry no additional cost, but routing through the same helper keeps the seam.
            if (action.altAllows(AlternativeCostType.WARP) &&
                zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
            ) {
                WarpGrants.effectiveWarp(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)
                    ?.additionalCost
                    ?.let { add(it) }
            }
        }
        // Runtime additional costs from entity component (e.g., The Infamous Cruelclaw)
        state.getEntity(action.cardId)
            ?.get<PlayWithAdditionalCostComponent>()
            ?.takeIf { it.controllerId == action.playerId }
            ?.let { addAll(it.additionalCosts) }

        // Linked-exile granter additional cost (e.g., Dawnhand Dissident's "remove three counters
        // from among creatures you control")
        zoneResolver.findLinkedExileGranter(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }

        // Self-referential MayCastSelfFromZones grant's additional cost (e.g. Alien Symbiosis'
        // "by discarding a card")
        zoneResolver.findMayCastSelfFromZoneAbility(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }

        // Gwenom: pay-life additional cost for a spell cast from the top of the library.
        zoneResolver.topOfLibraryAlternativeGrant(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }
    }

}
