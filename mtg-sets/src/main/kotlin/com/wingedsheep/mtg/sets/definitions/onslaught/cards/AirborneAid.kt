package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect

/**
 * Airborne Aid
 * {3}{U}
 * Sorcery
 * Draw a card for each Bird on the battlefield.
 */
val AirborneAid = card("Airborne Aid") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each Bird on the battlefield."

    spell {
        effect = DrawCardsEffect(DynamicAmounts.creaturesWithSubtype(Subtype.BIRD))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Bradley Williams"
        flavorText = "The weights of war are heaviest on the ground."
        imageUri = "https://cards.scryfall.io/large/front/0/a/0aaa43b0-601f-4b99-a328-541b04d5696d.jpg?1593017362"
    }
}
