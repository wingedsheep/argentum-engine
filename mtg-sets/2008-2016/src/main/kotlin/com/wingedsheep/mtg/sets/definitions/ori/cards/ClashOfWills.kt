package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clash of Wills
 * {X}{U}
 * Instant
 * Counter target spell unless its controller pays {X}.
 */
val ClashOfWills = card("Clash of Wills") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Yan Li"
        flavorText = "Alhammarret rose to his feet, and the full force of his mind hit Jace like a storm front."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/665ee42f-8d76-4f8b-9dd3-7455a90f0da7.jpg?1783938353"
    }
}
