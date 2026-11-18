package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nyx Infusion — Journey into Nyx #77 (canonical printing)
 * {2}{B} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 as long as it's an enchantment. Otherwise, it gets -2/-2.
 *
 * Two mutually exclusive conditional statics, each gated on the enchanted creature's *projected*
 * card types (`EntityMatches` over the attached permanent), so the buff flips the moment the
 * creature gains or loses the enchantment type.
 */
val NyxInfusion = card("Nyx Infusion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 as long as it's an enchantment. Otherwise, it gets -2/-2."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Enchantment)
    }

    staticAbility {
        ability = ModifyStats(-2, -2, Filters.EnchantedCreature)
        condition = Conditions.Not(Conditions.EnchantedPermanentMatches(GameObjectFilter.Enchantment))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Jason A. Engle"
        flavorText = "A balm or a poison depending on how it is administered, and to whom."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1910d072-1660-4918-a338-d9f87926541d.jpg?1783939434"
    }
}
