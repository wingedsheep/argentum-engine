package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spell Shrivel
 * {2}{U}
 * Instant
 * Devoid (This card has no color.)
 * Counter target spell unless its controller pays {4}. If that spell is countered this way, exile it
 * instead of putting it into its owner's graveyard.
 */
val SpellShrivel = card("Spell Shrivel") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Counter target spell unless its controller pays {4}. If that spell is countered this way, exile it " +
        "instead of putting it into its owner's graveyard."

    keywords(Keyword.DEVOID)

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.fixed(4), exileOnCounter = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efa110cb-f091-48f0-bc62-80f5f18568e8.jpg?1783938211"
    }
}
