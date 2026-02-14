package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCreatureTypeChoice
import com.wingedsheep.sdk.scripting.GrantKeywordForChosenCreatureType

/**
 * Steely Resolve
 * {1}{G}
 * Enchantment
 * As Steely Resolve enters the battlefield, choose a creature type.
 * Creatures of the chosen type have shroud.
 */
val SteelyResolve = card("Steely Resolve") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment"
    oracleText = "As Steely Resolve enters the battlefield, choose a creature type.\nCreatures of the chosen type have shroud."

    replacementEffect(EntersWithCreatureTypeChoice())

    staticAbility {
        ability = GrantKeywordForChosenCreatureType(Keyword.SHROUD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "286"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/large/front/0/9/09a431ac-50dc-4789-981a-ecade707a3d4.jpg?1562898120"
    }
}
