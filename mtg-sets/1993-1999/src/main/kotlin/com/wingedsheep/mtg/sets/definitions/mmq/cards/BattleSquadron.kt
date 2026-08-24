package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Battle Squadron
 * {3}{R}{R}
 * Creature — Goblin
 * * / *
 */
val BattleSquadron = card("Battle Squadron") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Flying\n" +
        "Battle Squadron's power and toughness are each equal to the number of creatures you control."
    power = 0
    toughness = 0
    dynamicStats(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature))

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Mark Tedin"
        flavorText = "The goblins made an unruly pile with military precision."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37d55504-ee04-4a5a-a952-9ec5dc2db413.jpg"
    }
}
