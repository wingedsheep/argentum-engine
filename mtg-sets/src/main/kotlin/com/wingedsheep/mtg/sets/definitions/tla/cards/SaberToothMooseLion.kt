package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Saber-Tooth Moose-Lion
 * {4}{G}{G}
 * Creature — Elk Cat
 * 7/7
 * Reach
 * Forestcycling {2} ({2}, Discard this card: Search your library for a Forest card,
 * reveal it, put it into your hand, then shuffle.)
 */
val SaberToothMooseLion = card("Saber-Tooth Moose-Lion") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elk Cat"
    power = 7
    toughness = 7
    oracleText = "Reach\nForestcycling {2} ({2}, Discard this card: Search your library for a " +
        "Forest card, reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.REACH)
    keywordAbility(KeywordAbility.typecycling("Forest", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Shiren"
        flavorText = "Foo Foo Cuddlypoops didn't look much like a saber-tooth moose-lion to Sokka, but his mother certainly did."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b15f302d-c451-4e79-a5af-75ee3ba4cadf.jpg?1764121320"
    }
}
