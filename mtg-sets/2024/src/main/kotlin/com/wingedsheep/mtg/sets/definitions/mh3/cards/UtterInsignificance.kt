package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Utter Insignificance
 * {1}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature loses all abilities and has base power and toughness 1/1.
 * {2}{C}: Exile enchanted creature.
 *
 * Stasis Field's ability-loss + base-P/T statics plus Cooped Up's exile activation; the {C}
 * symbol demands colorless mana specifically.
 */
val UtterInsignificance = card("Utter Insignificance") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature loses all abilities and has base power and toughness 1/1.\n" +
        "{2}{C}: Exile enchanted creature."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{C}")
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "{2}{C}: Exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Lius Lasahido"
        flavorText = "\"I used to think my life was meaningless. Now I know for certain.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3050ac06-4c16-4155-a97f-f6bc92709ee4.jpg?1783911286"
        ruling("2024-06-07", "If the enchanted creature gains an ability after Utter Insignificance becomes attached to it, it will keep that ability.")
        ruling("2024-06-07", "Utter Insignificance overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply afterward will overwrite this effect.")
        ruling("2024-06-07", "Effects that modify the creature's power and/or toughness, such as the effect of Wing It, will apply to the creature no matter when they started to take effect. The same is true for counters that change its power and/or toughness and effects that switch its power and toughness.")
    }
}
