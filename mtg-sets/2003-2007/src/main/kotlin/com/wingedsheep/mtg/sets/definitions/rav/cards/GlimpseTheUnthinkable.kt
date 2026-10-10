package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glimpse the Unthinkable
 * {U}{B}
 * Sorcery
 * Target player mills ten cards.
 *
 * [Patterns.Library.mill] publishes the whole recipe — a `GatherCards(TopOfLibrary, isMill = true)`
 * and one `MoveCollection` to the graveyard. The `isMill` flag is load-bearing: CR 701.17 applies
 * "mill that many plus N instead" at the count site, so a same-shaped plain move is a different
 * thing.
 */
val GlimpseTheUnthinkable = card("Glimpse the Unthinkable") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player mills ten cards."

    spell {
        val p = target(Targets.Player)
        effect = Patterns.Library.mill(10, p)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Brandon Kitkouski"
        flavorText = "\"I am confident that if anyone actually penetrates our facades, even the most perceptive would still be fundamentally unprepared for the truth of House Dimir.\"\n—Szadek"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48058253-54b8-403b-8d95-94d8da986e69.jpg"
    }
}
