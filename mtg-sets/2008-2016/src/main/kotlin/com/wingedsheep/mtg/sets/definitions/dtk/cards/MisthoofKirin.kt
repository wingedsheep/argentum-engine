package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Misthoof Kirin
 * {2}{W}
 * Creature — Kirin
 * 2/1
 * Flying, vigilance
 * Megamorph {1}{W}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val MisthoofKirin = card("Misthoof Kirin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kirin"
    power = 2
    toughness = 1
    oracleText = "Flying, vigilance\n" +
        "Megamorph {1}{W} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    // Megamorph {1}{W}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{1}{W}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccef8595-96e8-4df3-aa5a-9fc8c5fa9a3d.jpg?1783938615"
    }
}
