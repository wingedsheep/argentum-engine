package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanOnlyAttackAlone
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * "This creature can only attack alone" ([CanOnlyAttackAlone], Master of Cruelties) — the
 * restriction (CR 508.1c) and how it trades off against attack requirements (CR 508.1d).
 *
 * One place for the rule so the declaration check, the requirement checks and the
 * `mandatoryAttackers` the client pre-selects cannot drift apart. Requirements are weighed per
 * creature — how many it carries (a creature goaded by two players carries two, CR 701.15c) — and a
 * creature that attacks obeys all of its own.
 *
 * With L the lone-only creatures and R the required ones, only two shapes of declaration can be
 * best: every creature of R \ L attacking (obeying their summed weight), or one creature of L
 * attacking alone (obeying its own weight). Whichever obeys more wins; equal weights tie, and then
 * either is legal.
 */
object LoneAttackers {

    /**
     * Whether [entityId] carries the restriction. Printed on its card or granted to it (a token's
     * abilities arrive as granted static abilities); a face-down creature has no abilities (CR
     * 708.2) and neither does one that lost them all.
     */
    fun isLoneOnly(state: GameState, projected: ProjectedState, entityId: EntityId, cardRegistry: CardRegistry): Boolean {
        val container = state.getEntity(entityId) ?: return false
        if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(entityId)) return false
        val card = container.get<CardComponent>() ?: return false
        val printed = cardRegistry.getCard(card.cardDefinitionId)?.staticAbilities.orEmpty()
        val granted = state.grantedStaticAbilities.filter { it.entityId == entityId }.map { it.ability }
        return (printed + granted).any { it is CanOnlyAttackAlone && it.filter.scope is Scope.Self }
    }

    /** The creatures of [candidates] that can only attack alone, in their order. */
    fun of(
        state: GameState,
        candidates: Collection<EntityId>,
        cardRegistry: CardRegistry,
        projected: ProjectedState = state.projectedState,
    ): List<EntityId> = candidates.filter { isLoneOnly(state, projected, it, cardRegistry) }

    /** The lone-only creature a declaration of [declared] breaks the restriction of, if any. */
    fun violator(declared: Collection<EntityId>, loneOnly: Collection<EntityId>): EntityId? =
        if (declared.size > 1) declared.firstOrNull { it in loneOnly } else null

    /**
     * The required creatures whose requirements a declaration of [declared] may leave unobeyed
     * because of the restriction — the rest are still checked as usual. [required] maps each
     * required creature to its requirement count. Assumes the declaration obeys the restriction.
     */
    fun excusedRequirements(
        declared: Set<EntityId>,
        required: Map<EntityId, Int>,
        loneOnly: Set<EntityId>,
    ): Set<EntityId> {
        if (loneOnly.isEmpty() || required.isEmpty()) return emptySet()
        val best = Best(required, loneOnly)
        val alone = declared.singleOrNull()
        if (alone != null && alone in loneOnly) {
            return if ((required[alone] ?: 0) == best.weight) required.keys - alone else emptySet()
        }
        return if (best.othersWin) required.keys intersect loneOnly else emptySet()
    }

    /**
     * The creatures every best declaration attacks with — what the client may lock in. A tie
     * between the shapes leaves the choice to the player.
     */
    fun mandatory(required: Map<EntityId, Int>, loneOnly: Set<EntityId>): List<EntityId> {
        if (required.keys.none { it in loneOnly }) return required.keys.toList()
        val best = Best(required, loneOnly)
        val loneWinners = required.filter { (id, weight) -> id in loneOnly && weight == best.weight }.keys
        return when {
            best.othersWin && loneWinners.isEmpty() -> required.keys.filter { it !in loneOnly }
            !best.othersWin && loneWinners.size == 1 -> loneWinners.toList()
            else -> emptyList()
        }
    }

    /** The most requirements any declaration can obey, and whether the others attacking does. */
    private class Best(required: Map<EntityId, Int>, loneOnly: Set<EntityId>) {
        private val others = required.filterKeys { it !in loneOnly }.values.sum()
        val weight = maxOf(others, required.filterKeys { it in loneOnly }.values.maxOrNull() ?: 0)
        val othersWin = others > 0 && others == weight
    }
}
