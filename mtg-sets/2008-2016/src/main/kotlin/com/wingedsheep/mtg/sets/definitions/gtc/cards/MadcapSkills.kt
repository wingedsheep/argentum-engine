package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Madcap Skills
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+0 and has menace.
 */
val MadcapSkills = card("Madcap Skills") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+0 and has menace."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Anthony Palumbo"
        flavorText = "The larger the crowd, the harder it is for them to run away."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67ef2667-40f6-4c1c-af81-3e45d2437d5f.jpg"
    }
}
