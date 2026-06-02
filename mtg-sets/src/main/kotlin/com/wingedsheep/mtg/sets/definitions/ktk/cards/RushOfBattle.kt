package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Rush of Battle
 * {3}{W}
 * Sorcery
 * Creatures you control get +2/+1 until end of turn.
 * Warrior creatures you control gain lifelink until end of turn.
 */
val RushOfBattle = card("Rush of Battle") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+1 until end of turn. Warrior creatures you control gain lifelink until end of turn."

    spell {
        effect = GroupPatterns.modifyStatsForAll(
            power = 2,
            toughness = 1,
            filter = GroupFilter.AllCreaturesYouControl
        ).then(
            GroupPatterns.grantKeywordToAll(
                keyword = Keyword.LIFELINK,
                filter = GroupFilter.AllCreaturesYouControl.withSubtype("Warrior")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Dan Murayama Scott"
        flavorText = "The Mardu charge reflects the dragon's speed—and its hunger."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d47d8aa-59c8-4e2c-bb48-328ae924dbb3.jpg?1562782475"
    }
}
