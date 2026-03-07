package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * End Hostilities
 * {3}{W}{W}
 * Sorcery
 * Destroy all creatures and all permanents attached to creatures.
 */
val EndHostilities = card("End Hostilities") {
    manaCost = "{3}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures and all permanents attached to creatures."

    spell {
        effect = Effects.DestroyAllAndAttached(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Jason Rainville"
        flavorText = "\"Her palm flared like the eye of a waking dragon. Then all was calm.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80a53ed7-a7b7-40d8-9239-cf6f205dbc59.jpg?1562789330"
    }
}
