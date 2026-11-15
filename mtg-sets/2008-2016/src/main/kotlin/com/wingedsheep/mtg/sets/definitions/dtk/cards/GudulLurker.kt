package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gudul Lurker
 * {U}
 * Creature — Salamander
 * 1/1
 * This creature can't be blocked.
 * Megamorph {U}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val GudulLurker = card("Gudul Lurker") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Salamander"
    power = 1
    toughness = 1
    oracleText = "This creature can't be blocked.\n" +
        "Megamorph {U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    flags(AbilityFlag.CANT_BE_BLOCKED)

    // Megamorph {U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Christopher Burdett"
        flavorText = "The small are mostly ignored by dragons."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c84d5344-2d15-43da-b536-27e0d308606b.jpg?1783938608"
    }
}
