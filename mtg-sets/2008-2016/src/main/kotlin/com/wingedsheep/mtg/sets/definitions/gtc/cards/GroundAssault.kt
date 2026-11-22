package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ground Assault
 * {R}{G}
 * Sorcery
 * Ground Assault deals damage to target creature equal to the number of lands you control.
 */
val GroundAssault = card("Ground Assault") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Ground Assault deals damage to target creature equal to the number of lands you control."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.landsYouControl(), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Karl Kopinski"
        flavorText = "\"Taking down a dragon is easy. All you need is enough space to get in a good swing.\"\n—Narbulg Nine Fingers"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4220348-f030-4639-b1a9-6f61ac6bb6a8.jpg"
    }
}
