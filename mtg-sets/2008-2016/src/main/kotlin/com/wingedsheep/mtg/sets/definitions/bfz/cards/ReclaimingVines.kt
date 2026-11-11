package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reclaiming Vines
 * {2}{G}{G}
 * Sorcery
 * Destroy target artifact, enchantment, or land.
 */
val ReclaimingVines = card("Reclaiming Vines") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, enchantment, or land."

    spell {
        val t = target(TargetFilter.ArtifactEnchantmentOrLand)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Bastien L. Deharme"
        flavorText = "\"This world tires of its long occupation.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18be4c1a-58d7-409e-b7e0-aadb5ccf814d.jpg?1783938185"
    }
}
