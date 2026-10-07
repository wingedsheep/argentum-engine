package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Beast Within
 * {2}{G}
 * Instant
 *
 * Destroy target permanent. Its controller creates a 3/3 green Beast creature token.
 */
val BeastWithin = card("Beast Within") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target permanent. Its controller creates a 3/3 green Beast creature token."

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Destroy(permanent) then
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Beast"),
                controller = EffectTarget.TargetController,
                imageUri = "https://cards.scryfall.io/normal/front/d/9/d93d0098-2147-4e84-af15-91dec8b98d21.jpg?1721427669"
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Dave Allsop"
        flavorText = "\"Kill the weak so they can't drag the strong down to their level. This is true compassion.\"\n—Benzir, archdruid of Temple Might"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce5b6d19-22e3-4f57-8f4d-a17e982286c7.jpg?1783941304"
    }
}
