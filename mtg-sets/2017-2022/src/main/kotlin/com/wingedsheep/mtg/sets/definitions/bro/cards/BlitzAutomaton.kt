package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Blitz Automaton
 * {7}
 * Artifact Creature — Construct
 * 6/4
 * Prototype {2}{R} — 3/2
 * Haste
 */
val BlitzAutomaton = card("Blitz Automaton") {
    manaCost = "{7}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 6
    toughness = 4
    oracleText = "Prototype {2}{R} — 3/2 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Haste"

    keywordAbility(KeywordAbility.prototype("{2}{R}", 3, 2))

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Simon Dominic"
        flavorText = "While Urza's machines favored defense, Mishra built his to end the war quickly."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4e5529a-848b-486c-97fb-1def35df7837.jpg?1783920058"
    }
}
