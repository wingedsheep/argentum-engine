package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Furtive Homunculus (Shadows over Innistrad #64)
 * {1}{U}
 * Creature — Homunculus
 * 2/1
 * Skulk
 */
val FurtiveHomunculus = card("Furtive Homunculus") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homunculus"
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)"
    power = 2
    toughness = 1

    keywords(Keyword.SKULK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Wayne Reynolds"
        flavorText = "\"In terms of intelligence, homunculi are somewhere between humans and skaabs—clever " +
            "enough to follow more complex orders, but dim enough to not question them.\"\n—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f376ec9b-48ac-4ed7-bfa2-60e9dca32dd7.jpg?1783937798"
        ruling(
            "2016-04-08",
            "Skulk matters only as blockers are chosen. Modifying either creature's power after " +
                "blockers are chosen won't cause the attacking creature to become unblocked."
        )
        ruling(
            "2016-04-08",
            "If you cause a creature to have 0 power or less, use the actual value (which may be " +
                "negative) to determine whether it can block or be blocked. A creature with skulk and 0 " +
                "or less power most likely won't be blocked, but it won't deal combat damage and won't " +
                "trigger any abilities that trigger when combat damage is dealt."
        )
    }
}
