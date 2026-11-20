package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stymied Hopes
 * {1}{U}
 * Instant
 * Counter target spell unless its controller pays {1}. Scry 1.
 */
val StymiedHopes = card("Stymied Hopes") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}. Scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}") then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Peter Mohrbacher"
        flavorText = "When a god's will is against you, the day never ends well."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5702b757-5be5-4a48-bc73-a87ec4f3193b.jpg?1783939790"
    }
}
