package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gridlock
 * {X}{U}
 * Instant
 * Tap X target nonland permanents.
 */
val Gridlock = card("Gridlock") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap X target nonland permanents."

    spell {
        // "Tap X target nonland permanents" — exactly X: the announced X is the number of targets.
        targets(TargetFilter.NonlandPermanent, exactly = DynamicAmounts.xValue())
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Yeong-Hao Han"
        flavorText = "\"One tiny peg pulled from a single wagon wheel, and an entire district grinds to a halt.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f5c126-3df9-4771-9e74-4ca33161ac08.jpg?1783940138"
    }
}
