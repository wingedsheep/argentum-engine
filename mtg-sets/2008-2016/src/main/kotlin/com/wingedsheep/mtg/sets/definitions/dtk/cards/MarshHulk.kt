package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marsh Hulk
 * {4}{B}{B}
 * Creature — Zombie Ogre
 * 4/6
 * Megamorph {6}{B}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost.
 *
 * Dragons of Tarkir is the earliest real printing; the only other is The List (PLST).
 */
val MarshHulk = card("Marsh Hulk") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Ogre"
    power = 4
    toughness = 6
    oracleText = "Megamorph {6}{B} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    // Megamorph {6}{B}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{6}{B}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Raf Sarmento"
        flavorText = "Now vengeance is his sole purpose."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff347b25-7ce9-42c4-811d-86f3e9c55d75.jpg?1783938596"
        ruling(
            "2015-02-25",
            "Turning a face-down creature with megamorph face up and putting a +1/+1 counter on it " +
                "is a special action. It doesn't use the stack and can't be responded to.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
        ruling(
            "2015-02-25",
            "Megamorph is a variant of the morph ability. You can find more information on morph on " +
                "cards with morph from the Khans of Tarkir set.",
        )
    }
}
