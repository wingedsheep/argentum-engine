package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.Effects

/**
 * Gempalm Avenger
 * {5}{W}
 * Creature — Human Soldier
 * 3/5
 * Cycling {2}{W}
 * When you cycle Gempalm Avenger, Soldier creatures get +1/+1 and gain first strike until end of turn.
 */
val GempalmAvenger = card("Gempalm Avenger") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Cycling {2}{W}\nWhen you cycle Gempalm Avenger, Soldier creatures get +1/+1 and gain first strike until end of turn."
    power = 3
    toughness = 5

    keywordAbility(KeywordAbility.cycling("{2}{W}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.Composite(
            listOf(
                GroupPatterns.modifyStatsForAll(1, 1, GroupFilter.allCreaturesWithSubtype("Soldier")),
                GroupPatterns.grantKeywordToAll(Keyword.FIRST_STRIKE, GroupFilter.allCreaturesWithSubtype("Soldier"))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbc66291-fdcc-4106-8875-94d2b0a70deb.jpg?1562939189"
    }
}
