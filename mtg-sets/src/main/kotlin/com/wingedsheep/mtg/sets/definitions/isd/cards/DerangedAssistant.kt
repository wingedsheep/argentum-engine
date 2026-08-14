package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Deranged Assistant
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 *
 * {T}, Mill a card: Add {C}.
 *
 * The mill is part of the *cost*, not the effect — `Costs.MillCard` (CostAtom.Mill). Two things
 * follow from that, and both are why this can't be modelled as "add mana, then mill":
 * - Per CR 701.17b the ability can't be activated at all with an empty library, so the mill cost
 *   gates legal-action enumeration rather than fizzling at resolution.
 * - Being a mana ability, the cost is paid immediately on activation and (per ruling) can't be
 *   reversed: back out of the spell you were casting and the mana is still gone and the card is
 *   still milled.
 */
val DerangedAssistant = card("Deranged Assistant") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}, Mill a card: Add {C}. (To mill a card, put the top card of your library " +
        "into your graveyard.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.MillCard)
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Nils Hamm"
        flavorText = "\"Garl, adjust the slurry dispensers. Garl, fetch more corpses. Garl, quit " +
            "crying and give me your brain tissue. If he doesn't stop being so rude, I'm quitting.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c03171-5ff0-4f79-bb03-16decf7d34ce.jpg?1783940977"
        ruling(
            "2025-01-24",
            "Once Deranged Assistant's ability has been activated, it can't be reversed for any " +
                "reason. If you activate it while casting a spell and discover you can't produce " +
                "enough mana to pay that spell's costs, the spell is reversed, but Deranged " +
                "Assistant's ability isn't — you'll still have the mana it produced and the milled " +
                "card will still be in your graveyard."
        )
    }
}
