package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Precision Bolt
 * {2}{R}
 * Sorcery
 * Precision Bolt deals 3 damage to any target.
 */
val PrecisionBolt = card("Precision Bolt") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Precision Bolt deals 3 damage to any target."

    spell {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(3, any)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "267"
        inBooster = false
        artist = "Grzegorz Rutkowski"
        flavorText = "Ral had wielded lightning all his life but had never harnessed the power of an entire guild."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a59b4e5b-e9e0-4507-b9e7-8fba7e3a54f9.jpg?1783934094"
    }
}
