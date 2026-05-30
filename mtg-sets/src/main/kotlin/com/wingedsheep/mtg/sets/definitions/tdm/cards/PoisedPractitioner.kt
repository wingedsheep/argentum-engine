package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Poised Practitioner
 * {2}{W}
 * Creature — Human Monk
 * 2/3
 *
 * Flurry — Whenever you cast your second spell each turn, put a +1/+1 counter on this
 * creature. Scry 1.
 */
val PoisedPractitioner = card("Poised Practitioner") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 3
    oracleText = "Flurry — Whenever you cast your second spell each turn, put a +1/+1 counter on this creature. Scry 1."

    flurry {
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            .then(EffectPatterns.scry(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Alessandra Pisano"
        flavorText = "\"Excellent form, Jun! Just keep your weight on that back foot, and you'll master this in no time.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb25366d-a647-4c5e-bcc7-7e54659aacbd.jpg?1743204021"
    }
}
