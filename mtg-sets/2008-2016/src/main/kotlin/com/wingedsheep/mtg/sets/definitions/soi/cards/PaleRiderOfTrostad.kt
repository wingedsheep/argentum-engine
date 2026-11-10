package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pale Rider of Trostad (Shadows over Innistrad #128)
 * {1}{B}
 * Creature — Spirit
 * 3/3
 * Skulk
 * When this creature enters, discard a card.
 */
val PaleRiderOfTrostad = card("Pale Rider of Trostad") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "When this creature enters, discard a card."
    power = 3
    toughness = 3

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Discard(1)
        description = "When this creature enters, discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Seb McKinnon"
        flavorText = "\"What's worse than being alone on the moors at night? Try *not* being alone.\"\n" +
            "—Nadila Maas, moor chaplain"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dad70b8-0dec-4634-a31a-b78438a313a2.jpg?1783937767"
        ruling(
            "2016-04-08",
            "You can cast Pale Rider of Trostad even if you have no other cards in your hand. If you " +
                "have no cards in hand as the last ability resolves, nothing happens."
        )
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
