package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Topiary Panther — Murders at Karlov Manor #179
 * {4}{G}{G} · Creature — Plant Cat · 6/5
 *
 * Trample
 * Basic landcycling {1}{G} ({1}{G}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 *
 * Basic landcycling is the [KeywordAbility.basicLandcycling] variant of cycling — the shared
 * typecycling machinery narrows the search to *basic* land cards (not merely lands with a basic
 * land type), so a nonbasic dual like Undercity Sewers is not a legal fetch.
 */
val TopiaryPanther = card("Topiary Panther") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Cat"
    power = 6
    toughness = 5
    oracleText = "Trample\n" +
        "Basic landcycling {1}{G} ({1}{G}, Discard this card: Search your library for a basic land " +
        "card, reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.basicLandcycling(ManaCost.parse("{1}{G}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Xabi Gaztelua"
        flavorText = "The gardens around Karlov Manor are oddly bereft of birdsong."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03d0365e-6dee-4236-a997-6761e3cde90d.jpg?1783912860"
    }
}
