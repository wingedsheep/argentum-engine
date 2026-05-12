package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Goblinoids
 * {4}{R}
 * Creature — Goblin Berserker Villain
 * 5/4
 * Haste
 * Mayhem — You may cast this spell from your graveyard for {2}{R}
 *   if you discarded it this turn.
 */
val RagingGoblinoids = card("Raging Goblinoids") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker Villain"
    power = 5
    toughness = 4
    oracleText = "Haste\nMayhem — You may cast this spell from your graveyard for {2}{R} if you discarded it this turn."

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
    }
}
