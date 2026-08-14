package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Surveyor — Aetherdrift #131
 * {2}{R} · Creature — Goblin Scout · 3/2
 *
 * Trample
 * Start your engines!
 * Max speed — {3}, Exile this card from your graveyard: Draw a card.
 *
 * The red member of the Surveyor cycle; see Loxodon Surveyor for why the max-speed gate follows
 * the ability into the graveyard (Scryfall ruling 2025-02-07: "If the granted ability functions in
 * a zone other than the battlefield, the max speed ability does too").
 */
val GoblinSurveyor = card("Goblin Surveyor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Scout"
    power = 3
    toughness = 2
    oracleText = "Trample\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — {3}, Exile this card from your graveyard: Draw a card."

    keywords(Keyword.TRAMPLE)
    startYourEngines()

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{3}"), Costs.ExileSelf)
            effect = Effects.DrawCards(1)
            activateFromZone = Zone.GRAVEYARD
            description = "{3}, Exile this card from your graveyard: Draw a card."
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1efffe9-00f8-4177-a9e6-4ad62887d32f.jpg?1783907881"
    }
}
