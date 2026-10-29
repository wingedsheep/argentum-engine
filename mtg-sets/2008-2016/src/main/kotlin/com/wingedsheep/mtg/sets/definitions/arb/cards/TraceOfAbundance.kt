package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Trace of Abundance
 * {R/W}{G}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has shroud.
 * Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any
 * color.
 *
 * Diplomatic Immunity's [GrantKeyword] (default filter = the enchanted permanent) for the shroud,
 * plus Fertile Ground's [AdditionalManaOnTap] with `anyColor = true` for the mana.
 */
val TraceOfAbundance = card("Trace of Abundance") {
    manaCost = "{R/W}{G}"
    colorIdentity = "RGW"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has shroud. (It can't be the target of spells or abilities.)\n" +
        "Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantKeyword(Keyword.SHROUD)
    }

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(1), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6151001a-cf4b-4f95-a986-7181313e2224.jpg?1783942409"
    }
}
