package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared enforcement for the "can't be the target of abilities from <card-type> sources" family
 * (Artifact Ward → [com.wingedsheep.sdk.scripting.CantBeTargetedBySourceTypeAbilities]).
 *
 * The restriction is projected onto the affected creature as a keyword whose name encodes the
 * source card type — mirroring the `PROTECTION_FROM_CARDTYPE_<TYPE>` idiom — so a single check
 * generalizes over any source card type (artifact, enchantment, …). It is consulted from both
 * [TargetFinder] (legal-target enumeration) and `StackResolver` (resolution-time fizzle check);
 * keeping the keyword name and source-type resolution here avoids the two drifting apart.
 *
 * Unlike the opponent-ability restriction this is NOT controller-gated — it keys off the ability's
 * source (CR 113.7) by card type, not its controller: a matching source can't target the warded
 * creature even if the same player controls both. It only blocks abilities, never spells.
 */
object SourceTypeTargeting {

    /** The projected keyword granted to creatures warded against [cardTypeName] sources' abilities. */
    fun keyword(cardTypeName: String): String =
        "CANT_BE_TARGETED_BY_CARDTYPE_${cardTypeName.uppercase()}_SOURCE_ABILITIES"

    /**
     * The source's card types — projected types for permanents on the battlefield, falling back to
     * the printed card types for spell/ability sources not present in the projection (the same
     * resolution used for protection-from-card-type).
     */
    fun sourceCardTypes(state: GameState, sourceId: EntityId): Set<String> {
        val projectedTypes = state.projectedState.getTypes(sourceId)
        if (projectedTypes.isNotEmpty()) return projectedTypes
        return state.getEntity(sourceId)?.get<CardComponent>()
            ?.typeLine?.cardTypes?.map { it.name }?.toSet() ?: emptySet()
    }

    /**
     * True if [targetId] is warded against the ability of an artifact/enchantment/… source
     * [sourceId]. Returns false for spell sources, an unknown source, or when the source's card
     * type doesn't match any restriction the target carries.
     */
    fun cantBeTargetedBySourceTypeAbility(
        state: GameState,
        targetId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType
    ): Boolean {
        if (targetingSourceType == TargetingSourceType.SPELL || sourceId == null) return false
        val projected = state.projectedState
        return sourceCardTypes(state, sourceId).any { type ->
            projected.hasKeyword(targetId, keyword(type))
        }
    }
}
