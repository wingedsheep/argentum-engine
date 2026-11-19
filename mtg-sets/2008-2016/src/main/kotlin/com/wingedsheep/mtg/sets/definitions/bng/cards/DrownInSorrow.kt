package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Drown in Sorrow
 * {1}{B}{B}
 * Sorcery
 *
 * All creatures get -2/-2 until end of turn. Scry 1.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val DrownInSorrow = card("Drown in Sorrow") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn. Scry 1. (Look at the top card of your " +
        "library. You may put that card on the bottom.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(-2, -2, GroupFilter.AllCreatures) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Noah Bradley"
        flavorText = "\"The sad truth is that the whip of Erebos is long enough to wrap around all our throats.\"\n—Perisophia the philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/287c7570-8080-43dc-a586-963e15566446.jpg?1783939559"
    }
}
