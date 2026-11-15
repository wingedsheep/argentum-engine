package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sandstorm Charger
 * {4}{W}
 * Creature — Beast
 * 3/4
 * Megamorph {4}{W}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val SandstormCharger = card("Sandstorm Charger") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "Megamorph {4}{W} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    // Megamorph {4}{W}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{W}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Dave Kendall"
        flavorText = "\"May our foes choke upon its horns!\"\n—Urdnan, Dromoka warrior"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9757be26-4480-43b7-a38a-8e4bde4e2d50.jpg?1783938612"
    }
}
