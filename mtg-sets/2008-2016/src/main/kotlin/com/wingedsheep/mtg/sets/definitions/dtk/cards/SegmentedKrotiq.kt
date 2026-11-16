package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Segmented Krotiq
 * {5}{G}
 * Creature — Insect
 * 6/5
 * Megamorph {6}{G}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val SegmentedKrotiq = card("Segmented Krotiq") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 6
    toughness = 5
    oracleText = "Megamorph {6}{G} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    // Megamorph {6}{G}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{6}{G}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Christopher Moeller"
        flavorText = "The list of things a krotiq eats is as long as the krotiq itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcdbe824-f9c7-4f4d-af92-438b16057d99.jpg?1783938576"
    }
}
