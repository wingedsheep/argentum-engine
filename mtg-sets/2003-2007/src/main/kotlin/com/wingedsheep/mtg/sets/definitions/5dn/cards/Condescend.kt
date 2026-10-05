package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Condescend
 * {X}{U}
 * Instant
 * Counter target spell unless its controller pays {X}. Scry 2.
 */
val Condescend = card("Condescend") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}. Scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)"

    spell {
        target(TargetFilter.SpellOnStack)
        // You scry 2 even if the spell's controller pays {X}.
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue()) then Patterns.Library.scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8303b80-e29a-46b8-90b0-c0cfe551b435.jpg?1783944406"
        ruling("2017-11-17", "You scry 2 even if the spell's controller pays {X}.")
        ruling("2017-11-17", "You must be able to target another spell to cast Condescend. Condescend can't target itself.")
    }
}
