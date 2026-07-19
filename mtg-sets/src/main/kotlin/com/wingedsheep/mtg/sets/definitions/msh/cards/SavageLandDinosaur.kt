package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Savage Land Dinosaur — Marvel Super Heroes #185
 * {4}{G}{G} · Creature — Dinosaur · Common
 * 7/6
 *
 * Trample
 * Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.)
 *
 * Basic landcycling is the [KeywordAbility.basicLandcycling] variant of cycling — the engine's
 * shared typecycling machinery with a "basic land card" search filter.
 */
val SavageLandDinosaur = card("Savage Land Dinosaur") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 7
    toughness = 6
    oracleText = "Trample\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land " +
        "card, reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.basicLandcycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Josu Hernaiz"
        flavorText = "\"Sometimes small things spook them.\"\n—Ka-Zar of the Savage Land"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/562bd376-3fba-4de0-b9fc-183004b03732.jpg?1783902912"
    }
}
