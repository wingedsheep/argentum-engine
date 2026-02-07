package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetLifeTotalForEachPlayerEffect

/**
 * Biorhythm
 * {6}{G}{G}
 * Sorcery
 * Each player's life total becomes the number of creatures they control.
 */
val Biorhythm = card("Biorhythm") {
    manaCost = "{6}{G}{G}"
    typeLine = "Sorcery"

    spell {
        effect = SetLifeTotalForEachPlayerEffect(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Ron Spears"
        flavorText = "\"I have seen life's purpose, and now it is my own.\"\n—Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17d1a10f-ce21-4571-948c-d6b9c3db3805.jpg?1562898858"
    }
}
