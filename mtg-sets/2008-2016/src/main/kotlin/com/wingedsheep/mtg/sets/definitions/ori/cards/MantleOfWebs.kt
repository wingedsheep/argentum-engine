package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Mantle of Webs
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+3 and has reach.
 */
val MantleOfWebs = card("Mantle of Webs") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+3 and has reach. (It can block creatures with flying.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 3)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Mathias Kollros"
        flavorText = "\"Why does everything the Golgari touch end up sticky?\"\n—Arrester Lavinia, Tenth Precinct"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b09e36a-ad53-44ae-8586-2b658e3c533c.jpg?1783938320"
    }
}
