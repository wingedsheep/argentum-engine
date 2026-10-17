package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * "Bands with other [quality]" (CR 702.22b) — the special form of banding — read off the projected
 * keyword strings [KeywordAbility.BandsWithOther.projectedKeyword] encodes. Every quality test reads
 * *projected* state (supertypes, subtypes, the Layer-3 name), so a creature made legendary, given a
 * creature type, or renamed by a continuous effect counts with its current characteristics.
 *
 * The two questions combat asks:
 * - [formsBand] — CR 702.22c's second form: one or more attacking [quality] creatures with
 *   "bands with other [quality]" and any number of other attacking [quality] creatures.
 * - [invertsDamageAssignment] — CR 702.22j/k: a creature blocked by (or blocking) a group that has
 *   banding, *or* both a [quality] creature with "bands with other [quality]" and another
 *   [quality] creature, has its combat damage divided by the other player.
 */
internal object BandsWithOtherRules {

    /** The "bands with other" abilities [entityId] currently has (CR 702.22m: one per quality). */
    fun abilitiesOf(projected: ProjectedState, entityId: EntityId): List<KeywordAbility.BandsWithOther> {
        val keywords = projected.getKeywords(entityId)
        if (keywords.none { KeywordAbility.BandsWithOther.isBandsWithOther(it) }) return emptyList()
        return keywords.mapNotNull { KeywordAbility.BandsWithOther.fromKeyword(it) }
    }

    /** Whether [entityId] has the quality [ability] names, in projected state. */
    fun hasQuality(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        ability: KeywordAbility.BandsWithOther,
    ): Boolean {
        ability.supertype?.let { return projected.hasType(entityId, it.name) }
        ability.subtype?.let { return projected.hasSubtype(entityId, it.value) }
        return nameOf(state, projected, entityId) == ability.name
    }

    /**
     * CR 702.22c: [members] form a legal band through "bands with other" — some member has
     * "bands with other [quality]" and every member has [quality]. (The member holding the
     * ability must have the quality itself: "[quality] creatures with 'bands with other
     * [quality]'".)
     */
    fun formsBand(state: GameState, projected: ProjectedState, members: Collection<EntityId>): Boolean =
        members.asSequence()
            .flatMap { abilitiesOf(projected, it).asSequence() }
            .distinct()
            .any { quality -> members.all { hasQuality(state, projected, it, quality) } }

    /**
     * CR 702.22j/k: whether the creatures on the other side of a creature's combat — [group], the
     * creatures blocking it or the creatures it blocks — hand the division of its combat damage to
     * the other player: one of them has banding, or one [quality] creature with "bands with other
     * [quality]" is joined by another [quality] creature.
     */
    fun invertsDamageAssignment(state: GameState, projected: ProjectedState, group: Collection<EntityId>): Boolean {
        if (group.any { projected.hasKeyword(it, Keyword.BANDING) }) return true
        if (group.size < 2) return false
        return group.any { holder ->
            abilitiesOf(projected, holder).any { quality ->
                hasQuality(state, projected, holder, quality) &&
                    group.any { other -> other != holder && hasQuality(state, projected, other, quality) }
            }
        }
    }

    /** A face-down creature has no name (CR 708.2); otherwise the Layer-3 name wins over the printed one. */
    private fun nameOf(state: GameState, projected: ProjectedState, entityId: EntityId): String? {
        if (projected.isFaceDown(entityId)) return null
        return projected.getName(entityId)?.takeIf { it.isNotBlank() }
            ?: state.getEntity(entityId)?.get<CardComponent>()?.name
    }
}
