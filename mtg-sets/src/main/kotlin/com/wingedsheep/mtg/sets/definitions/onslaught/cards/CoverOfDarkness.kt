package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.GrantKeywordForChosenCreatureType

/**
 * Cover of Darkness
 * {1}{B}
 * Enchantment
 * As Cover of Darkness enters the battlefield, choose a creature type.
 * Creatures of the chosen type have fear.
 */
val CoverOfDarkness = card("Cover of Darkness") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment"
    oracleText = "As Cover of Darkness enters the battlefield, choose a creature type.\nCreatures of the chosen type have fear."

    replacementEffect(EntersWithCreatureTypeChoice())

    staticAbility {
        ability = GrantKeywordForChosenCreatureType(Keyword.FEAR)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/large/front/0/d/0d6d7d88-d82b-40f4-bf57-ec5d7c480689.jpg?1562898088"
    }
}
