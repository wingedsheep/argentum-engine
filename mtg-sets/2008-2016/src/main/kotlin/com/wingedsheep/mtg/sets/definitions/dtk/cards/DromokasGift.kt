package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dromoka's Gift
 * {4}{G}
 * Instant
 * Bolster 4. (Choose a creature with the least toughness among creatures you control and put four
 * +1/+1 counters on it.)
 */
val DromokasGift = card("Dromoka's Gift") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Bolster 4. (Choose a creature with the least toughness among creatures you control and " +
        "put four +1/+1 counters on it.)"

    spell {
        effect = Patterns.Mechanic.bolster(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Winona Nelson"
        flavorText = "The simplest gift from a dragon can be a revered accolade for a human warrior."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a356afb6-19d7-459a-84ca-090924d45387.jpg?1783938580"
    }
}
