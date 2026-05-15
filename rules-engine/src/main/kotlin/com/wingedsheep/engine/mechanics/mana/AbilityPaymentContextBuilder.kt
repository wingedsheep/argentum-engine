package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.EntityId

/**
 * Build a [SpellPaymentContext] for an activated ability being paid for.
 *
 * Card-types and subtypes are the union of base ([CardComponent]) and projected values so
 * mana-spending restrictions that key on the source's type (e.g.
 * [com.wingedsheep.sdk.scripting.effects.ManaRestriction.CardTypeSpellsOrAbilitiesOnly],
 * [com.wingedsheep.sdk.scripting.effects.ManaRestriction.SubtypeSpellsOrAbilitiesOnly]) see
 * the correct types even when continuous effects (Mycosynth Lattice, Sea's Claim) modify
 * them. Works for any zone — projected types simply aren't reported for entities the layer
 * system doesn't project.
 */
internal fun buildAbilityPaymentContext(
    cardComponent: CardComponent,
    projected: ProjectedState,
    sourceId: EntityId,
): SpellPaymentContext {
    val projectedTypes = projected.getTypes(sourceId)
        .mapNotNull { name -> CardType.entries.find { it.name == name } }
        .toSet()
    val cardTypes = cardComponent.typeLine.cardTypes + projectedTypes
    val subtypes = (cardComponent.typeLine.subtypes.map { it.value } +
        projected.getSubtypes(sourceId)).toSet()
    return SpellPaymentContext(
        isAbilityActivation = true,
        abilitySourceCardTypes = cardTypes,
        subtypes = subtypes,
    )
}
