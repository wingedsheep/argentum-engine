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

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 5)
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Greg Staples"
        flavorText = "\"In the pits, you learn to fight with whatever you can find.\"\n—Kamahl, pit fighter"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02c1b04f-5150-4816-bf7f-77eee0035596.jpg?1562895486"
    }
}
