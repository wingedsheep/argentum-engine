package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Border Guard
 * {2}{W}
 * Creature — Human Soldier
 * 1/4
 * (No abilities - vanilla creature)
 */
val BorderGuard = card("Border Guard") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Kev Walker"
        flavorText = "\"Join the army, see foreign countries!\" they say. I wish the countries would come to me."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985af775-2036-459d-83c6-31ac84a0ffb1.jpg"
    }
}
