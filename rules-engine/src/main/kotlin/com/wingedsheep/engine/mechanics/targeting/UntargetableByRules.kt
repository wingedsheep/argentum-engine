package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntargetableBy
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The one reading of [com.wingedsheep.sdk.scripting.CantBeTargetedBy] — "can't be the target of
 * Aura spells" (Tetsuo Umezawa), "… of spells that can target only Walls or of abilities that can
 * target only Walls" (Wall of Shadows) — shared by every targeting site: legal-target enumeration
 * (`TargetFinder`, `TargetEnumerationUtils`), cast/activation validation (`TargetValidator`) and
 * the resolution re-check (`ResolutionTargetValidator`), so the four can't drift apart.
 *
 * The restriction is projected onto the permanent as a keyword per [UntargetableBy] quality (see
 * [keyword]), so losing all abilities drops it. A targeting attempt computes the keywords that
 * would block it once ([blockingKeywords]) — from the kind of source (spell or ability), the
 * spell's subtypes, and the subtypes the target requirement structurally demands — and each
 * candidate is then a set lookup. Not controller-gated: it binds the permanent's own controller.
 *
 * It also reads [CardsInZonesCantBeTargeted] ("Cards in graveyards can't be the targets of spells
 * or abilities") — the off-battlefield counterpart, read off the holder's projected statics
 * ([zoneLocks]) because a card in a graveyard has no projection entry to carry a keyword.
 */
object UntargetableByRules {

    /** The projected keyword a permanent carries for [quality]. */
    fun keyword(quality: UntargetableBy): String = when (quality) {
        is UntargetableBy.SpellsWithSubtype ->
            "CANT_BE_TARGETED_BY_SUBTYPE_SPELLS_${quality.subtype.value.uppercase()}"
        is UntargetableBy.TargetingOnly ->
            "CANT_BE_TARGETED_BY_TARGETING_ONLY_${quality.subtype.value.uppercase()}"
    }

    /**
     * The keywords that bar a target from a spell or ability from [sourceId] choosing it for
     * [requirement]. Empty — the common case — when nothing could apply, so callers skip the
     * per-candidate check. An ANY-typed caller is resolved to "spell" only when the source is a
     * spell on the stack; otherwise the restriction is never applied on a guess.
     *
     * [spellSubtypes] is the face actually being cast, when the caller knows it — a disturb card
     * cast as its Aura back (Lanterns' Lift) is still its front face on the entity. Null falls back
     * to the source's projected or printed subtypes.
     */
    fun blockingKeywords(
        state: GameState,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType,
        requirement: TargetRequirement?,
        spellSubtypes: Set<String>? = null
    ): List<String> {
        val isSpell = when (targetingSourceType) {
            TargetingSourceType.SPELL -> true
            TargetingSourceType.ACTIVATED_ABILITY, TargetingSourceType.TRIGGERED_ABILITY -> false
            TargetingSourceType.ANY ->
                if (sourceId != null && SourceKindProtection.isSpell(state, sourceId)) true else return emptyList()
        }
        val required = requirement?.let(::soleTargetFilter)?.let(::requiredSubtypes).orEmpty()
        val subtypes = when {
            !isSpell -> emptySet()
            spellSubtypes != null -> spellSubtypes
            sourceId != null -> sourceSubtypes(state, sourceId)
            else -> emptySet()
        }
        if (required.isEmpty() && subtypes.isEmpty()) return emptyList()
        return subtypes.map { keyword(UntargetableBy.SpellsWithSubtype(Subtype(it))) } +
            required.map { keyword(UntargetableBy.TargetingOnly(it)) }
    }

    /**
     * True when [targetId] carries any of [keywords] (from [blockingKeywords]). Only battlefield
     * permanents have projected keywords, so no zone check is needed.
     */
    fun isBlocked(state: GameState, targetId: EntityId, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return false
        val projected = state.projectedState
        return keywords.any { projected.hasKeyword(targetId, it) }
    }

    /** Why [targetId] can't be chosen for [requirement] by a spell or ability from [sourceId], or null. */
    fun targetingError(
        state: GameState,
        targetId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType,
        requirement: TargetRequirement?,
        spellSubtypes: Set<String>? = null
    ): String? {
        val keywords = blockingKeywords(state, sourceId, targetingSourceType, requirement, spellSubtypes)
        if (!isBlocked(state, targetId, keywords)) return null
        val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "That permanent"
        return "$name can't be the target of this ${if (targetingSourceType == TargetingSourceType.SPELL) "spell" else "ability"}"
    }

    /**
     * The zone locks in force — every [CardsInZonesCantBeTargeted] ("Cards in graveyards can't be
     * the targets of spells or abilities") among the battlefield's projected static abilities,
     * with the permanent that holds it. Read off projection, so a printed lock, a granted one and
     * ability loss are all handled the way projection handles them. Empty in the common case;
     * callers compute it once per targeting attempt and only consult it for off-battlefield cards.
     */
    fun zoneLocks(state: GameState): List<Pair<EntityId, CardsInZonesCantBeTargeted>> {
        val projected = state.projectedState
        var locks: MutableList<Pair<EntityId, CardsInZonesCantBeTargeted>>? = null
        for (id in state.getBattlefield()) {
            for (ability in projected.getStaticAbilities(id)) {
                if (ability is CardsInZonesCantBeTargeted) {
                    (locks ?: mutableListOf<Pair<EntityId, CardsInZonesCantBeTargeted>>().also { locks = it })
                        .add(id to ability)
                }
            }
        }
        return locks ?: emptyList()
    }

    /**
     * True when one of [locks] (from [zoneLocks]) forbids targeting the card [cardId] where it
     * is now. Players, battlefield permanents and spells are never in a locked zone.
     */
    fun isZoneLocked(
        state: GameState,
        cardId: EntityId,
        locks: List<Pair<EntityId, CardsInZonesCantBeTargeted>>,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (locks.isEmpty()) return false
        val zone = state.logicalZone(cardId)?.zoneType ?: return false
        return locks.any { (holderId, lock) ->
            if (zone !in lock.zones) return@any false
            if (lock.filter == GameObjectFilter.Any) return@any true
            val controller = ControllerGrants.granterController(state, holderId) ?: return@any false
            predicateEvaluator.matches(
                state, state.projectedState, cardId, lock.filter,
                PredicateContext(controllerId = controller, sourceId = holderId)
            )
        }
    }

    /** Why the card [cardId] can't be targeted where it is, or null — [zoneLocks] and [isZoneLocked] in one. */
    fun zoneLockError(state: GameState, cardId: EntityId, predicateEvaluator: PredicateEvaluator): String? {
        if (!isZoneLocked(state, cardId, zoneLocks(state), predicateEvaluator)) return null
        val name = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "That card"
        return "$name can't be the target of spells or abilities where it is"
    }

    /**
     * The filter a requirement restricts its targets to, when that filter is *all* it can target:
     * a plain object requirement ("target Wall", "enchant Wall"), through any "other" wrapper.
     * Requirements that can also reach a player or a spell ("target Wall or player") return null —
     * they can target something other than the subtype.
     */
    private fun soleTargetFilter(requirement: TargetRequirement): GameObjectFilter? = when (requirement) {
        is TargetOther -> soleTargetFilter(requirement.baseRequirement)
        is TargetObject -> requirement.filter.takeUnless { it.isUnion }?.baseFilter
        else -> null
    }

    /**
     * Subtypes [filter] structurally demands of every match: a top-level `HasSubtype`, or one
     * shared by every branch of an `anyOf` union. Conservative — anything looser isn't "only".
     */
    private fun requiredSubtypes(filter: GameObjectFilter): Set<Subtype> {
        val own = filter.cardPredicates.filterIsInstance<CardPredicate.HasSubtype>().mapTo(mutableSetOf()) { it.subtype }
        if (filter.anyOf.isNotEmpty()) {
            own += filter.anyOf.map(::requiredSubtypes).reduce { acc, s -> acc intersect s }
        }
        return own
    }

    /** A spell's subtypes — projected when present, else its printed type line (at cast it's still in hand). */
    private fun sourceSubtypes(state: GameState, sourceId: EntityId): Set<String> =
        state.projectedState.getSubtypes(sourceId).ifEmpty {
            state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.subtypes?.mapTo(mutableSetOf()) { it.value }.orEmpty()
        }
}
