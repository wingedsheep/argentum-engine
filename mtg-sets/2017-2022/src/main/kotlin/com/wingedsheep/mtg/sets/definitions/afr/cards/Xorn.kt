package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Xorn
 * {2}{R}
 * Creature — Elemental
 * 3/2
 * If you would create one or more Treasure tokens, instead create those tokens plus an
 * additional Treasure token.
 *
 * One extra Treasure per creation event that includes a Treasure, the same "those tokens plus
 * an additional X token" shape as Worldwalker Helm. Each Xorn judges the batch the previous one
 * grew, so multiple Xorns each add one (per the ruling).
 */
val Xorn = card("Xorn") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 2
    oracleText = "If you would create one or more Treasure tokens, instead create those tokens plus an additional Treasure token."

    replacementEffect(
        CreateAdditionalToken(
            additionalTokenType = "Treasure",
            additionalTokenCount = 1,
            inheritTapped = true,
            appliesTo = EventPattern.TokenCreationEvent(
                controller = Player.You,
                tokenFilter = GameObjectFilter.Artifact.withSubtype("Treasure"),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Yigit Koroglu"
        flavorText = "Xorns are native to the Elemental Plane of Earth, where gems and rare stones are plentiful. When summoned elsewhere they can be quite destructive in search of food."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f73fdf76-e3a6-49d0-bfb0-4b7cdbd4271e.jpg?1783926470"
        ruling("2021-07-23", "If you control multiple copies of Xorn, you'll create that many additional Treasure tokens.")
    }
}
