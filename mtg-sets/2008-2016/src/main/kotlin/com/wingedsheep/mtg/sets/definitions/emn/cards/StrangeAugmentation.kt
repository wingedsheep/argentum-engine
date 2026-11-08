package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Strange Augmentation (Eldritch Moon #105)
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * Delirium — Enchanted creature gets an additional +2/+2 as long as there are four or more card
 * types among cards in your graveyard.
 *
 * "Your graveyard" is the graveyard of the Aura's controller, not the enchanted creature's.
 */
val StrangeAugmentation = card("Strange Augmentation") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Delirium — Enchanted creature gets an additional +2/+2 as long as there are four or more " +
        "card types among cards in your graveyard."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = ModifyStats(2, 2)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d9b9847-7ef7-4fcc-ba16-8f2859d53fe6.jpg?1783937473"
    }
}
