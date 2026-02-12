package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Improvised Armor
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+5.
 * Cycling {3}
 */
val ImprovisedArmor = card("Improvised Armor") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+5.\nCycling {3}"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 5)
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Alan Pollack"
        flavorText = "\"In the pits, you learn to fight with whatever you can find.\"\n—Kamahl, pit fighter"
        imageUri = "https://cards.scryfall.io/large/front/8/d/8d7d5d79-73d8-4f1a-9dda-4de5f41539d9.jpg?1562928336"
    }
}
