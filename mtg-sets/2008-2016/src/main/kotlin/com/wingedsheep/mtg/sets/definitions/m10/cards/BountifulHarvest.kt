package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bountiful Harvest
 * {4}{G}
 * Sorcery
 * You gain 1 life for each land you control.
 */
val BountifulHarvest = card("Bountiful Harvest") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You gain 1 life for each land you control."

    spell {
        effect = Effects.GainLife(DynamicAmounts.landsYouControl())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Jason Chan"
        flavorText = "Following the sun's cycle, harvest season comes once a year—unless the druids bid the sun behave otherwise."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d225382c-cc6f-4224-82b5-4309b72feb0b.jpg"
    }
}
