package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rising Miasma
 * {3}{B}
 * Sorcery
 * All creatures get -2/-2 until end of turn.
 * Awaken 3—{5}{B}{B} (If you cast this spell for {5}{B}{B}, also put three +1/+1 counters on
 * target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The -2/-2 locks in the creatures on the battlefield as it resolves; the awaken ability runs
 * after it, so a land that only becomes a creature through awaken is a 3/3, not a 1/1.
 */
val RisingMiasma = card("Rising Miasma") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn.\n" +
        "Awaken 3—{5}{B}{B} (If you cast this spell for {5}{B}{B}, also put three +1/+1 counters on " +
        "target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{5}{B}{B}"))

    spell {
        effect = Patterns.Group.modifyStatsForAll(-2, -2, GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f9a8e87-3b8b-4dbf-9c1e-0a3290a33a0b.jpg?1783938199"
        ruling(
            "2015-08-25",
            "If you cast Rising Miasma for its awaken cost, creatures will get -2/-2 before the land you targeted is turned into a creature. Unless it was already a land creature before Rising Miasma started resolving, it won't get -2/-2.",
        )
    }
}
