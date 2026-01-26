package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moon Sprite
 * {1}{G}
 * Creature — Faerie
 * 1/1
 * Flying
 */
val MoonSprite = card("Moon Sprite") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Faerie"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Terese Nielsen"
        flavorText = "Dancing in the moonlight, she weaves dreams into the night."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0944759-ee9f-4ae0-9d1b-2533ff6791a2.jpg"
    }
}
