package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.ModifyStatsForChosenCreatureType

/**
 * Shared Triumph
 * {1}{W}
 * Enchantment
 * As Shared Triumph enters the battlefield, choose a creature type.
 * Creatures of the chosen type get +1/+1.
 */
val SharedTriumph = card("Shared Triumph") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "As Shared Triumph enters the battlefield, choose a creature type.\nCreatures of the chosen type get +1/+1."

    replacementEffect(EntersWithCreatureTypeChoice())

    staticAbility {
        ability = ModifyStatsForChosenCreatureType(
            powerBonus = 1,
            toughnessBonus = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Mark Brill"
        flavorText = "\"Win together, die alone.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d07ebe6-76cf-4345-b59b-9954496c44d0.jpg?1562898003"
    }
}
