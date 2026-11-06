package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Sacred Cat — Amonkhet #27
 * {W} · Creature — Cat · Common
 * 1/1
 *
 * Lifelink
 * Embalm {W}
 */
val SacredCat = card("Sacred Cat") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "Lifelink\n" +
        "Embalm {W} ({W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Cat with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.LIFELINK)

    embalm("{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08891c78-13c1-4d84-aa9c-78346b3b7d18.jpg?1783936533"
    }
}
