package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Lab Rats — Stronghold #61 (canonical printing)
 * {B} · Sorcery
 *
 * Buyback {4}
 * Create a 1/1 black Rat creature token.
 */
val LabRats = card("Lab Rats") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Buyback {4} (You may pay an additional {4} as you cast this spell. If you do, put this card into your hand as it resolves.)\n" +
        "Create a 1/1 black Rat creature token."

    keywordAbility(KeywordAbility.buyback("{4}"))

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3132c128-e0bd-4524-9526-914b3c7181fc.jpg?1783946562"
    }
}
