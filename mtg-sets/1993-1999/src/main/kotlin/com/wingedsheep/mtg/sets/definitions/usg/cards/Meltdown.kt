package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Meltdown
 * {X}{R}
 * Sorcery
 * Destroy each artifact with mana value X or less.
 *
 * The mana-value cap is read on resolution from the spell's X.
 */
val Meltdown = card("Meltdown") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy each artifact with mana value X or less."

    spell {
        effect = Effects.DestroyAll(
            GameObjectFilter.Artifact.manaValueAtMostDynamic(DynamicAmounts.xValue())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Donato Giancola"
        flavorText = "Catastrophes happened so often at the mana rig that the viashino language had a special word to describe them."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e7a967a-35a0-4e5c-a32b-123a9cfdb79e.jpg?1783946327"
    }
}
