package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Avishkar Raceway
 * Land
 *
 * Start your engines!
 * {T}: Add {C}.
 * Max speed — {3}, {T}, Discard a card: Draw a card.
 */
val AvishkarRaceway = card("Avishkar Raceway") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{T}: Add {C}.\n" +
        "Max speed — {3}, {T}, Discard a card: Draw a card."

    startYourEngines()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.DiscardCard)
            effect = Effects.DrawCards(1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Julian Kok Joon Wen"
        flavorText = "Stage 1: The winding streets of Ghirapur."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08a6b378-c7fa-4226-a310-4ee7e550b4d6.jpg?1783907845"
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
