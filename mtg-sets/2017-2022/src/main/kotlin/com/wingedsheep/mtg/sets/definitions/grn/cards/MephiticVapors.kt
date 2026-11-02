package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mephitic Vapors
 * {2}{B}
 * Sorcery
 * All creatures get -1/-1 until end of turn.
 * Surveil 2.
 */
val MephiticVapors = card("Mephitic Vapors") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -1/-1 until end of turn.\nSurveil 2. (Look at the top two cards of your " +
        "library, then put any number of them into your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter.AllCreatures) then
            Patterns.Library.surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Darek Zabrocki"
        flavorText = "Ravnica's safest strongholds are often neither."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/675640ba-37e7-4231-8524-87e8b87ea46f.jpg?1783934174"
    }
}
