package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Whelming Wave
 * {2}{U}{U}
 * Sorcery
 * Return all creatures to their owners' hands except for Krakens, Leviathans, Octopuses, and Serpents.
 */
val WhelmingWave = card("Whelming Wave") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all creatures to their owners' hands except for Krakens, Leviathans, Octopuses, and Serpents."

    spell {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.Creature
                    .notSubtype(Subtype.KRAKEN)
                    .notSubtype(Subtype.LEVIATHAN)
                    .notSubtype(Subtype.OCTOPUS)
                    .notSubtype(Subtype.SERPENT)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Slawomir Maniak"
        flavorText = "\"I can see why this appeals to Thassa.\"\n—Kiora"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcabd4c7-093f-4ef6-8b89-b08565c48e3c.jpg?1783939563"
    }
}
