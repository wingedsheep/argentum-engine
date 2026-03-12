package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Benalish Marshal
 * {W}{W}{W}
 * Creature — Human Knight
 * 3/3
 * Other creatures you control get +1/+1.
 */
val BenalishMarshal = card("Benalish Marshal") {
    manaCost = "{W}{W}{W}"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "Other creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Mark Zug"
        flavorText = "\"Some aspire to climb the mountain of Honor. The Benalish are born upon its peak, and from there ascend among the stars.\" —History of Benalia"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dd1a7fc-5dbd-4ed2-9b02-9fd5c55bb629.jpg?1562737424"
    }
}
