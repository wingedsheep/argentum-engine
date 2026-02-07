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
        imageUri = "https://cards.scryfall.io/large/front/2/a/2a02d6d5-27be-4301-a467-5b49491d0d4f.jpg?1593017428"
    }
}
