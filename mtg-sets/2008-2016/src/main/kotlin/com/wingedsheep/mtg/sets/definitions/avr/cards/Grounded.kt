package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grounded
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature loses flying.
 */
val Grounded = card("Grounded") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature loses flying."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Greg Staples"
        flavorText = "\"We've tracked Ludevic's monster across four provinces. When it lands, make sure it stays put.\"\n—Alena, trapper of Kessig, to Halana of Ulvenwald"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc4982f0-0ede-4846-82c8-bcf7ad63d099.jpg?1783940666"
    }
}
