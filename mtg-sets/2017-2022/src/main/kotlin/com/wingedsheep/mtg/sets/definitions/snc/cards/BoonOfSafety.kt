package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boon of Safety
 * {W}
 * Instant
 * Put a shield counter on target creature. (If it would be dealt damage or destroyed, remove a
 * shield counter from it instead.)
 * Scry 1.
 */
val BoonOfSafety = card("Boon of Safety") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Put a shield counter on target creature. (If it would be dealt damage or destroyed, " +
        "remove a shield counter from it instead.)\nScry 1."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.SHIELD, 1, t) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Robin Olausson"
        flavorText = "\"Saving a stranger's life is an excellent recruitment method. Luckily, peril is so " +
            "easy to come by.\"\n—Falco Spara"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2069310a-9fb6-41b7-bbd5-849e3df05632.jpg?1783923162"
    }
}
