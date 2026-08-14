package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Loxodon Surveyor — Aetherdrift #167
 * {2}{G} · Creature — Elephant Scout · 3/3
 *
 * Start your engines!
 * Max speed — {3}, Exile this card from your graveyard: Draw a card.
 *
 * The Surveyor cycle's graveyard ability is the one max-speed shape that functions outside the
 * battlefield, and the rules say the gate follows it there (Scryfall ruling 2025-02-07: "If the
 * granted ability functions in a zone other than the battlefield, the max speed ability does too").
 * That falls out of the vocabulary: `maxSpeed { }` gates activated abilities with
 * `ActivationRestriction.OnlyIfCondition`, and `GraveyardAbilityEnumerator` runs the same
 * restriction check as the battlefield enumerator, resolving `Player.You` to the player who would
 * activate — the card's owner while it sits in their graveyard.
 */
val LoxodonSurveyor = card("Loxodon Surveyor") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant Scout"
    power = 3
    toughness = 3
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — {3}, Exile this card from your graveyard: Draw a card."

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
        collectorNumber = "167"
        artist = "J.P. Targete"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd1cecb1-6776-4495-be56-b7dde65453f1.jpg?1783907870"
    }
}
