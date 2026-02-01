package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Charging Slateback
 * {4}{R}
 * Creature — Beast
 * 4/3
 * Charging Slateback can't block.
 * Morph {4}{R} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 *
 * Note: Morph ability not yet implemented.
 */
val ChargingSlateback = card("Charging Slateback") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 3

    staticAbility {
        ability = CantBlock(StaticTarget.SourceCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Mark Tedin"
        flavorText = "Goblins prize its hide for building rock sled runners."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2cfff37-655f-4107-abf3-e6f63d0e4de2.jpg?1562945225"
    }
}
