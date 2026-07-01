package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Exsanguinate
 * {X}{B}{B}
 * Sorcery
 * Each opponent loses X life. You gain life equal to the life lost this way.
 */
val Exsanguinate = card("Exsanguinate") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent loses X life. You gain life equal to the life lost this way."

    spell {
        effect = Effects.DrainLife(DynamicAmount.XValue)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Carl Critchlow"
        flavorText = "Vampires don't consider patience a virtue nor gluttony a sin."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0878b541-a730-49db-b062-5a01656e269d.jpg?1782715331"
    }
}
