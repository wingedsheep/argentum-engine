package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity

/**
 * Monastery Swiftspear
 * {R}
 * Creature — Human Monk
 * 1/2
 * Haste
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val MonasterySwiftspear = card("Monastery Swiftspear") {
    manaCost = "{R}"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "Haste\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.HASTE)
    prowess()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Steve Argyle"
        flavorText = "The dragon has no fire that matches her own."
        imageUri = "https://cards.scryfall.io/large/front/b/8/b81c6c8b-a9cf-4866-89ba-7f8ad077b836.jpg?1562792493"
    }
}
