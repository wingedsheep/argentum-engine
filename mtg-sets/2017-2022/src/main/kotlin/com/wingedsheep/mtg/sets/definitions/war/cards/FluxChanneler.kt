package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flux Channeler
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 * Whenever you cast a noncreature spell, proliferate.
 */
val FluxChanneler = card("Flux Channeler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a noncreature spell, proliferate. (Choose any number of permanents " +
        "and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Heonhwa"
        flavorText = "\"Inform our allies in the Tenth and throw the circuit. It's about to get weird.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6202e82-e159-482f-9224-292cb533171c.jpg?1783933466"
        ruling(
            "2019-05-03",
            "Flux Channeler's ability resolves before the spell that caused it to trigger. It resolves even " +
                "if that spell is countered."
        )
    }
}
