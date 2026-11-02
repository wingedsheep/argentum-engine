package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dwindle — Core Set 2019 #53
 * {2}{U} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -6/-0.
 * When enchanted creature blocks, destroy it. (The attacking creature remains blocked.)
 */
val Dwindle = card("Dwindle") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -6/-0.\n" +
        "When enchanted creature blocks, destroy it. (The attacking creature remains blocked.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-6, 0, Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.blocks()
        effect = Effects.Destroy(EffectTarget.EnchantedCreature)
        description = "When enchanted creature blocks, destroy it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Ryan Pancoast"
        flavorText = "The collection didn't have room to expand, so the wizard improvised a solution."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb38f190-30f0-49ca-99c3-d3cdf21075e8.jpg?1783934589"
    }
}
