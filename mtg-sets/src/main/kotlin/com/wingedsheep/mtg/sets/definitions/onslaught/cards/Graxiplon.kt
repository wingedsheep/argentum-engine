package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType

/**
 * Graxiplon
 * {5}{U}
 * Creature — Beast
 * 3/4
 * Graxiplon can't be blocked unless defending player controls three or more
 * creatures that share a creature type.
 */
val Graxiplon = card("Graxiplon") {
    manaCost = "{5}{U}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4

    staticAbility {
        ability = CantBeBlockedUnlessDefenderSharesCreatureType(minSharedCount = 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Iain McCaig"
        flavorText = "It might be the apex predator, or it might be the missing link."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c16e565-0b7f-46b1-a091-64c47c923a9f.jpg?1562897530"
    }
}
