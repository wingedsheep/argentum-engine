package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stormcaller of Keranos
 * {2}{R}
 * Creature — Human Shaman
 * 2/2
 *
 * Haste
 * {1}{U}: Scry 1.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val StormcallerOfKeranos = card("Stormcaller of Keranos") {
    manaCost = "{2}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Haste\n{1}{U}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Marco Nelor"
        flavorText = "A prayer answered by Keranos is clear for all to witness."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d397bdc3-a866-45be-8740-185d9ccf29c4.jpg?1783939539"
    }
}
