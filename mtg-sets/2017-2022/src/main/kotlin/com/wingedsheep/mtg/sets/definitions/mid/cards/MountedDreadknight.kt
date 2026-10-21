package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Mounted Dreadknight
 * {4}{R}
 * Creature — Vampire Knight
 * 5/4
 * Trample
 * This creature enters with a +1/+1 counter on it if an opponent lost life this turn.
 *
 * "Enters with a counter" is a replacement effect (CR 614.1c), gated by
 * [Conditions.OpponentLostLifeThisTurn] — not an ETB trigger.
 */
val MountedDreadknight = card("Mounted Dreadknight") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Knight"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "This creature enters with a +1/+1 counter on it if an opponent lost life this turn."

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.OpponentLostLifeThisTurn
    ))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Josu Hernaiz"
        flavorText = "Markov vampires cultivate an image of such decadent elegance that it can be easy to " +
            "forget they can field a deadly fighting force."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1814fc8-011f-4cd4-825b-f00ae6ec0fe0.jpg"
    }
}
