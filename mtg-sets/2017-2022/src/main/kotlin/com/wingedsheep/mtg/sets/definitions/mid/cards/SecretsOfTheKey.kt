package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Secrets of the Key
 * {U}
 * Instant
 * Investigate. If this spell was cast from a graveyard, investigate twice instead.
 * Flashback {3}{U}
 */
val SecretsOfTheKey = card("Secrets of the Key") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Investigate. If this spell was cast from a graveyard, investigate twice instead. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Flashback {3}{U}"

    spell {
        effect = Effects.If(
            condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
            then = Effects.Investigate(2),
            otherwise = Effects.Investigate(1),
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Alix Branwyn"
        flavorText = "As Sigarda and Sorin battled, Arlinn and Teferi recovered the key from the Markov catacombs."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b022ba8-07b6-4993-99c8-89c6dcb19e60.jpg?1783925633"
    }
}
