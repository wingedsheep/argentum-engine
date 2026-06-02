package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Eagles of the North
 * {5}{W}
 * Creature — Bird Soldier
 * 3/3
 * Flying
 * When this creature enters, creatures you control get +1/+0 and gain first strike until end of turn.
 * Plainscycling {1}
 */
val EaglesOfTheNorth = card("Eagles of the North") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, creatures you control get +1/+0 and gain first strike until end of turn.\n" +
        "Plainscycling {1} ({1}, Discard this card: Search your library for a Plains card, reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GroupPatterns.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl) then
            GroupPatterns.grantKeywordToAll(Keyword.FIRST_STRIKE, GroupFilter.AllCreaturesYouControl)
    }

    keywordAbility(KeywordAbility.typecycling("Plains", ManaCost.parse("{1}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Axel Sauerwald"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1bd3bc0-77bd-40fe-b4f1-835a04cb6e41.jpg?1687210951"
    }
}
