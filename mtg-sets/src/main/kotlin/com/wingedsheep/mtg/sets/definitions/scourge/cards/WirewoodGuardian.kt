package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Wirewood Guardian
 * {5}{G}{G}
 * Creature — Elf Mutant
 * 6/6
 * Forestcycling {2} ({2}, Discard this card: Search your library for a Forest card,
 * reveal it, put it into your hand, then shuffle.)
 */
val WirewoodGuardian = card("Wirewood Guardian") {
    manaCost = "{5}{G}{G}"
    typeLine = "Creature — Elf Mutant"
    power = 6
    toughness = 6
    oracleText = "Forestcycling {2}"

    keywordAbility(KeywordAbility.Typecycling("Forest", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Carl Critchlow"
        flavorText = "The elite among the Wirewood elves are not the most agile—they're the most massive."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8676b1f-e37c-4ae1-9dbe-d000369fa422.jpg?1562536268"
    }
}
