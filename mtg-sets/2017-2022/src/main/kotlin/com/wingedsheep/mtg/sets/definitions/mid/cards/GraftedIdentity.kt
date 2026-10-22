package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grafted Identity (Innistrad: Midnight Hunt #57)
 * {2}{U}{U} · Enchantment — Aura
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Enchant creature
 * You control enchanted creature.
 * Enchanted creature gets +1/+1.
 */
val GraftedIdentity = card("Grafted Identity") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Enchant creature\n" +
        "You control enchanted creature.\n" +
        "Enchanted creature gets +1/+1."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Manuel Castañón"
        flavorText = "Autonomy is only skin deep."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2559846b-03d6-4a2c-97e1-c71f107e9f24.jpg?1783925642"
    }
}
