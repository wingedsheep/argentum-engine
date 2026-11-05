package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Censor
 * {1}{U}
 * Instant
 * Counter target spell unless its controller pays {1}.
 * Cycling {U} ({U}, Discard this card: Draw a card.)
 */
val Censor = card("Censor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}.\n" +
        "Cycling {U} ({U}, Discard this card: Draw a card.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "John Stanko"
        flavorText = "\"Why so many questions?\"\n—Temmet, vizier of Naktamun"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cb4e315-1a77-479a-9f15-fb23575de805.jpg"
    }
}
