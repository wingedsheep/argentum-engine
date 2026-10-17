package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Brute
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+0.
 * {R}{R}{R}: Regenerate enchanted creature.
 */
val TheBrute = card("The Brute") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+0.\n{R}{R}{R}: Regenerate enchanted creature."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    activatedAbility {
        cost = Costs.Mana("{R}{R}{R}")
        effect = Effects.Regenerate(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Mark Poole"
        flavorText = "\"Union may be strength, but it is mere blind brute strength unless wisely directed.\" —Samuel Butler"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9ffb265-872f-47b3-974c-92bcbebd557e.jpg?1783948052"
    }
}
