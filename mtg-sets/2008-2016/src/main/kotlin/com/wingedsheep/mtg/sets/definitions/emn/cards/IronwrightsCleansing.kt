package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ironwright's Cleansing (Eldritch Moon #32)
 * {2}{W}
 * Sorcery
 *
 * Exile target artifact or enchantment.
 */
val IronwrightsCleansing = card("Ironwright's Cleansing") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target artifact or enchantment."

    spell {
        val target = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Exile(target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "John Stanko"
        flavorText = "\"Impurities must be pounded out.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/916d8aa5-158f-46f4-babe-981a391fd88b.jpg"
    }
}
