package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Initiate — Amonkhet #43
 * {3}{U} · Creature — Bird Warrior · Common
 * 3/2
 *
 * Flying
 * Embalm {6}{U}
 */
val AvenInitiate = card("Aven Initiate") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Warrior"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "Embalm {6}{U} ({6}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Bird Warrior with no mana cost. Embalm only as a sorcery.)"

    keywords(Keyword.FLYING)

    embalm("{6}{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61203781-24ad-4bc9-9b69-97149f3043bc.jpg?1783936526"
    }
}
