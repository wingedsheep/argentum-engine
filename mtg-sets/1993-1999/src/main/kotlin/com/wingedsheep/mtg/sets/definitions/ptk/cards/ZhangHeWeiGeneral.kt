package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Zhang He, Wei General
 * {3}{B}{B}
 * Legendary Creature — Human Soldier
 * 4/2
 */
val ZhangHeWeiGeneral = card("Zhang He, Wei General") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 2
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "Whenever Zhang He attacks, each other creature you control gets +1/+0 until end of turn."

    keywords(Keyword.HORSEMANSHIP)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Jack Wei"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/736e4bf1-fee9-47cc-9bbc-093f21c77297.jpg?1783946110"
    }
}
