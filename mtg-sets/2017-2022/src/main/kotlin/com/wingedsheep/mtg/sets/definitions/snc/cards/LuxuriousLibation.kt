package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Luxurious Libation
 * {X}{G}
 * Instant
 * Target creature gets +X/+X until end of turn. Create a 1/1 green and white Citizen creature token.
 *
 * The X pump Howl from Beyond makes ([DynamicAmounts.xValue] on both stats), followed by the
 * set's shared 1/1 green and white Citizen token.
 */
val LuxuriousLibation = card("Luxurious Libation") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+X until end of turn. Create a 1/1 green and white Citizen creature token."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.xValue(), creature) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Citizen"),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Joe Slucher"
        flavorText = "\"Crisp, flavorful, and energizing. Perhaps the most elegant offering of Halo in Park Heights. Five stars.\"\n—Conzuelo, restaurant critic"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3e0e818-db6a-453a-a1f0-a7fade6509d3.jpg?1783923100"
    }
}
