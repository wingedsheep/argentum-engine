package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Reduce in Stature
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has base power and toughness 0/2.
 *
 * Same shape as Illusory Wrappings: a base-P/T-setting static in layer 7b, so +N/+N effects,
 * counters and switches (7c/7d) still apply on top of it.
 */
val ReduceInStature = card("Reduce in Stature") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has base power and toughness 0/2."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = SetBasePowerToughnessStatic(0, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Dan Murayama Scott"
        flavorText = "A dragon learns humility only in the moments before its death."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bad1d13-c4f6-4440-9988-e1db40039f02.jpg?1783938606"
        ruling("2015-02-25", "Reduce in Stature overrides all previous effects that set the creature's power or toughness to specific values. However, effects that set its power or toughness to specific values that start to apply after Reduce in Stature becomes attached to the creature will override this effect.")
        ruling("2015-02-25", "Effects that modify the power or toughness of the creature, such as the effects of Giant Growth or Honor of the Pure, will apply to it no matter when they started to take effect. The same is true for counters that change the creature's power or toughness (such as +1/+1 counters) and effects that switch its power and toughness.")
    }
}
