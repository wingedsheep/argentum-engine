package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ardent Militia
 * {4}{W}
 * Creature — Human Soldier
 * 2/5
 * Vigilance
 */
val ArdentMilitia = card("Ardent Militia") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 5
    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Mike Raabe"
        flavorText = "Some fight for honor and some for gold. We fight for those who cannot."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/543f8c6a-bcf1-4400-82e5-83d36cb60464.jpg"
    }
}
