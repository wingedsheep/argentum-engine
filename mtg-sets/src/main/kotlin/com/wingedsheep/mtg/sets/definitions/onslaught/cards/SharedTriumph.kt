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
        imageUri = "https://cards.scryfall.io/normal/front/0/4/043a4031-0854-41c5-a558-adfa7921e54d.jpg?1562895765"
    }
}
