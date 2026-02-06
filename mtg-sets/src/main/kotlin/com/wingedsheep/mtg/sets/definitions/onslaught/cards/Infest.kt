package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForGroupEffect

/**
 * Infest
 * {1}{B}{B}
 * Sorcery
 * All creatures get -2/-2 until end of turn.
 */
val Infest = card("Infest") {
    manaCost = "{1}{B}{B}"
    typeLine = "Sorcery"

    spell {
        effect = ModifyStatsForGroupEffect(
            powerModifier = -2,
            toughnessModifier = -2,
            filter = GroupFilter.AllCreatures
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Carl Critchlow"
        flavorText = "\"This is the end for you, insect. And for you. And you.\"\n—Braids, dementia summoner"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8604f47a-1048-4b49-b3be-56bef48a5940.jpg?1562922221"
    }
}
