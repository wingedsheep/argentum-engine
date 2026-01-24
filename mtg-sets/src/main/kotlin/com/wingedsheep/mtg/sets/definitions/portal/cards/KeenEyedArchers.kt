package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Keen-Eyed Archers
 * {2}{W}
 * Creature — Elf Archer
 * 2/2
 * Reach
 */
val KeenEyedArchers = card("Keen-Eyed Archers") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Elf Archer"
    power = 2
    toughness = 2
    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Alan Rabinowitz"
        flavorText = "If it has wings, shoot it. If it doesn't, shoot it anyway."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/594de429-58ed-4a0c-9631-464cde7a48c3.jpg"
    }
}
