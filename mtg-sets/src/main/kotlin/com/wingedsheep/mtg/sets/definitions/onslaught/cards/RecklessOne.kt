package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

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
    oracleText = "Haste\nReckless One's power and toughness are each equal to the number of Goblins on the battlefield."

    dynamicStats(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Goblin")))

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Ron Spencer"
        flavorText = "It counts only two things: the number of goblins on the battlefield and the number of goblins it takes to burn down a forest."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37775f40-10de-4f5d-abb2-c49e682039de.jpg?1585512782"
    }
}
