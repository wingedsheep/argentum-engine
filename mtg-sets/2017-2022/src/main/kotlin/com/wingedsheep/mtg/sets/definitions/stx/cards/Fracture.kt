package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fracture — Strixhaven: School of Mages #188 (canonical printing)
 * {W}{B} · Instant
 *
 * Destroy target artifact, enchantment, or planeswalker.
 *
 * A single [Effects.Destroy] over an artifact-or-enchantment-or-planeswalker target.
 */
val Fracture = card("Fracture") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or planeswalker."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment or GameObjectFilter.Planeswalker))
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Miranda Meeks"
        flavorText = "\"Your defiance is amusing, little mage. It's time you learn how fragile you really are.\"\n—Extus Narr"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a11224f8-06ea-4ec3-85e6-d5c8d906840c.jpg?1783927312"
    }
}
