package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * High Stride
 * {G}
 * Instant
 * Target creature gets +1/+3 and gains reach until end of turn. Untap it.
 */
val HighStride = card("High Stride") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+3 and gains reach until end of turn. Untap it."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.ModifyStats(1, 3, t)
            .then(Effects.GrantKeyword(Keyword.REACH, t))
            .then(Effects.Untap(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Dan Murayama Scott"
        flavorText = "\"It takes a moment to find your stride, but once you do, you'll feel like a giant!\"\n—Brisco, river guide"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09c8cf4b-8e65-4a1c-b458-28b5ab56b390.jpg?1721426829"
    }
}
