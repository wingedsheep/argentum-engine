package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Market Festival
 * {3}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional two mana in any
 * combination of colors.
 *
 * Fertile Ground's any-color tap bonus with two units, each colored independently.
 */
val MarketFestival = card("Market Festival") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land is tapped for mana, its controller adds an additional two mana in any combination of colors."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(2), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Ryan Barger"
        flavorText = "Commerce is always the basis for peace."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a70329-ecae-4647-b912-75d936a6c8c5.jpg?1783939412"
    }
}
