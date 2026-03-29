package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
/**
 * Head of the Homestead
 * {3}{G/W}{G/W}
 * Creature — Rabbit Citizen
 * 3/2
 * When this creature enters, create two 1/1 white Rabbit creature tokens.
 */
val HeadOfTheHomestead = card("Head of the Homestead") {
    manaCost = "{3}{G/W}{G/W}"
    typeLine = "Creature — Rabbit Citizen"
    oracleText = "When this creature enters, create two 1/1 white Rabbit creature tokens."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Rabbit"),
            imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1721431122"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Omar Rayyan"
        flavorText = "\"Interrupting a rabbitfolk dinner is a most grievous offense. If you must do so, bring gifts for the little ones, or bring a shield.\"\n—Giddy's Guide to Valley"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fc20157-edd3-484d-8864-925c071c0551.jpg?1721427071"
    }
}
