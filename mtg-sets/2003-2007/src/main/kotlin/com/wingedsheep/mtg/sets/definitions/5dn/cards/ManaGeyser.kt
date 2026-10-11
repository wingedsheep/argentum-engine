package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mana Geyser
 * {3}{R}{R}
 * Sorcery
 * Add {R} for each tapped land your opponents control.
 */
val ManaGeyser = card("Mana Geyser") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Add {R} for each tapped land your opponents control."

    spell {
        effect = Effects.AddMana(
            Color.RED,
            DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Land.tapped()).count()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Martina Pilcerova"
        flavorText = "The Quicksilver Sea hissed and bubbled at the indignity. The Vulshok shaman just smiled."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3929662e-99d7-48e9-afac-1852af8be722.jpg?1783944392"
    }
}
