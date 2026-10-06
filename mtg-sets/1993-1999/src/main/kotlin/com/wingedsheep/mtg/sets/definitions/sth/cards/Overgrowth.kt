package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Overgrowth
 * {2}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional {G}{G}.
 */
val Overgrowth = card("Overgrowth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land is tapped for mana, its controller adds an additional {G}{G}."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = AdditionalManaOnTap(color = Color.GREEN, amount = DynamicAmounts.fixed(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Rob Alexander"
        flavorText = "Life needs no encouragement."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb9179f5-c3e0-4499-9cfb-6cb7e8329a59.jpg?1783946550"
        ruling("2004-10-04", "This is a triggered mana ability.")
    }
}
