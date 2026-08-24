package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.FlipAdditionalCoins

/**
 * Krark's Thumb
 * {2}
 * Legendary Artifact
 *
 * If you would flip a coin, instead flip two coins and ignore one.
 *
 * The engine's [FlipAdditionalCoins] coin-flip replacement (CR 614), consulted by `CoinFlipService`
 * wherever a coin is flipped. Three of the card's rulings are what make that a *per-coin*
 * replacement rather than a per-instruction one, and the engine follows each:
 *
 * - "Flip five coins" becomes five *pairs* with one kept from each — not ten coins with any five
 *   ignored.
 * - All the coins of a batch are flipped before any is ignored, so the flipper chooses knowing every
 *   result.
 * - A second Thumb replaces each of the first one's coins in turn: four coins per flip, three
 *   ignored.
 */
val KrarksThumb = card("Krark's Thumb") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact"
    oracleText = "If you would flip a coin, instead flip two coins and ignore one."

    staticAbility {
        ability = FlipAdditionalCoins(coinsPerFlip = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Ron Spencer"
        flavorText = "\"I can think of one goblin it ain't so lucky for.\"\n—Slobad, goblin tinkerer"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78a5d49a-747e-4ec8-a20a-ca917c315774.jpg?1783944517"
    }
}
