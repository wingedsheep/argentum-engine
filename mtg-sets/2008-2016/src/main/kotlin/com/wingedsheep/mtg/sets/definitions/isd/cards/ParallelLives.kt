package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation

/**
 * Parallel Lives
 * {3}{G}
 * Enchantment
 * If an effect would create one or more tokens under your control, it creates twice that many of
 * those tokens instead.
 *
 * Doubling Season's token clause: [MultiplyTokenCreation] with its default
 * "under your control" pattern. Multiple copies stack multiplicatively (each is its own
 * replacement effect).
 */
val ParallelLives = card("Parallel Lives") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText =
        "If an effect would create one or more tokens under your control, it creates twice that many of those tokens instead."

    replacementEffect(MultiplyTokenCreation())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Steve Prescott"
        flavorText = "\"There will come a time when the only prey left will be each other.\"\n—Ulrich of Krallenhorde Pack"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01033dae-fec1-41f2-b7f2-cc6a43331790.jpg?1783940912"
        ruling("2023-09-01", "If you control two Parallel Lives, then the number of tokens created is four times the original number. If you control three, then the number of tokens created is eight times the original number, and so on.")
        ruling("2023-09-01", "Everything that is specified by the effect creating the original token or tokens will also be true about the additional token or tokens created by Parallel Lives's replacement effect. For example, if an effect tells you to create a token \"tapped and attacking,\" the additional tokens will also be tapped and attacking.")
    }
}
