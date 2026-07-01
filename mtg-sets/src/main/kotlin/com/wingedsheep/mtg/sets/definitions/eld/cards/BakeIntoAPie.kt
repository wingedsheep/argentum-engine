package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bake into a Pie
 * {2}{B}{B}
 * Instant
 * Destroy target creature. Create a Food token. (It's an artifact with
 * "{2}, {T}, Sacrifice this token: You gain 3 life.")
 */
val BakeIntoAPie = card("Bake into a Pie") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature. Create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val target = target("target creature", Targets.Creature)
        effect = Effects.Composite(listOf(
            Effects.Destroy(target),
            Effects.CreateFood()
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Zoltan Boros"
        flavorText = "\"My secret ingredient? Well, I can't tell you that. But here's a hint. It's not love.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42a4d090-1bb7-4334-ab22-e2527391e79b.jpg?1782707883"
    }
}
