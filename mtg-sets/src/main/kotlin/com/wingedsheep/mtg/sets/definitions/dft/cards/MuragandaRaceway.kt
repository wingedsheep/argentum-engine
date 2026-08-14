package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Muraganda Raceway
 * Land
 *
 * Start your engines!
 * {T}: Add {C}.
 * Max speed — {T}: Add {C}{C}.
 */
val MuragandaRaceway = card("Muraganda Raceway") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{T}: Add {C}.\n" +
        "Max speed — {T}: Add {C}{C}."

    startYourEngines()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(2)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Brian Valeza"
        flavorText = "Stage 3: The steaming jungles of Muraganda under falling skies."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5041ae16-29ff-4ad5-8a37-4736e9409294.jpg?1783907840"
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
