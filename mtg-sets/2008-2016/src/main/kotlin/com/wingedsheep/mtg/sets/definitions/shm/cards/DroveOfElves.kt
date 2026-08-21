package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Drove of Elves
 * {3}{G}
 * Creature — Elf
 * * / *
 *
 * Hexproof
 * Drove of Elves's power and toughness are each equal to the number of green permanents you control.
 *
 * - The P/T is a characteristic-defining ability (CR 604.3): `dynamicStats`, no printed base P/T.
 * - "green **permanents**", not creatures — `GameObjectFilter.Permanent.withColor(GREEN)` counts
 *   green lands, artifacts and enchantments too, and Drove of Elves itself while on the battlefield.
 * - Printed as "shroud" in Shadowmoor; the current Oracle text errata'd it to hexproof, which is
 *   what the brief carries and what is authored here.
 */
val DroveOfElves = card("Drove of Elves") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf"
    oracleText = "Hexproof\n" +
        "Drove of Elves's power and toughness are each equal to the number of green permanents you control."

    keywords(Keyword.HEXPROOF)

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Permanent.withColor(Color.GREEN)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Larry MacDougall"
        flavorText = "\"The light of beauty protects our journeys through darkness.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b657ad23-e84b-4b53-a6b6-80f359624ab8.jpg?1783942744"
    }
}
