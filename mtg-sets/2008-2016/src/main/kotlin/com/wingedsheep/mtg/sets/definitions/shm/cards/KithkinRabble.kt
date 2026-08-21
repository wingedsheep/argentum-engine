package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kithkin Rabble
 * {3}{W}
 * Creature — Kithkin
 * * / *
 *
 * Vigilance
 * Kithkin Rabble's power and toughness are each equal to the number of white permanents you control.
 *
 * - The P/T is a characteristic-defining ability (CR 604.3): `dynamicStats`, no printed base P/T.
 * - "white **permanents**", not creatures — `GameObjectFilter.Permanent.withColor(WHITE)` counts
 *   white lands, artifacts and enchantments too, and Kithkin Rabble itself on the battlefield.
 */
val KithkinRabble = card("Kithkin Rabble") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Kithkin"
    oracleText = "Vigilance\n" +
        "Kithkin Rabble's power and toughness are each equal to the number of white permanents you control."

    keywords(Keyword.VIGILANCE)

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Permanent.withColor(Color.WHITE)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Omar Rayyan"
        flavorText = "If even the slightest hint of panic enters the thoughtweft, bakers, potters, and even medics drop their spoons and salves to take up arms."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f9a0afa-50e6-4bce-b261-4559fd31295e.jpg?1783942768"
    }
}
