package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Mockingbird
 * {X}{U}
 * Creature — Bird Bard
 * 1/1
 *
 * Flying
 * You may have this creature enter as a copy of any creature on the battlefield
 * with mana value less than or equal to the amount of mana spent to cast this creature,
 * except it's a Bird in addition to its other types and it has flying.
 */
val Mockingbird = card("Mockingbird") {
    manaCost = "{X}{U}"
    typeLine = "Creature — Bird Bard"
    power = 1
    toughness = 1
    oracleText = "Flying\nYou may have this creature enter as a copy of any creature on the battlefield with mana value less than or equal to the amount of mana spent to cast this creature, except it's a Bird in addition to its other types and it has flying."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            filterByTotalManaSpent = true,
            additionalSubtypes = listOf("Bird"),
            additionalKeywords = listOf(Keyword.FLYING)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Aurore Folny"
        flavorText = "\"Laughing at you? No, no, I'm laughing *as* you!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ade32396-8841-4ba4-8852-d11146607f21.jpg?1722388218"
    }
}
