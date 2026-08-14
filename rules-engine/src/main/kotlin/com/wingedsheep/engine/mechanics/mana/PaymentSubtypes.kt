package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype

/**
 * The subtypes an object effectively has when evaluating mana-spending restrictions.
 *
 * Changeling makes a card every creature type in every zone, so a Shapeshifter spell on the
 * stack (Firdoch Core) is an Elf spell, a Goblin spell, … and restricted mana keyed to a chosen
 * creature type (Eclipsed Realms, Cavern of Souls, Unclaimed Territory) must be spendable on it.
 * The printed type line alone doesn't say so — spells aren't projected by the layer system, so
 * the expansion the projector does for battlefield permanents has to be redone here.
 */
internal fun paymentSubtypesOf(cardComponent: CardComponent): Set<String> {
    val printed = cardComponent.typeLine.subtypes.map { it.value }.toSet()
    return if (Keyword.CHANGELING in cardComponent.baseKeywords) {
        printed + Subtype.ALL_CREATURE_TYPES
    } else {
        printed
    }
}

/**
 * The mana-spending context for casting the spell described by [cardComponent] — the shape every
 * cast path (enumeration, validation, payment) needs so conditional mana ("spend this mana only
 * to …") is judged against the same characteristics.
 *
 * [SpellPaymentContext.manaValue] is the card's *printed* mana value: cost reductions and
 * alternative payments like convoke pay part of a cost, they never change the spell's mana value
 * (CR 202.3), so a {3}{R}{R} spell convoked down to one real mana is still MV 5 for
 * [com.wingedsheep.sdk.scripting.effects.ManaRestriction.SpellsWithManaValueAtLeast] (Ashling, Rimebound).
 */
internal fun spellPaymentContextFor(
    cardComponent: CardComponent,
    isKicked: Boolean = false,
    isFromExile: Boolean = false,
    isFromHand: Boolean = true
): SpellPaymentContext = SpellPaymentContext(
    isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
    isKicked = isKicked,
    isCreature = cardComponent.typeLine.isCreature,
    isLegendary = cardComponent.typeLine.isLegendary,
    manaValue = cardComponent.manaCost.cmc,
    hasXInCost = cardComponent.manaCost.hasX,
    subtypes = paymentSubtypesOf(cardComponent),
    isFromExile = isFromExile,
    isFromHand = isFromHand,
    cardTypes = cardComponent.typeLine.cardTypes,
)
