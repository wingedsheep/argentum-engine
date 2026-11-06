package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Tah-Crop Skirmisher — Amonkhet #72
 * {1}{U} · Creature — Snake Warrior · Common
 * 2/1
 *
 * Embalm {3}{U}
 */
val TahCropSkirmisher = card("Tah-Crop Skirmisher") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Warrior"
    power = 2
    toughness = 1
    oracleText = "Embalm {3}{U} ({3}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Snake Warrior with no mana cost. Embalm only as a sorcery.)"

    embalm("{3}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Victor Adame Minguez"
        flavorText = "Initiates live and train with their crop, the unit that begins the trials together."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab0429ec-0809-4e68-9790-8c21bde201a2.jpg?1783936514"
    }
}
