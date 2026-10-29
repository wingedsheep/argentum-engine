package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Run Afoul
 * {G}
 * Instant
 * Target opponent sacrifices a creature of their choice with flying.
 */
val RunAfoul = card("Run Afoul") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target opponent sacrifices a creature of their choice with flying."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature.withKeyword(Keyword.FLYING), 1, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Cristi Balanescu"
        flavorText = "\"What did you catch today, my pet? Ah, it looks like a bit of vampire stuck between your teeth.\"\n—Atla Palani, nest tender"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd328139-0dc1-403b-ad79-b1cf3479ac33.jpg?1783930669"
    }
}
