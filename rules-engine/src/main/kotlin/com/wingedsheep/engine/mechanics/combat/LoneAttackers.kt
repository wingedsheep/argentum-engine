package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanOnlyAttackAlone

/**
 * "This creature can only attack alone" ([CanOnlyAttackAlone], Master of Cruelties) — the
 * restriction (CR 508.1c) and how it trades off against attack requirements (CR 508.1d).
 *
 * One place for the rule so the declaration check, the requirement checks and the
 * `mandatoryAttackers` the client pre-selects cannot drift apart. Requirements are counted one per
 * creature that must attack — every requirement source the engine enforces names a creature, and
 * a creature that obeys one obeys them all by attacking.
 *
 * With L the lone-only creatures and R the creatures required to attack, the declarations that
 * obey the most requirements are:
 *  - every creature of R \ L, when there are at least two of them;
 *  - with exactly one, either it or any one creature of R ∩ L alone (a tie, both are legal);
 *  - with none, any one creature of R ∩ L alone.
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
        if (cardRegistry.getCard(card.cardDefinitionId)?.staticAbilities.orEmpty().any { it is CanOnlyAttackAlone }) {
            return true
        }
        return state.grantedStaticAbilities.any { it.entityId == entityId && it.ability is CanOnlyAttackAlone }
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
     * The required creatures ([required]) whose requirement a declaration of [declared] may leave
     * unobeyed because of the restriction — the rest are still checked as usual. Assumes the
     * declaration already obeys the restriction.
     */
    fun excusedRequirements(
        declared: Set<EntityId>,
        required: Set<EntityId>,
        loneOnly: Set<EntityId>,
    ): Set<EntityId> {
        if (loneOnly.isEmpty() || required.isEmpty()) return emptySet()
        val others = required - loneOnly
        val alone = declared.singleOrNull()
        if (alone != null && alone in loneOnly) {
            return if (alone in required && others.size <= 1) required - alone else emptySet()
        }
        return if (others.isNotEmpty()) required intersect loneOnly else emptySet()
    }

    /**
     * The creatures every best declaration attacks with — what the client may lock in. Ties (one
     * other required creature, or several lone-only ones) leave the choice to the player.
     */
    fun mandatory(required: List<EntityId>, loneOnly: Set<EntityId>): List<EntityId> {
        val lone = required.filter { it in loneOnly }
        if (lone.isEmpty()) return required
        val others = required.filter { it !in loneOnly }
        return when {
            others.size >= 2 -> others
            others.isEmpty() && lone.size == 1 -> lone
            else -> emptyList()
        }
    }
}
