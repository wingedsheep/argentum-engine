package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The "up to one target … of each <category>" cross-target rule shared by
 * [TargetObject.onePerCardType] (Uldaros Theorix) and [TargetObject.onePerColor] (Rogues' Gallery).
 *
 * Both are a single instance of the word "target" (CR 115.3): every chosen object claims exactly
 * one of its categories, so a selection is legal when the objects can be paired with *distinct*
 * categories they have — a bipartite matching ([OnePerCardType.canAssignDistinct]). An object with
 * no category (a colourless card for "of each color") can fill no slot and is never legal.
 *
 * The per-candidate category lists travel to choosers as
 * [com.wingedsheep.engine.core.TargetRequirementInfo.distinctCategories] so the client and the AI
 * can keep a selection legal while it is being built, rather than learning it was illegal on submit.
 */
object OnePerCategoryTargets {

    /** Whether [requirement] carries a one-per-category rule at all. */
    fun applies(requirement: TargetRequirement): Boolean =
        requirement is TargetObject && (requirement.onePerCardType || requirement.onePerColor)

    /** The categories [id] can claim under [requirement], or null when the requirement has no such rule. */
    fun categoriesOf(state: GameState, requirement: TargetRequirement, id: EntityId): Set<String>? {
        val target = requirement as? TargetObject ?: return null
        return when {
            target.onePerCardType -> OnePerCardType.cardTypesOf(state, id)
            target.onePerColor -> colorsOf(state, id)
            else -> null
        }
    }

    /**
     * Each candidate that can claim a category, mapped to its categories — the chooser-facing table.
     * Candidates with no category are left out: they can never be chosen.
     */
    fun forCandidates(
        state: GameState,
        requirement: TargetRequirement,
        candidates: List<EntityId>,
    ): Map<EntityId, List<String>>? {
        if (!applies(requirement)) return null
        return candidates.associateWith { categoriesOf(state, requirement, it).orEmpty().sorted() }
            .filterValues { it.isNotEmpty() }
    }

    /** Whether [ids] can each claim a different category under [requirement] (true when it has no such rule). */
    fun isSatisfied(state: GameState, requirement: TargetRequirement, ids: List<EntityId>): Boolean {
        if (!applies(requirement)) return true
        return OnePerCardType.canAssignDistinct(ids.map { categoriesOf(state, requirement, it).orEmpty() })
    }

    /** The most [candidates] that can be chosen together — the size of a maximum matching. */
    fun maxTargets(table: Map<EntityId, List<String>>): Int =
        OnePerCardType.maxDistinctAssignment(table.values.map { it.toSet() })

    /**
     * An object's colours (CR 105.2) — projected on the battlefield so a colour-changing effect
     * counts (and a permanent made colourless has none), the card's own colours elsewhere.
     */
    private fun colorsOf(state: GameState, id: EntityId): Set<String> =
        if (id in state.getBattlefield()) state.projectedState.getColors(id)
        else state.getEntity(id)?.get<CardComponent>()?.colors?.mapTo(mutableSetOf()) { it.name }.orEmpty()
}
