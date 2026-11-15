package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dirgur Nemesis
 * {5}{U}
 * Creature — Serpent
 * 6/5
 * Defender
 * Megamorph {6}{U}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val DirgurNemesis = card("Dirgur Nemesis") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 6
    toughness = 5
    oracleText = "Defender\n" +
        "Megamorph {6}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.DEFENDER)

    // Megamorph {6}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{6}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54c388de-c2c9-4975-9eac-86a8030a9884.jpg?1783938609"
    }
}
