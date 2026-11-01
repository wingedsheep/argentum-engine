package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Clear the Mind
 * {2}{U}
 * Sorcery
 * Target player shuffles their graveyard into their library.
 * Draw a card.
 */
val ClearTheMind = card("Clear the Mind") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player shuffles their graveyard into their library.\nDraw a card."

    spell {
        val t = target(Targets.Player)
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "David Palumbo"
        flavorText = "\"The best way to keep a secret is to forget it.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7da6982-9e57-41d2-a052-f2a3bb646436.jpg?1783933712"

        ruling(
            "2019-01-25",
            "Clear the Mind can target a player with no cards in their graveyard. " +
                "That player shuffles their library and then you draw a card.",
        )
        ruling(
            "2019-01-25",
            "If you choose yourself as the target for Clear the Mind, Clear the Mind will end up as the only " +
                "card in your graveyard after it’s done resolving.",
        )
    }
}
