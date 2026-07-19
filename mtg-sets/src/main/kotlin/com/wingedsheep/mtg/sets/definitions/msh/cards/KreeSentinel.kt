package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Kree Sentinel (MSH #141) — {4}{R} Artifact Creature — Kree Robot Villain, 5/5
 *
 * Reach
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 *
 * Both halves are printed keywords: [Keyword.REACH] and the
 * [KeywordAbility.basicLandcycling] alternative-play ability (the same wiring as Stratosoarer)
 * — the engine owns the search/reveal/shuffle pipeline, so nothing card-specific is needed.
 */
val KreeSentinel = card("Kree Sentinel") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Kree Robot Villain"
    power = 5
    toughness = 5
    oracleText = "Reach\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.REACH)

    keywordAbility(KeywordAbility.basicLandcycling(ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Slawomir Maniak"
        flavorText = "\"The Supreme Intelligence claims this planet.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba5e441d-d036-4080-9571-6cf76df7b452.jpg?1783902928"
    }
}
