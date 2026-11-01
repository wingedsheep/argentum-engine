package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kasmina's Transmutation
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature loses all abilities and has base power and toughness 1/1.
 *
 * Same ability-loss + base-P/T statics as Utter Insignificance, without the flash and exile.
 */
val KasminasTransmutation = card("Kasmina's Transmutation") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature loses all abilities and has base power and toughness 1/1."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Uriah Voth"
        flavorText = "\"Hop away now, little Eternal. Go eat some flies.\"\n—Kasmina"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9771412-695a-4e89-b146-0e3a7336d319.jpg?1783933460"
        ruling("2019-05-03", "Kasmina's Transmutation overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Kasmina's Transmutation becomes attached to a creature will overwrite this effect.")
        ruling("2019-05-03", "Effects that modify a creature's power and/or toughness, such as the effect of Bleeding Edge, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness.")
        ruling("2019-05-03", "If the affected creature gains an ability after Kasmina's Transmutation becomes attached to it, it will keep that ability.")
    }
}
