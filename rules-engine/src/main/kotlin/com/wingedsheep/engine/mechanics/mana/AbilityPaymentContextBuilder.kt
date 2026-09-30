package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.actions.ability.extractManaCost
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
 * system doesn't project, which is why the base subtypes go through [paymentSubtypesOf] (so a
 * changeling source keeps every creature type outside the battlefield too).
 *
 * [ability] is the activated ability whose cost is being paid. It is a required parameter (nullable
 * rather than defaulted) so every activation site has to state what it is activating: facts about
 * the *ability* rather than its source — "is this an equip ability" (CR 702.6, for
 * [com.wingedsheep.sdk.scripting.effects.ManaRestriction.EquipAbilityActivationOnly]) and "does its
 * cost contain {X}" (for [com.wingedsheep.sdk.scripting.effects.ManaRestriction.CostsContainingXOnly])
 * — can't be recovered from [cardComponent]. Pass null only where the ability genuinely isn't resolvable
 * (a granted ability the caller can't look up); both facts then read false, i.e. the
 * restriction refuses, which is the safe direction.
 */
internal fun buildAbilityPaymentContext(
    cardComponent: CardComponent,
    projected: ProjectedState,
    sourceId: EntityId,
    ability: com.wingedsheep.sdk.scripting.ActivatedAbility?,
): SpellPaymentContext {
    val projectedTypes = projected.getTypes(sourceId)
        .mapNotNull { name -> CardType.entries.find { it.name == name } }
        .toSet()
    val cardTypes = cardComponent.typeLine.cardTypes + projectedTypes
    val subtypes = paymentSubtypesOf(cardComponent) + projected.getSubtypes(sourceId)
    return SpellPaymentContext(
        isAbilityActivation = true,
        abilitySourceCardTypes = cardTypes,
        subtypes = subtypes,
        isEquipAbilityActivation = ability?.isEquipAbility == true,
        hasXInCost = ability?.cost?.extractManaCost()?.hasX == true,
    )
}
