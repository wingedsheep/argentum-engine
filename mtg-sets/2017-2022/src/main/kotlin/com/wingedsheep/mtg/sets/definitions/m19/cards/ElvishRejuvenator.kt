package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Elvish Rejuvenator
 * {2}{G}
 * Creature — Elf Druid
 * 1/1
 * When this creature enters, look at the top five cards of your library. You may put a land card
 * from among them onto the battlefield tapped. Put the rest on the bottom of your library in a
 * random order.
 *
 * Kaslem's Stonetree's Gather → Select → Move pipeline over the top five: an optional land pick
 * (`chooseUpToSplit(1)`) to the battlefield tapped, the rest to the bottom in a random order.
 */
val ElvishRejuvenator = card("Elvish Rejuvenator") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, look at the top five cards of your library. You may put " +
        "a land card from among them onto the battlefield tapped. Put the rest on the bottom of your " +
        "library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(5))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card from among them onto the battlefield tapped",
                showAllCards = true
            )
            move(kept, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0299b00-b16c-4e7d-b67a-ec160ea81a54.jpg?1783934536"
    }
}
