package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thundering Ceratok
 * {4}{G}
 * Creature — Rhino
 * 4/5
 * Trample
 * When this creature enters, other creatures you control gain trample until end of turn.
 */
val ThunderingCeratok = card("Thundering Ceratok") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino"
    power = 4
    toughness = 5
    oracleText = "Trample\n" +
        "When this creature enters, other creatures you control gain trample until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Izzy"
        flavorText = "\"I thought this was a civilized plane. How are there so many feral beasts?\"\n—Dovin Baan"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9888d9a4-ad72-43f6-8031-1031877171a9.jpg?1783933404"
    }
}
