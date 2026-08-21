package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wasp Lancer
 * {U/B}{U/B}{U/B}
 * Creature — Faerie Soldier
 * 3 / 2
 *
 * Flying
 *
 * - Keyword-only creature: [Keyword.FLYING] is engine-live, so no scripted ability is needed.
 */
val WaspLancer = card("Wasp Lancer") {
    manaCost = "{U/B}{U/B}{U/B}"
    typeLine = "Creature — Faerie Soldier"
    power = 3
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Warren Mahy"
        flavorText = "\"I doubt that faeries understand how short their lives are, compared to the rest of us. If they did, would they so readily charge into battle, heedless of the danger before them?\"\n" +
            "—Awylla, elvish safewright"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac48a4ff-433b-4fd0-b0d1-43b188ee81b6.jpg?1783942728"
    }
}
