package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Festering Thicket — Edge of Eternities Commander #21 (canonical printing)
 * Land — Swamp Forest
 *
 * ({T}: Add {B} or {G}.)
 * This land enters tapped.
 * Cycling {2} ({2}, Discard this card: Draw a card.)
 */
val FesteringThicket = card("Festering Thicket") {
    colorIdentity = "BG"
    typeLine = "Land — Swamp Forest"
    oracleText = "({T}: Add {B} or {G}.)\nThis land enters tapped.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    replacementEffect(EntersTapped())

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Jakob Eirich"
        flavorText = "Evendo's microbial life swiftly adapted to metabolize the leaking star fuel, " +
            "devouring it like any other nourishment."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d312f4ca-909a-47df-91dc-dfb63838b5f5.jpg?1783906062"

        ruling(
            "2025-07-25",
            "Unlike some other dual lands, Festering Thicket has two basic land types. It's not basic, so " +
                "effects that search for basic lands can't find it, but it does have the appropriate land " +
                "types for effects that care about them."
        )
    }
}
