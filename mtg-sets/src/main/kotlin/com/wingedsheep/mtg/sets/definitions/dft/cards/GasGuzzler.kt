package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gas Guzzler
 * {B}
 * Creature — Vampire Rogue
 * 2/1
 * Start your engines!
 * This creature enters tapped.
 * Max speed — {B}, Sacrifice another creature or Vehicle: Draw a card.
 *
 * "Another creature or Vehicle" is [Costs.SacrificeAnother] over
 * [GameObjectFilter.CreatureOrVehicle] — the Vehicle half matches by subtype, so an uncrewed
 * (noncreature) Vehicle is a legal sacrifice.
 */
val GasGuzzler = card("Gas Guzzler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "This creature enters tapped.\n" +
        "Max speed — {B}, Sacrifice another creature or Vehicle: Draw a card."
    power = 2
    toughness = 1

    startYourEngines()

    replacementEffect(EntersTapped())

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{B}"),
                Costs.SacrificeAnother(GameObjectFilter.CreatureOrVehicle)
            )
            effect = Effects.DrawCards(1)
            description = "{B}, Sacrifice another creature or Vehicle: Draw a card"
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Yohann Schepacz"
        flavorText = "Gastal's vampires took fuel from the dead."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4db3a28c-e4b4-4b18-8d56-e3842184d105.jpg?1783907896"
        ruling("2025-02-07", "A player \"has max speed\" if their speed is 4.")
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
    }
}
