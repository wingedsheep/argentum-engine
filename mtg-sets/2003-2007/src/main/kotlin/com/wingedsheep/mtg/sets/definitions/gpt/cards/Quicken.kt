package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Quicken
 * {U}
 * Instant
 *
 * The next sorcery spell you cast this turn can be cast as though it had flash.
 * (It can be cast any time you could cast an instant.)
 * Draw a card.
 */
val Quicken = card("Quicken") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "The next sorcery spell you cast this turn can be cast as though it had flash. " +
        "(It can be cast any time you could cast an instant.)\nDraw a card."

    spell {
        effect = Effects.GrantFlashToNextSpell(GameObjectFilter.Sorcery) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Aleksi Briclot"
        flavorText = "A skilled Izzet chronarch can carry out an epic vendetta between the fall of one " +
            "hourglass grain and the next."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a276b12-4647-4223-b89e-f55d72feb2d0.jpg?1783943518"
    }
}
