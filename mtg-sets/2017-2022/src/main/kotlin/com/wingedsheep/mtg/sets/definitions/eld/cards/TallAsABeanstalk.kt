package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tall as a Beanstalk
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3, has reach, and is a Giant in addition to its other types.
 */
val TallAsABeanstalk = card("Tall as a Beanstalk") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +3/+3, has reach, and is a Giant in addition to its other types."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    staticAbility {
        // GrantSubtype defaults its filter to the source — an Aura names the enchanted creature.
        ability = GrantSubtype("Giant", Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Emrah Elmasli"
        flavorText = "He realized a bit late that he was supposed to *plant* the magic beans."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/098bfd71-a47f-4dfc-b516-b88a388fb7b5.jpg?1783932603"
    }
}
