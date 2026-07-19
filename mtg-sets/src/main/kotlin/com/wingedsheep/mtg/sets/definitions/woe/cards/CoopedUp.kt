package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cooped Up
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block.
 * {2}{W}: Exile enchanted creature.
 *
 * "Can't attack or block" is the Pacifism idiom — [CantAttack] + [CantBlock] over the attached
 * creature. The activated ability reuses [EffectTarget.EnchantedCreature], same shape as
 * Sigarda's Imprisonment (VOW).
 */
val CoopedUp = card("Cooped Up") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "{2}{W}: Exile enchanted creature."

    auraTarget = Targets.Creature

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "{2}{W}: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Jodie Muir"
        flavorText = "\"Cold iron's all you need to hold a faerie. The charms are mostly " +
            "decorative.\"\n—Eulyn, Edgewall peddler"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8acb8758-09c5-4e19-ada1-904e36ece1fc.jpg?1783915134"
    }
}
