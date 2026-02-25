package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chief of the Edge
 * {W}{B}
 * Creature — Human Warrior
 * 3/2
 * Other Warrior creatures you control get +1/+0.
 */
val ChiefOfTheEdge = card("Chief of the Edge") {
    manaCost = "{W}{B}"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Warrior"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "David Palumbo"
        flavorText = "\"We are the swift, the strong, the blade's sharp shriek! Fear nothing, and strike!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28822a9a-97fa-4784-ad97-072fcfc7b9ed.jpg?1562784003"
    }
}
