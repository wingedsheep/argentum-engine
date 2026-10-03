package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Harbinger of the Seas
 * {1}{U}{U}
 * Creature — Merfolk Wizard
 * 2/2
 *
 * Nonbasic lands are Islands.
 *
 * Canonical printing: Modern Horizons 3, the card's earliest real printing.
 *
 * Blood Moon's land-type overwrite pointed at Island: a [SetLandTypesForGroup] over nonbasic lands.
 */
val HarbingerOfTheSeas = card("Harbinger of the Seas") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Nonbasic lands are Islands."
    power = 2
    toughness = 2

    staticAbility {
        ability = SetLandTypesForGroup(
            filter = GroupFilter(GameObjectFilter.NonbasicLand),
            landTypes = setOf("Island"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Winona Nelson"
        flavorText = "The rising moon calls mercilessly to the waves, commanding the sea to rise high enough to devour mountains."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00212714-a410-4cbc-bf1c-f90d7d77378c.jpg?1783911289"
        ruling("2024-06-07", "Nonbasic lands lose any other land types and abilities they had. They gain the land type Island and gain the ability \"{T}: Add {U}.\"")
        ruling("2024-06-07", "Harbinger of the Seas doesn't affect names or supertypes. It won't turn any land into a basic land or remove the legendary supertype from a legendary land, and the lands won't be named \"Island.\"")
        ruling("2024-06-07", "If a nonbasic land has an ability that causes it to enter the battlefield tapped, it will lose that ability before it can apply.")
    }
}
