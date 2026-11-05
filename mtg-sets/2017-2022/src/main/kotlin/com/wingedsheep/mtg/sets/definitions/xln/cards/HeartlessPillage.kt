package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Heartless Pillage
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards.
 * Raid — If you attacked this turn, create a Treasure token.
 *
 * Raid is a resolution-time [Conditions.YouAttackedThisTurn] check (same shape as Chart a Course).
 */
val HeartlessPillage = card("Heartless Pillage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards.\n" +
        "Raid — If you attacked this turn, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(2, opponent) then Effects.If(
            condition = Conditions.YouAttackedThisTurn,
            then = Effects.CreateTreasure(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5e791ce-1380-4de0-b314-246ea6dfc3cc.jpg?1783935759"
    }
}
