package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Zealot's Conviction — Phyrexia: All Will Be One #39
 * {W}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * Corrupted — As long as an opponent has three or more poison counters, enchanted creature gets an
 * additional +1/+0 and has first strike.
 *
 * The corrupted half is two statics gated on [Conditions.Corrupted] (layer 7c P/T and layer 6
 * first strike are separate modifications), the same two-block shape as Knife.
 */
val ZealotsConviction = card("Zealot's Conviction") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Corrupted — As long as an opponent has three or more poison counters, enchanted creature gets an additional +1/+0 and has first strike."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        condition = Conditions.Corrupted
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        condition = Conditions.Corrupted
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a419ba1-7bd3-48e3-8f88-d9f833b25d6d.jpg?1783918071"
    }
}
