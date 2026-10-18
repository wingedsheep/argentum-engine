package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
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
