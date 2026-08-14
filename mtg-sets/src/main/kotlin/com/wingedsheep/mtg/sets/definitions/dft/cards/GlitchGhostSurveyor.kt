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
 * Glitch Ghost Surveyor — Aetherdrift #44
 * {2}{U} · Creature — Spirit Scout · 2/2
 *
 * Flying
 * Start your engines!
 * Max speed — {3}, Exile this card from your graveyard: Draw a card.
 *
 * The blue member of the Surveyor cycle — same graveyard-activated max-speed draw as
 * [LoxodonSurveyor], with flying instead of a second battlefield ability.
 */
val GlitchGhostSurveyor = card("Glitch Ghost Surveyor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit Scout"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — {3}, Exile this card from your graveyard: Draw a card."

    keywords(Keyword.FLYING)

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
        collectorNumber = "44"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9bb89b9-50dd-4b36-aa10-aba585e50246.jpg?1783907909"
    }
}
