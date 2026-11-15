package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aven Sunstriker
 * {1}{W}{W}
 * Creature — Bird Warrior
 * 1/1
 * Flying
 * Double strike
 * Megamorph {4}{W}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val AvenSunstriker = card("Aven Sunstriker") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Double strike (This creature deals both first-strike and regular combat damage.)\n" +
        "Megamorph {4}{W} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.FLYING, Keyword.DOUBLE_STRIKE)

    // Megamorph {4}{W}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{W}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a54b36b-5a8d-41a1-9371-1a59d81c2733.jpg?1783938620"
    }
}
