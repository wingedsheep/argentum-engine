package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Augur of Bolas
 * {1}{U}
 * Creature — Merfolk Wizard
 * 1/3
 * When this creature enters, look at the top three cards of your library. You may reveal an instant or sorcery card from among them and put it into your hand. Put the rest on the bottom of your library in any order.
 */
val AugurOfBolas = card("Augur of Bolas") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, look at the top three cards of your library. You may reveal an instant or sorcery card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "You may reveal an instant or sorcery card from among them and put it into your hand",
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e6ec8a6-ad88-45c9-ab4b-dd7de2418bb7.jpg?1783940510"
    }
}
