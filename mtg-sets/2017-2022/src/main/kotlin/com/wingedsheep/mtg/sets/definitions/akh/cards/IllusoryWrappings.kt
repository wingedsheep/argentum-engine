package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Illusory Wrappings
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has base power and toughness 0/2.
 *
 * Same base-P/T static as Kasmina's Transmutation, without the ability loss.
 */
val IllusoryWrappings = card("Illusory Wrappings") {
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
        collectorNumber = "58"
        artist = "Deruchenko Alexander"
        flavorText = "\"No amount of strength can help you overcome my trial. You must be ready to forgo " +
            "your body and favor your mind.\"\n—Kefnet, god of knowledge"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a67aefde-2c91-4974-b89d-84333a2874eb.jpg?1783936521"
        ruling("2017-04-18", "Illusory Wrappings overwrites all previous effects that set the creature's base power and toughness to specific values, including those that define a * such as that of Enigma Drake. Any power- or toughness-setting effects that start to apply to a creature after Illusory Wrappings becomes attached to it will overwrite this effect.")
        ruling("2017-04-18", "Any power- or toughness-modifying effects (those that give +N/+N, for example) and counters will apply to the creature's new base power and toughness, even if they started to apply before Illusory Wrappings became attached.")
    }
}
