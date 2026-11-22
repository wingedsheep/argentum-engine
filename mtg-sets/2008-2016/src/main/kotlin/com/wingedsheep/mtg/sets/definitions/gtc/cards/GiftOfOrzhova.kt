package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gift of Orzhova
 * {1}{W/B}{W/B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has flying and lifelink.
 */
val GiftOfOrzhova = card("Gift of Orzhova") {
    manaCost = "{1}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has flying and lifelink."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Johannes Voss"
        flavorText = "\"Remember by whose gift you ascend.\"\n—Milana, Orzhov prelate"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d759ef8-cb04-4769-9944-85793af3f6e8.jpg"
    }
}
