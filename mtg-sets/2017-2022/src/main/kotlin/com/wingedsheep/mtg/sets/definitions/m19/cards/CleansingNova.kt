package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cleansing Nova
 * {3}{W}{W}
 * Sorcery
 * Choose one —
 * • Destroy all creatures.
 * • Destroy all artifacts and enchantments.
 */
val CleansingNova = card("Cleansing Nova") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy all creatures.\n" +
        "• Destroy all artifacts and enchantments."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy all creatures") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature)
            }
            mode("Destroy all artifacts and enchantments") {
                effect = Effects.DestroyAll(GameObjectFilter.ArtifactOrEnchantment)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Noah Bradley"
        flavorText = "In their final moments, the demons gazed upon grace in its purest form."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5be8eed7-c033-42cc-bd21-4512db7af66c.jpg?1783934610"
    }
}
