package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Shiv's Embrace
 * {2}{R}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and has flying.
 * {R}: Enchanted creature gets +1/+0 until end of turn.
 */
val ShivsEmbrace = card("Shiv's Embrace") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and has flying.\n" +
        "{R}: Enchanted creature gets +1/+0 until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Bob Eggleton"
        flavorText = "\"Wear the foe's form to best it in battle. So sayeth the bey.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c9ba1a3-1de4-4771-b76a-89354bc799d3.jpg?1783946325"
    }
}
