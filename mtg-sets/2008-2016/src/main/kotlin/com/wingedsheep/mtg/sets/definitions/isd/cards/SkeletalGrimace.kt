package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Skeletal Grimace
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has "{B}: Regenerate this creature."
 */
val SkeletalGrimace = card("Skeletal Grimace") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1 and has \"{B}: Regenerate this creature.\""
    auraTarget = TargetObject(filter = TargetFilter.Creature)
    staticAbility {
        ability = ModifyStats(1, 1)
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{B}")
                effect = Effects.Regenerate(EffectTarget.Self)
                description = "{B}: Regenerate this creature."
            },
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Eric Deschamps"
        flavorText = "\"And then I realized we all have skeletons on the inside. Why wait until the bodies are dead to control them?\"\n—Jadar, ghoulcaller of Nephalia"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9b28f37-d6b8-4d35-95e9-9533aea0a071.jpg"
    }
}
