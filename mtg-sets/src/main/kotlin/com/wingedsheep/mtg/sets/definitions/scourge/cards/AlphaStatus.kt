package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStatsPerSharedCreatureType
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Alpha Status
 * {2}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 for each other creature on the battlefield
 * that shares a creature type with it.
 */
val AlphaStatus = card("Alpha Status") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2 for each other creature on the battlefield that shares a creature type with it."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStatsPerSharedCreatureType(2, 2, StaticTarget.AttachedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Darrell Riche"
        flavorText = "\"The best leaders are made by their followers.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd210c45-57f3-4d7d-93ba-81fe4298ade3.jpg?1562537375"

        ruling("2004-10-04", "Alpha Status counts each creature once if that creature shares at least one creature type with the enchanted creature.")
    }
}
