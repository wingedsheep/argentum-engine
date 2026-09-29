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
 */
val TranscendentMessage = card("Transcendent Message") {
    manaCost = "{X}{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\nDraw X cards."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Liiga Smilshkalne"
        flavorText = "Wrenn reached out to Teferi through the Blind Eternities, and an impossible connection flared across the vast emptiness."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3261c7e6-45be-44d1-8855-026359ba0ae7.jpg?1783917021"

        ruling("2024-01-12", "When calculating a spell's total cost, include any alternative costs, additional costs, or anything else that increases or reduces the cost to cast the spell. Convoke applies after the total cost is calculated. Convoke doesn't change a spell's mana cost or mana value.")
        ruling("2024-01-12", "You can tap any untapped creature you control to convoke a spell, even one you haven't controlled continuously since the beginning of your most recent turn.")
        ruling("2024-01-12", "Tapping an untapped creature that's attacking or blocking to convoke a spell won't cause that creature to stop attacking or blocking.")
        ruling("2024-01-12", "Tapping a multicolored creature using convoke will pay for {1} or one mana of your choice of any of that creature's colors.")
    }
}
