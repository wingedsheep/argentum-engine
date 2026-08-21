package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Millikin — Odyssey #302
 * {2} · Artifact Creature — Construct · 0 / 1
 *
 * {T}, Mill a card: Add {C}. (Activate only as an instant. To mill a card, put the top card of your library into your graveyard.)
 *
 * Deranged Assistant's ability on an artifact body, and it is modelled the same way. The mill is
 * part of the *cost*, not the effect — `Costs.MillCard` (CostAtom.Mill). Per CR 701.17b the ability
 * can't be activated at all with an empty library, so the mill gates legal-action enumeration
 * rather than fizzling at resolution.
 *
 * Not a mana ability, despite producing mana with no target. CR 605.1a requires that a mana
 * ability's **cost** and effect not move any card to or from a library, and the mill cost does
 * exactly that — so this is an ordinary activated ability: it uses the stack, can be responded to,
 * and can't be activated while another cost is being paid. That is what the printed reminder text
 * "Activate only as an instant" now spells out; the old rules classification (a mana ability, whose
 * cost is paid on activation and can't be reversed when the spell being cast is) no longer
 * describes how the card plays. `manaAbility` is therefore deliberately left at its `false` default.
 */
val Millikin = card("Millikin") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 1
    oracleText = "{T}, Mill a card: Add {C}. (Activate only as an instant. To mill a card, put the top card of your library into your graveyard.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.MillCard)
        effect = Effects.AddColorlessMana(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "302"
        artist = "Alex Horley-Orlandelli"
        flavorText = "The toymaker frowned in bewilderment. None of his creations had ever sneezed before."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0550133b-22cf-4ecd-b89a-8c2f0beeaa22.jpg?1783945203"
    }
}
