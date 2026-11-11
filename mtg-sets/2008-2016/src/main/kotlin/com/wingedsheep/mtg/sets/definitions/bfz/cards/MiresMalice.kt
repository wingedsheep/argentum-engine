package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Mire's Malice
 * {3}{B}
 * Sorcery
 * Target opponent discards two cards.
 * Awaken 3—{5}{B} (If you cast this spell for {5}{B}, also put three +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the opponent target and runs the awaken ability after the discard.
 */
val MiresMalice = card("Mire's Malice") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards.\n" +
        "Awaken 3—{5}{B} (If you cast this spell for {5}{B}, also put three +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{5}{B}"))

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2d737fb-fd09-4483-8128-8897cbcdc4f5.jpg?1783938200"
    }
}
