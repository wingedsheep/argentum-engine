package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Regal Unicorn
 * {2}{W}
 * Creature - Unicorn
 * 2/3
 */
val RegalUnicorn = card("Regal Unicorn") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Unicorn"
    power = 2
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Zina Saunders"
        flavorText = "\"Unicorns don't care if you believe in them any more than you care if they believe in you.\"\n—Hanna, ship's navigator"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa1fb8c-12fa-4e9c-979f-55e89356acaf.jpg"
    }
}
