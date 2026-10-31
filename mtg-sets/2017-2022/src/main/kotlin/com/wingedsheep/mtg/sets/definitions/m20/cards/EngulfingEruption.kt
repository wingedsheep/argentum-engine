package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Engulfing Eruption (M20 #328)
 * {2}{R}{R}  Sorcery
 *
 * Engulfing Eruption deals 5 damage to target creature.
 */
val EngulfingEruption = card("Engulfing Eruption") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Engulfing Eruption deals 5 damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "328"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Compared to the hunger of the fires within the earth, the appetite of a mere wurm is nothing.\"\n" +
            "—Phelgis Ojin, Keral Keep acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f198c7e3-8a85-48f3-a7c2-93320fd4fb81.jpg"
    }
}
