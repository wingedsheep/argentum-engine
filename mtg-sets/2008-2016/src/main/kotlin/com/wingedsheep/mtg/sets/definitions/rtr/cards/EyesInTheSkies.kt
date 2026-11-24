package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Eyes in the Skies
 * {3}{W}
 * Instant
 *
 * Create a 1/1 white Bird creature token with flying, then populate. (Create a token that's a copy
 * of a creature token you control.)
 *
 * Same shape as Coursers' Accord: [Effects.CreateToken] then populate via
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens you control (chosen, not targeted).
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val EyesInTheSkies = card("Eyes in the Skies") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Create a 1/1 white Bird creature token with flying, then populate. (Create a token " +
        "that's a copy of a creature token you control.)"

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
        ) then Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "James Ryman"
        flavorText = "\"What root and branch cannot reach, our winged friends will attend to.\"\n—Zunak, Selesnya strategist"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/befef095-3429-4dd7-aa01-2f7f619675d4.jpg?1783940377"
    }
}
