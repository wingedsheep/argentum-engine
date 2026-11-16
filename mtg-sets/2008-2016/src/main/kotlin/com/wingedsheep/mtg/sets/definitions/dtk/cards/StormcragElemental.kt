package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormcrag Elemental
 * {5}{R}
 * Creature — Elemental
 * 5/5
 * Trample
 * Megamorph {4}{R}{R}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val StormcragElemental = card("Stormcrag Elemental") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Megamorph {4}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.TRAMPLE)

    // Megamorph {4}{R}{R}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{R}{R}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Ralph Horsley"
        flavorText = "The storms of Tarkir awaken more than dragons."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a906f718-635b-4d51-8896-530c52b260f7.jpg?1783938585"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
