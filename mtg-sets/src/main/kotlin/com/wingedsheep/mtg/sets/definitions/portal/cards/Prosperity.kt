package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachPlayerDrawsXEffect

/**
 * Prosperity
 * {X}{U}
 * Sorcery
 * Each player draws X cards.
 */
val Prosperity = card("Prosperity") {
    manaCost = "{X}{U}"
    typeLine = "Sorcery"

    spell {
        effect = EachPlayerDrawsXEffect(
            includeController = true,
            includeOpponents = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69ebc3a4-94b8-45aa-a3bf-21cd435ff5cf.jpg"
    }
}
