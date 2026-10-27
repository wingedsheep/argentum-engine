package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vernal Fen
 *
 * Land — Swamp Forest
 * ({T}: Add {B} or {G}.)
 * This land enters tapped unless you control two or more basic lands.
 */
val VernalFen = card("Vernal Fen") {
    colorIdentity = "BG"
    typeLine = "Land — Swamp Forest"
    oracleText = "({T}: Add {B} or {G}.)\nThis land enters tapped unless you control two or more basic lands."

    // Mana abilities are intrinsic from the basic land types in the type line.

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.BasicLand).count(),
            ComparisonOperator.GTE,
            2
        )
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Titus Lunter"
        flavorText = "\"New things need room to grow. We prune time so the future can flourish.\"\n—Antioxa, Drix elder"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bac98f27-ef3e-45e0-87cb-0fb4325ae734.jpg?1783906060"

        ruling(
            "2025-07-25",
            "Unlike some other dual lands, Vernal Fen has two basic land types. It's not basic, so effects " +
                "that search for basic lands can't find it, but it does have the appropriate land types for " +
                "effects that care about them."
        )
    }
}
