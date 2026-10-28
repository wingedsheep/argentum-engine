package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Horta
 * {4}{R}{R}
 * Creature — Elemental
 * 7/6
 *
 * Trample
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 */
val Horta = card("Horta") {
    manaCost = "{4}{R}{R}"
    typeLine = "Creature — Elemental"
    colorIdentity = "R"
    oracleText = "Trample\nBasic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"
    power = 7
    toughness = 6
    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Arthur Haas"
        flavorText = "Before miners discovered the Horta on Janus VI, silicon-based life was considered little more than science fiction."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abf44662-62ad-47fb-9cfe-6bed47133de5.jpg?1785981533"
    }
}

