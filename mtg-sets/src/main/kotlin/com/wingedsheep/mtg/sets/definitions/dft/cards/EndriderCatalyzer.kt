package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Endrider Catalyzer
 * {1}{R}
 * Creature — Human Warrior
 * 3/1
 * Start your engines! (If you have no speed, it starts at 1. It increases once on each of your turns when an opponent loses life. Max speed is 4.)
 * Max speed — {T}: Add {R}{R}.
 */
val EndriderCatalyzer = card("Endrider Catalyzer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — {T}: Add {R}{R}."
    power = 3
    toughness = 1
    startYourEngines()
    maxSpeed {
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.RED, 2)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Karl Kopinski"
        flavorText = "Endrider magic was developed on a world that was constantly on the move—the " +
            "higher the velocity, the stronger the magic."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55a5a67b-d969-4ba4-9dcc-32d0c2e5c04a.jpg"
    }
}
