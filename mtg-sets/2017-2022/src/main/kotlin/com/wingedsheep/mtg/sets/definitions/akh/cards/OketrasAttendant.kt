package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Oketra's Attendant
 * {3}{W}{W}
 * Creature — Bird Soldier
 * 3/3
 * Flying
 * Cycling {2}
 * Embalm {3}{W}{W}
 */
val OketrasAttendant = card("Oketra's Attendant") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)\n" +
        "Embalm {3}{W}{W} ({3}{W}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Bird Soldier with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.FLYING)

    keywordAbility(KeywordAbility.cycling("{2}"))

    embalm("{3}{W}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6074fe64-8a2d-411e-8d33-97b1c60633e4.jpg?1783936537"
    }
}
