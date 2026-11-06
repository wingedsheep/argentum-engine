package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Unwavering Initiate — Amonkhet #36
 * {2}{W} · Creature — Human Warrior · Common
 * 3/2
 *
 * Vigilance
 * Embalm {4}{W}
 */
val UnwaveringInitiate = card("Unwavering Initiate") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Vigilance\n" +
        "Embalm {4}{W} ({4}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Human Warrior with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.VIGILANCE)

    embalm("{4}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a73fe35-68d6-4882-aeab-5c1e7b97b938.jpg?1783936530"
    }
}
