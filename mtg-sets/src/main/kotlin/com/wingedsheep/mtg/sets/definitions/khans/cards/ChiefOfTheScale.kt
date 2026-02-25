package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chief of the Scale
 * {W}{B}
 * Creature — Human Warrior
 * 2/3
 * Other Warrior creatures you control get +0/+1.
 */
val ChiefOfTheScale = card("Chief of the Scale") {
    manaCost = "{W}{B}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Warrior"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "David Palumbo"
        flavorText = "\"We are the shield unbroken. If we fall today, we will die well, and our trees will bear our names in honor.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a227b8cc-cbe5-4955-ad1b-a354704a82e8.jpg?1562791315"
    }
}
