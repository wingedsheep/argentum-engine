package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Reckless One
 * {3}{R}
 * Creature — Goblin Avatar
 * *|*
 * Haste
 * Reckless One's power and toughness are each equal to the number of Goblins on the battlefield.
 */
val RecklessOne = card("Reckless One") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin Avatar"

    dynamicStats(DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Goblin")))

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Mark Romanoski"
        flavorText = "It counts only two things: the number of goblins on the battlefield and the number of goblins it takes to burn down a forest."
        imageUri = "https://cards.scryfall.io/large/front/2/0/2062c279-2e06-4c3c-98e8-775b1fe8d4fb.jpg?1562901246"
    }
}
