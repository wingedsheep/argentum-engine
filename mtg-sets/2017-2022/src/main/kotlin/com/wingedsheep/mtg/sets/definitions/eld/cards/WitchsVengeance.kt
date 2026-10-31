package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Witch's Vengeance
 * {1}{B}{B}
 * Sorcery
 * Creatures of the creature type of your choice get -3/-3 until end of turn.
 */
val WitchsVengeance = card("Witch's Vengeance") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Creatures of the creature type of your choice get -3/-3 until end of turn."

    spell {
        effect = Effects.ChooseCreatureTypeModifyStats(
            powerModifier = -3,
            toughnessModifier = -3
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Titus Lunter"
        flavorText = "The hex swept through the village, its citizens falling one by one."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbf16457-3444-4130-b220-834b69d9faa3.jpg?1783932629"
    }
}
