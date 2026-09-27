package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.mechanics.targeting.ColorProtection
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The one reading of an Aura's printed "Enchant …" restriction (CR 303.4a) against a would-be host.
 *
 * Only the requirement's *filter* is evaluated — never targeting legality — because both callers
 * ask about an Aura that isn't being cast: the enchant state-based action (CR 704.5m, an attached
 * Aura isn't re-targeted, so hexproof/shroud don't dislodge it) and `CardPredicate.CouldEnchant`
 * ("an Aura card that could enchant it" — an Aura put onto the battlefield doesn't target,
 * CR 303.4f).
 */
object EnchantRestriction {

    /**
     * The battlefield filter behind an Aura's `auraTarget`, or null when the requirement isn't one
     * that can be checked against a permanent host (an "enchant player" requirement, say).
     */
    fun filterOf(requirement: TargetRequirement): TargetFilter? = when (requirement) {
        is TargetObject -> requirement.filter
        // "Enchant another …" — the distinctness rule is targeting-only; the filter is the base's.
        is TargetOther -> filterOf(requirement.baseRequirement)
        else -> null
    }

    /**
     * Whether the Aura [auraId] could legally be attached to [hostId]: the host satisfies its printed
     * enchant restriction **and** doesn't have protection from one of the Aura's colors (CR 702.16c —
     * a permanent with protection can't be enchanted by Auras with that quality). The one question
     * behind "an Aura card that could enchant it" and an effect that names an Aura's host
     * (`MoveCollectionEffect.attachTo`): an Aura that fails it stays where it is (CR 303.4g) rather
     * than attaching and then being put into the graveyard. Fails closed (false) when the restriction
     * can't be judged.
     */
    fun couldAttach(
        state: GameState,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator,
        cardRegistry: CardRegistry,
        auraId: EntityId,
        auraCard: CardComponent,
        hostId: EntityId,
        controllerId: EntityId
    ): Boolean {
        val requirement = cardRegistry.getCard(auraCard.cardDefinitionId)?.script?.auraTarget ?: return false
        if (hostSatisfies(state, projected, predicateEvaluator, requirement, hostId, controllerId, auraId) != true) {
            return false
        }
        return !hostProtectedFromAttachmentColor(state, projected, cardRegistry, auraId, auraCard, hostId)
    }

    /**
     * True when [hostId] has protection from one of the attachment's colors, CR 702.16c/d. The
     * attachment's colors are its projected ones while it is on the battlefield, else its card's
     * (an Aura about to enter from a library or exile). An attachment whose own printed
     * [GrantProtection] grants that color's protection is exempt — the Ward cycle's "This effect
     * doesn't remove this Aura" — and one with a dynamic protection grant is exempt entirely
     * (Pledge of Loyalty). (Approximation: the exemption is per-color rather than per-effect.)
     */
    fun hostProtectedFromAttachmentColor(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        attachmentId: EntityId,
        attachmentCard: CardComponent,
        hostId: EntityId
    ): Boolean {
        val colors: Set<String> = if (attachmentId in state.getBattlefield()) projected.getColors(attachmentId)
        else attachmentCard.colors.map { it.name }.toSet()
        // A colorless attachment meets protection from colorless (CR 105.2c, 702.16c/d).
        if (colors.isEmpty()) return projected.hasKeyword(hostId, ColorProtection.PROTECTION_FROM_COLORLESS)
        val statics = cardRegistry.getCard(attachmentCard.cardDefinitionId)?.staticAbilities.orEmpty()
        if (statics.any { it is GrantProtectionFromControlledColors || it is GrantProtectionFromChosenColorToGroup }) {
            return false
        }
        val selfGrantedColors: Set<Color> = statics.filterIsInstance<GrantProtection>().map { it.color }.toSet()
        return Color.entries.any { color ->
            color.name in colors &&
                color !in selfGrantedColors &&
                projected.hasKeyword(hostId, "PROTECTION_FROM_${color.name}")
        }
    }

    /**
     * Whether [hostId] satisfies [requirement], with "you" in the restriction meaning
     * [controllerId] (the Aura's controller, or the player who would put it onto the battlefield).
     * Null when the requirement can't be judged against a permanent at all — callers decide
     * whether that fails open (the SBA) or closed (a search filter).
     */
    fun hostSatisfies(
        state: GameState,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator,
        requirement: TargetRequirement,
        hostId: EntityId,
        controllerId: EntityId,
        auraId: EntityId?
    ): Boolean? {
        val filter = filterOf(requirement) ?: return null
        // A cross-zone union requirement is satisfied by any one clause; only battlefield clauses
        // can describe a permanent host.
        val battlefieldClauses = filter.clauses().filter { it.zone == Zone.BATTLEFIELD }
        if (battlefieldClauses.isEmpty()) return null
        val context = PredicateContext(controllerId = controllerId, sourceId = auraId)
        return battlefieldClauses.any {
            predicateEvaluator.matches(state, projected, hostId, it.baseFilter, context)
        }
    }
}
