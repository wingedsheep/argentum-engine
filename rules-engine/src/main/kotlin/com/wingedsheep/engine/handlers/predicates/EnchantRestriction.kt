package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.mechanics.targeting.ColorProtection
import com.wingedsheep.engine.mechanics.targeting.SourceKindProtection
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
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
        if (!hostAllowsAura(state, projected, predicateEvaluator, auraId, hostId)) return false
        val gained = gainedRestrictionAdmits(state, projected, predicateEvaluator, auraId, hostId, controllerId)
        if (gained != null) {
            if (!gained) return false
        } else {
            val requirement = if (state.getEntity(auraId)?.has<com.wingedsheep.engine.mechanics.BestowedComponent>() == true) {
                if (!projected.hasKeyword(auraId, com.wingedsheep.engine.mechanics.BestowCasts.ENCHANT_CREATURE)) return false
                com.wingedsheep.engine.mechanics.BestowCasts.enchantCreature
            } else cardRegistry.getCard(auraCard.cardDefinitionId)?.script?.auraTarget ?: return false
            if (hostSatisfies(state, projected, predicateEvaluator, requirement, hostId, controllerId, auraId) != true) {
                return false
            }
        }
        return !hostProtectedFromAttachment(state, projected, cardRegistry, auraId, auraCard, hostId)
    }

    /**
     * The verdict of an enchant ability the Aura *gained* in place of its printed one
     * ([GainedEnchantRestrictionComponent] — Animate Dead's "enchant creature put onto the
     * battlefield with this Aura"), or null when the Aura has none and its printed `auraTarget`
     * still rules. The host must be the very object recorded (CR 400.7: one that left and returned
     * is a new object), still on the battlefield, and still match the gained filter.
     */
    fun gainedRestrictionAdmits(
        state: GameState,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator,
        auraId: EntityId,
        hostId: EntityId,
        controllerId: EntityId
    ): Boolean? {
        val gained = state.getEntity(auraId)?.get<GainedEnchantRestrictionComponent>() ?: return null
        if (hostId !in state.getBattlefield()) return false
        if (gained.hosts.none { it.entityId == hostId && state.isCurrentObject(it) }) return false
        return predicateEvaluator.matches(
            state, projected, hostId, gained.filter, PredicateContext(controllerId = controllerId, sourceId = auraId)
        )
    }

    /** Host-side prohibitions, shared by targeting, entry, reattachment, and state-based actions. */
    fun hostAllowsAura(
        state: GameState,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator,
        auraId: EntityId,
        hostId: EntityId
    ): Boolean {
        if (projected.hasKeyword(hostId, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_ENCHANTED)) return false
        return sourceRestrictionsAllowAura(state, projected, predicateEvaluator, auraId, hostId)
    }

    /** Source-aware restrictions apply to existing attachments as well as new ones. */
    fun sourceRestrictionsAllowAura(
        state: GameState,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator,
        auraId: EntityId,
        hostId: EntityId
    ): Boolean {
        return projected.getProjectedValues(hostId)?.enchantmentRestrictions.orEmpty().none { restriction ->
            if (!restriction.survivesSourceAbilityRemoval && projected.hasLostAllAbilities(restriction.sourceId)) false
            else if (restriction.exceptSource && restriction.sourceId == auraId) false
            else {
                val controller = projected.getController(restriction.sourceId)
                    ?: state.getEntity(restriction.sourceId)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
                    ?: return@none false
                predicateEvaluator.matches(state, projected, auraId, restriction.auras,
                    PredicateContext(controllerId = controller, sourceId = restriction.sourceId))
            }
        }
    }

    /**
     * True when [hostId] has protection from one of the attachment's colors, CR 702.16c/d. The
     * attachment's colors are its projected ones while it is on the battlefield, else its card's
     * (an Aura about to enter from a library or exile). An attachment whose own printed
     * [GrantProtection] grants that color's protection is exempt — the Ward cycle's "This effect
     * doesn't remove this Aura" — and one with a dynamic protection grant is exempt entirely
     * (Pledge of Loyalty). (Approximation: the exemption is per-color rather than per-effect.)
     */
    fun hostProtectedFromAttachment(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        attachmentId: EntityId,
        attachmentCard: CardComponent,
        hostId: EntityId
    ): Boolean {
        val battlefield = attachmentId in state.getBattlefield()
        val types = if (battlefield) projected.getTypes(attachmentId) else attachmentCard.typeLine.cardTypes.map { it.name }.toSet()
        if (types.any { projected.hasKeyword(hostId, "PROTECTION_FROM_CARDTYPE_$it") }) return true
        val subtypes = if (battlefield) projected.getSubtypes(attachmentId) else attachmentCard.typeLine.subtypes.map { it.value }.toSet()
        if (subtypes.any { projected.hasKeyword(hostId, "PROTECTION_FROM_SUBTYPE_${it.uppercase()}") }) return true
        val supertypes = if (battlefield) projected.getSupertypes(attachmentId) else attachmentCard.typeLine.supertypes.map { it.name }.toSet()
        if (supertypes.any { projected.hasKeyword(hostId, "PROTECTION_FROM_SUPERTYPE_${it.uppercase()}") }) return true
        // An Aura or Equipment that was cast this turn (CR 702.16c/d) — Emrakul, the World Anew.
        if (SourceKindProtection.isProtectedFromObject(state, hostId, attachmentId)) return true
        if (projected.hasKeyword(hostId, "PROTECTION_FROM_EACH_OPPONENT")) {
            val hostController = projected.getController(hostId)
            val attachmentController = projected.getController(attachmentId)
                ?: state.getEntity(attachmentId)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            if (hostController != null && attachmentController in state.getOpponents(hostController)) return true
        }
        val colors: Set<String> = if (attachmentId in state.getBattlefield()) projected.getColors(attachmentId)
        else attachmentCard.colors.map { it.name }.toSet()
        // A colorless attachment meets protection from colorless (CR 105.2c, 702.16c/d).
        if (colors.isEmpty()) return projected.hasKeyword(hostId, ColorProtection.PROTECTION_FROM_COLORLESS)
        // A multicolored attachment meets protection from multicolored (CR 105.2b).
        if (colors.size >= 2 && projected.hasKeyword(hostId, ColorProtection.PROTECTION_FROM_MULTICOLORED)) return true
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
        auraId: EntityId?,
        hostZone: Zone = Zone.BATTLEFIELD
    ): Boolean? {
        val filter = filterOf(requirement) ?: return null
        // A cross-zone union requirement is satisfied by any one clause; only the clauses scoped to
        // [hostZone] can describe the host. Usually that is the battlefield — and "an Aura card
        // that could enchant it" asks the battlefield question even of a host that has since left
        // — but the enchant state-based action also asks it of a card an Aura enchants in another
        // zone ("Enchant creature card in a graveyard", Animate Dead; CR 303.4a).
        val clauses = filter.clauses().filter { it.zone == hostZone }
        if (clauses.isEmpty()) return null
        val context = PredicateContext(controllerId = controllerId, sourceId = auraId)
        return clauses.any {
            predicateEvaluator.matches(state, projected, hostId, it.baseFilter, context)
        }
    }

    /** The zone [entityId] is in right now — the battlefield, or the card zone it actually sits in. */
    fun zoneOf(state: GameState, entityId: EntityId): Zone? {
        if (entityId in state.getBattlefield()) return Zone.BATTLEFIELD
        val key = state.logicalZone(entityId) ?: return null
        return key.zoneType.takeIf { entityId in state.getZone(key) }
    }
}
