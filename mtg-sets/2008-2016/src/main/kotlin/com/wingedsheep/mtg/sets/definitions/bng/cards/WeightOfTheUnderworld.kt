package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Weight of the Underworld
 * {3}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -3/-2.
 */
val WeightOfTheUnderworld = card("Weight of the Underworld") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -3/-2."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(powerBonus = -3, toughnessBonus = -2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Wesley Burt"
        flavorText = "Proud Alkmenos, who would not bow to Erebos in death, is now bowed by his own hubris for all eternity."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a99bcecd-0b5a-4806-824b-4885fcad1449.jpg?1783939550"
    }
}
