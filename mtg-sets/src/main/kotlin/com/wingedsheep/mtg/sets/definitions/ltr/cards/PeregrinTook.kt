package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Peregrin Took
 * {2}{G}
 * Legendary Creature — Halfling Citizen
 * 2/3
 *
 * If one or more tokens would be created under your control, those tokens plus an
 * additional Food token are created instead.
 * Sacrifice three Foods: Draw a card.
 *
 * The token-creation clause is a replacement effect (CR 614) that fires once per
 * token-creation event under your control, regardless of how many or what kind of tokens
 * the event makes (rulings 2023-06-16). It is self-limiting (CR 614.5): the added Food is
 * created directly and does not itself trigger another Food.
 */
val PeregrinTook = card("Peregrin Took") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Halfling Citizen"
    power = 2
    toughness = 3
    oracleText = "If one or more tokens would be created under your control, those tokens plus an additional Food token are created instead. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Sacrifice three Foods: Draw a card."

    // "those tokens plus an additional Food token are created instead." The engine creates
    // the extra Food directly after the primary tokens, without re-entering the token-creation
    // replacement pipeline, so it never recurses on its own added Food (CR 614.5).
    replacementEffect(CreateAdditionalToken(additionalTokenType = "Food"))

    // Sacrifice three Foods: Draw a card.
    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Campbell White"
        flavorText = "\"Sam! Get breakfast ready for half-past nine!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5baee8d-88e7-4468-94a9-66ca8e2caf15.jpg?1686969525"
    }
}
