package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cabal Ritual
 * {1}{B}
 * Instant
 * Add {B}{B}{B}.
 * Threshold — Add {B}{B}{B}{B}{B} instead if there are seven or more cards in your graveyard.
 *
 * Threshold is checked on resolution; Cabal Ritual itself is still on the stack then, so it
 * doesn't count toward the seven.
 */
val CabalRitual = card("Cabal Ritual") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Add {B}{B}{B}.\n" +
        "Threshold — Add {B}{B}{B}{B}{B} instead if there are seven or more cards in your graveyard."

    spell {
        effect = Effects.If(
            Conditions.CardsInGraveyardAtLeast(7),
            then = Effects.AddMana(Color.BLACK, 5),
            otherwise = Effects.AddMana(Color.BLACK, 3),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Greg Hildebrandt"
        flavorText = "\"Each syllable chills your veins. Each word rattles your mind.\"\n—Cabal Patriarch"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5403b49d-03a7-4cc3-af3c-df098c1c9c2e.jpg?1783945159"
    }
}
