package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Hoshi Sato, Exolinguist
 * {4}{U}
 * Legendary Creature — Human Scientist
 * 3/3
 *
 * Federation — When Hoshi Sato enters, look at the top X cards of your library, where X is the
 * number of creature types among non-Borg creatures you control. Put one of them into your hand and
 * the rest on the bottom of your library in a random order.
 *
 * X counts on resolution (`DynamicAmounts.federation()`), Hoshi's own Human and Scientist included.
 */
val HoshiSatoExolinguist = card("Hoshi Sato, Exolinguist") {
    manaCost = "{4}{U}"
    typeLine = "Legendary Creature — Human Scientist"
    colorIdentity = "U"
    oracleText = "Federation — When Hoshi Sato enters, look at the top X cards of your library, where X is " +
        "the number of creature types among non-Borg creatures you control. Put one of them into your hand " +
        "and the rest on the bottom of your library in a random order."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmounts.federation(),
            keepCount = DynamicAmounts.fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Jason Seiler"
        flavorText = "\"Whatever they say, you have to respond in kind. Otherwise they'll take offense.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24589f4d-c530-4f6a-b2d0-450ef70fca71.jpg?1784065562"
    }
}
