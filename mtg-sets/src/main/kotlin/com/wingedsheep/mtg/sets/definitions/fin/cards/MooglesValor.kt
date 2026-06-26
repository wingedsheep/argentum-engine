package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moogles' Valor
 * {3}{W}{W}
 * Instant
 *
 * For each creature you control, create a 1/2 white Moogle creature token with lifelink.
 * Then creatures you control gain indestructible until end of turn.
 *
 * The token count is evaluated as this resolves (before any tokens are made), so it counts
 * only the creatures already in play. The indestructible grant resolves afterward over
 * "creatures you control", which now also includes the freshly-created Moogles.
 */
val MooglesValor = card("Moogles' Valor") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "For each creature you control, create a 1/2 white Moogle creature token with lifelink. Then creatures you control gain indestructible until end of turn."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.creaturesYouControl(),
            power = 1,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Moogle"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/2/9/295b78dc-b26d-4e92-8f75-916566c4db14.jpg?1748704063"
        ).then(
            Patterns.Group.grantKeywordToAll(Keyword.INDESTRUCTIBLE, Filters.Group.creaturesYouControl)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Kotakan"
        flavorText = "\"Thanks, moogles! We're in your debt!\"\n—Locke Cole"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/caa838a7-60a9-4791-af5b-194f7574c4c8.jpg?1748705854"
    }
}
