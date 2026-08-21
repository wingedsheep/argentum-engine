package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Horde of Boggarts
 * {3}{R}
 * Creature — Goblin
 * * / *
 *
 * Menace (This creature can't be blocked except by two or more creatures.)
 * Horde of Boggarts's power and toughness are each equal to the number of red permanents you control.
 *
 * - The P/T is a characteristic-defining ability (CR 604.3): `dynamicStats`, no printed base P/T.
 * - "red **permanents**", not creatures — `GameObjectFilter.Permanent.withColor(RED)` counts red
 *   lands, artifacts and enchantments too, and Horde of Boggarts itself while on the battlefield.
 * - Printed with "can't be blocked except by two or more creatures"; the current Oracle text
 *   errata'd that to the menace keyword, which is what is authored here.
 */
val HordeOfBoggarts = card("Horde of Boggarts") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Horde of Boggarts's power and toughness are each equal to the number of red permanents you control."

    keywords(Keyword.MENACE)

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Permanent.withColor(Color.RED)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Steve Prescott"
        flavorText = "Strategies don't come easily to the boggarts' feral minds, but full-on assault hasn't failed them yet."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/023562bf-da7d-486d-93fc-60353f55b8a7.jpg?1783942749"
    }
}
