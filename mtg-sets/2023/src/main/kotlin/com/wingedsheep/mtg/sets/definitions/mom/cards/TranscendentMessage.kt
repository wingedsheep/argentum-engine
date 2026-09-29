package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Transcendent Message
 * {X}{U}{U}{U}{U}
 * Instant
 * Convoke
 * Draw X cards.
 *
 * The announced X is generic mana in the total cost (CR 601.2f), so a creature convoked for {1}
 * pays toward X as well as any printed generic — the engine credits the taps left over after the
 * printed generic against the X mana (`CastCostTotaller.paymentXValue`). The X the spell draws is
 * the announced value; convoke pays for it but never changes it.
 */
val TranscendentMessage = card("Transcendent Message") {
    manaCost = "{X}{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Draw X cards."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Liiga Smilshkalne"
        flavorText = "Wrenn reached out to Teferi through the Blind Eternities, and an impossible " +
            "connection flared across the vast emptiness."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3261c7e6-45be-44d1-8855-026359ba0ae7.jpg?1783917021"
    }
}
