package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantShroudUntilEndOfTurnEffect

/**
 * Gilded Light
 * {1}{W}
 * Instant
 * You gain shroud until end of turn. (You can't be the target of spells or abilities.)
 * Cycling {2}
 */
val GildedLight = card("Gilded Light") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "You gain shroud until end of turn. (You can't be the target of spells or abilities.)\nCycling {2}"

    spell {
        effect = GrantShroudUntilEndOfTurnEffect()
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "John Avon"
        flavorText = "\"Whoever survives the first blow lives to land the second.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01b92597-cb1e-4b8f-9ee9-07b48cf1a5c6.jpg?1562524890"
    }
}
