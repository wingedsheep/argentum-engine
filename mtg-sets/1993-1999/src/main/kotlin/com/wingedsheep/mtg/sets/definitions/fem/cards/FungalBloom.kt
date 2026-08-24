package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Fungal Bloom
 * {G}{G}
 * Enchantment
 * {G}{G}: Put a spore counter on target Fungus.
 *
 * "Target Fungus" is any permanent with the subtype, not just a creature — the Oracle text says
 * Fungus, and a kindred permanent would qualify.
 */
val FungalBloom = card("Fungal Bloom") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{G}{G}: Put a spore counter on target Fungus."

    activatedAbility {
        cost = Costs.Mana("{G}{G}")
        val t = target(
            "target Fungus",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.FUNGUS)))
        )
        effect = Effects.AddCounters(Counters.SPORE, 1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Daniel Gelon"
        flavorText = "\"Thallids could absorb energy from the forest itself. Even Elves were at a disadvantage in fighting them.\"\n—*Sarpadian Empires, vol. III*"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf1a2cb2-9a6b-41f7-96f7-ec457c69c16c.jpg?1783947887"
    }
}
