package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RevealUntilNonlandModifyStatsEffect

/**
 * Goblin Machinist
 * {4}{R}
 * Creature — Goblin
 * 0/5
 * {2}{R}: Reveal cards from the top of your library until you reveal a nonland card.
 * Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value.
 * Put the revealed cards on the bottom of your library in any order.
 */
val GoblinMachinist = card("Goblin Machinist") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Goblin"
    power = 0
    toughness = 5

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = RevealUntilNonlandModifyStatsEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Doug Chaffee"
        imageUri = "https://cards.scryfall.io/large/front/5/8/5874e312-1010-43f2-b330-82bc9fcc9f53.jpg?1562915797"
    }
}
