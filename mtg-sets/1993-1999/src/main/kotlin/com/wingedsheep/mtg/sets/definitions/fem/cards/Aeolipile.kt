package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Aeolipile
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: It deals 2 damage to any target.
 */
val Aeolipile = card("Aeolipile") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: It deals 2 damage to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Heather Hudson"
        flavorText = "\"Although fragile, the Aeolipile could be quite destructive.\"\n—*Sarpadian Empires, vol. I*"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a09030ee-415c-45af-bf08-7623197a314f.jpg?1783947882"
    }
}
