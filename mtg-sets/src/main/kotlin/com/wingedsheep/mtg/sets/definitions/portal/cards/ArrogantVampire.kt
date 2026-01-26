package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Arrogant Vampire
 * {3}{B}{B}
 * Creature — Vampire
 * 4/3
 * Flying
 */
val ArrogantVampire = card("Arrogant Vampire") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 3
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7342875-d49b-4fa7-a2fb-8dfc5e3d6e4f.jpg"
    }
}
