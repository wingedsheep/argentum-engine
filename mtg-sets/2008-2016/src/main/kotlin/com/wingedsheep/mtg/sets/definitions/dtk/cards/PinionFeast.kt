package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pinion Feast
 * {4}{G}
 * Instant
 * Destroy target creature with flying. Bolster 2. (Choose a creature with the least toughness among
 * creatures you control and put two +1/+1 counters on it.)
 */
val PinionFeast = card("Pinion Feast") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target creature with flying. Bolster 2. (Choose a creature with the least " +
        "toughness among creatures you control and put two +1/+1 counters on it.)"

    spell {
        val flier = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.Destroy(flier) then Patterns.Mechanic.bolster(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Ryan Barger"
        flavorText = "\"Even paradise is not without tragedies.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45d6df03-c3c3-42c3-85a4-6fccb0741592.jpg?1783938577"
    }
}
