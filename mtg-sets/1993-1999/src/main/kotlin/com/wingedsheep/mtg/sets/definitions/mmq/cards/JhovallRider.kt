package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jhovall Rider
 * {4}{W}
 * Creature — Human Rebel
 * 3 / 3
 *
 * Trample
 */
val JhovallRider = card("Jhovall Rider") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "Trample"
    power = 3
    toughness = 3
    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Scott M. Fischer"
        flavorText = "Don't be fooled by the riders' fluid grace—it takes years of practice to ride these beasts."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1f7c51-0011-4ea5-b123-3c26293f5dab.jpg"
    }
}
