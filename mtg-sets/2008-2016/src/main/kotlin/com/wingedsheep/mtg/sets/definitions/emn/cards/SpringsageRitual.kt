package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Springsage Ritual
 * {3}{G}
 * Instant
 * Destroy target artifact or enchantment. You gain 4 life.
 */
val SpringsageRitual = card("Springsage Ritual") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment. You gain 4 life."

    spell {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(permanent) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Ben Maier"
        flavorText = "\"Every evil destroyed is a strike against the enemy.\"\n" +
            "—Erold, Gatstaf springsage"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e0b23a4-5868-4dd8-ae68-846934e1bd52.jpg"
    }
}
