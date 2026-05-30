package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Exotic Curse
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * Domain — Enchanted creature gets -1/-1 for each basic land type among lands you control.
 */
val ExoticCurse = card("Exotic Curse") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Domain — Enchanted creature gets -1/-1 for each basic land type among lands you control."

    auraTarget = Targets.Creature

    staticAbility {
        val negDomain = DynamicAmount.Multiply(DynamicAmounts.domain(), -1)
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = negDomain,
            toughnessBonus = negDomain
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ee35d99-9a8a-421b-bf43-74446909d87d.jpg?1562923839"
    }
}
