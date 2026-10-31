package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Loxodon Sergeant
 * {3}{W}
 * Creature — Elephant Soldier
 * 3/3
 * Vigilance
 * When this creature enters, other creatures you control gain vigilance until end of turn.
 */
val LoxodonSergeant = card("Loxodon Sergeant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Soldier"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, other creatures you control gain vigilance until end of turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.grantKeywordToAll(Keyword.VIGILANCE, GroupFilter.OtherCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Jesper Ejsing"
        flavorText = "His voice is both war horn and rallying cry, a trumpeting call that stirs even the faintest heart."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c57f873b-5fb2-4638-b66c-f6d1aec156ba.jpg?1783933480"
    }
}
