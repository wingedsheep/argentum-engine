package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Stasis Field
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has base power and toughness 0/2, has defender, and loses all other abilities.
 */
val StasisField = card("Stasis Field") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has base power and toughness 0/2, has defender, and loses all other abilities."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Jinho Bae"
        flavorText = "Old glitches in mage-ring function became critical tools in Vryn's defense."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38211d78-ebac-4150-ac11-7613a0e9e1bc.jpg?1783917023"
    }
}
